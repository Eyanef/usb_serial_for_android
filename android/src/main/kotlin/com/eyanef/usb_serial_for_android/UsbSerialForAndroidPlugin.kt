package com.eyanef.usb_serial_for_android

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import androidx.annotation.NonNull
import com.hoho.android.usbserial.driver.*
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

/** UsbSerialForAndroidPlugin */
class UsbSerialForAndroidPlugin : FlutterPlugin, MethodCallHandler, EventChannel.StreamHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var _methodChannel: MethodChannel
    private var _context: Context? = null
    private var _usbManager: UsbManager? = null
    private var _binaryMessenger: BinaryMessenger? = null
    private var _eventSink: EventChannel.EventSink? = null
    private var _eventChannel: EventChannel? = null

    private val _usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent!!.action.equals(ACTION_USB_ATTACHED)) {
                Log.d(TAG, "ACTION_USB_ATTACHED")
                if (_eventSink != null) {
                    val device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) as UsbDevice?
                    val msg: HashMap<String, Any?> = _serializeDevice(device!!)
                    msg["event"] = ACTION_USB_ATTACHED
                    _eventSink!!.success(msg)
                }
            } else if (intent.action.equals(ACTION_USB_DETACHED)) {
                Log.d(TAG, "ACTION_USB_DETACHED")
                if (_eventSink != null) {
                    val device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) as UsbDevice?
                    val msg: HashMap<String, Any?> = _serializeDevice(device!!)
                    msg["event"] = ACTION_USB_DETACHED
                    _eventSink!!.success(msg)
                }
            }
        }

    }


    private interface AcquirePermissionCallback {
        fun onSuccess(device: UsbDevice)
        fun onFailed(device: UsbDevice)
    }


    private fun acquirePermission(device: UsbDevice, cb: AcquirePermissionCallback) {

        class BCR2(
            private val _device: UsbDevice,
            private val _cb: AcquirePermissionCallback
        ) : BroadcastReceiver() {

            override fun onReceive(context: Context?, intent: Intent?) {
                val action: String? = intent?.action
                if (ACTION_USB_PERMISSION == action) {
                    Log.i(TAG, "BCR2 intent arrived, entering sync...")
                    _context?.unregisterReceiver(this)
                    synchronized(this) {
                        Log.i(TAG, "BCR2 in sync")
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            _cb.onSuccess(_device)
                        } else {
                            Log.e(TAG, "Permission denied for device")
                            _cb.onFailed(_device)
                        }
                    }

                }
            }

        }

        val cw = _context
        val usbReceiver = BCR2(device, cb)
        val permissionIntent = PendingIntent.getBroadcast(cw, 0, Intent(ACTION_USB_PERMISSION), 0)
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        cw?.registerReceiver(usbReceiver, filter)
        _usbManager?.requestPermission(device, permissionIntent)
    }

    private fun openDevice(
        device: UsbDevice,
        result: Result,
        allowAcquirePermission: Boolean,
        portNum: Int,
        type: String? = null
    ) {
        val cb: AcquirePermissionCallback = object : AcquirePermissionCallback {
            override fun onSuccess(device: UsbDevice) {
                openDevice(device, result, false, portNum)
            }

            override fun onFailed(device: UsbDevice) {
                result.error(TAG, "Failed to acquire permission", null)
            }
        }

        try {
            val connection: UsbDeviceConnection? = _usbManager?.openDevice(device)
            if (connection == null && allowAcquirePermission) {
                acquirePermission(device, cb)
                return
            }

            var usbSerialDriver: UsbSerialDriver? = null

            Log.i(TAG, "type =>  $type")
            usbSerialDriver = when (type) {
                null -> UsbSerialProber.getDefaultProber().probeDevice(device)
                "ch34x" -> Ch34xSerialDriver(device)
                "cp21xx" -> Cp21xxSerialDriver(device)
                "ftdi" -> FtdiSerialDriver(device)
                "cdc" -> CdcAcmSerialDriver(device)

                else -> {
                    result.error(TAG, "unknown type", null)
                    null
                }
            }


            if (usbSerialDriver == null) {
                result.error(TAG, "connection failed, no driver for device", null)
            }

            val usbSerialPort: UsbSerialPort? = usbSerialDriver?.ports?.get(portNum)
            val usbConnection = _usbManager?.openDevice(usbSerialDriver?.device)


            if (usbSerialPort != null && usbConnection != null && _binaryMessenger != null) {
                Log.i(TAG, "usbSerialPort $usbSerialPort")
                Log.i(TAG, "usbConnection $usbConnection")
                Log.i(TAG, "_binaryMessenger $_binaryMessenger")
                val listener =
                    UsbSerialListener(
                        usbSerialPort,
                        usbConnection,
                        _binaryMessenger!!,
                    )
                result.success(listener.getMethodChannelName())
                Log.i(TAG, "success serial construct")
                return
            }
            result.error(TAG, "Not an Serial device.", null)


        } catch (e: SecurityException) {
            if (allowAcquirePermission) {
                acquirePermission(device, cb)
                return
            } else {
                result.error(TAG, "Failed to acquire USB permission", null)
            }
        } catch (e: java.lang.Exception) {
            Log.e(TAG, e.stackTraceToString())
            result.error(TAG, "Failed to acquire USB device", null)
        }
    }

    private fun _serializeDevice(device: UsbDevice): HashMap<String, Any?> {
        val dev: HashMap<String, Any?> = HashMap()
        dev["deviceName"] = device.deviceName
        dev["vid"] = device.vendorId
        dev["pid"] = device.productId
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            dev["manufacturerName"] = device.manufacturerName
            dev["productName"] = device.productName
            dev["interfaceCount"] = device.interfaceCount
            /* if the app targets SDK >= android.os.Build.VERSION_CODES.Q and the app does not have permission to read from the device. */try {
                dev["serialNumber"] = device.serialNumber
            } catch (e: SecurityException) {
            }
        }
        dev["deviceId"] = device.deviceId
        return dev
    }

    private fun listDevices(result: Result) {
        val devices = _usbManager?.deviceList
        if (devices == null) {
            result.error(TAG, "Could not get USB device list", null)
            return
        }
        val transferDevices: MutableList<HashMap<String, Any?>> = ArrayList()
        for (device: UsbDevice in devices.values) {
            transferDevices.add(_serializeDevice(device))
        }
        result.success(transferDevices)
    }

    fun register(messenger: BinaryMessenger, context: Context) {
        _binaryMessenger = messenger
        _context = context
        _usbManager = _context?.getSystemService(android.content.Context.USB_SERVICE) as UsbManager
        _eventChannel = EventChannel(messenger, "usb_serial_for_android/usb_events")
        _eventChannel?.setStreamHandler(this)

        val filter = IntentFilter()
        filter.addAction(ACTION_USB_ATTACHED)
        filter.addAction(ACTION_USB_DETACHED)
        context.registerReceiver(_usbReceiver, filter)
    }

    fun unregister() {
        _context?.unregisterReceiver(_usbReceiver)
        _eventChannel = null
        _usbManager = null
        _context = null
        _binaryMessenger = null
    }

    private fun _createTyped(
        type: String?,
        vid: Int,
        pid: Int,
        deviceId: Int,
        result: Result,
        portNum: Int
    ) {
        val devices: Map<String, UsbDevice> = _usbManager?.deviceList!!

        for (device: UsbDevice in devices.values) {
            if (deviceId == device.deviceId || (device.vendorId == vid && device.productId == pid)) {
                openDevice(device, result, true, portNum, type)
                return
            }
        }
        result.error(TAG, "No such device", null)
    }


    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        register(flutterPluginBinding.binaryMessenger, flutterPluginBinding.applicationContext)
        _methodChannel =
            MethodChannel(flutterPluginBinding.binaryMessenger, "usb_serial_for_android")
        _methodChannel.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "listDevices" -> {
                listDevices(result)
            }
            "create" -> {
                _createTyped(
                    call.argument<String>("type"),
                    call.argument<Int>("vid")!!,
                    call.argument<Int>("pid")!!,
                    call.argument<Int>("deviceId")!!,
                    result,
                    4,
                )
            }
            else -> result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        _methodChannel.setMethodCallHandler(null)
        unregister()
    }


    /**
     * Flutter Stream handler
     */
    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        _eventSink = events
    }

    override fun onCancel(arguments: Any?) {
        _eventSink = null
    }

    companion object {
        val TAG = UsbSerialForAndroidPlugin::class.simpleName!!
        const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
        const val ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED"
        const val ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED"
    }
}
