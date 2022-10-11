package com.eyanef.usb_serial_for_android

import android.hardware.usb.UsbDeviceConnection
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.util.SerialInputOutputManager
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler

class UsbSerialListener(
    private var _usbSerialPort: UsbSerialPort?,
    private var _usbConnection: UsbDeviceConnection?,
    _binaryMessenger: BinaryMessenger,
) : MethodCallHandler, EventChannel.StreamHandler {

    private var _eventSink: EventChannel.EventSink? = null;
    private val _handler: Handler = Handler(Looper.getMainLooper())
    private var _serialListener: SerialListener = SerialListener()
    private var _ioManager: SerialInputOutputManager? = null

    //        SerialInputOutputManager(_usbSerialPort, _serialListener)
    private val _methodChannelName: String =
        "usb_serial_for_android/UsbSerialListener"

    init {
        val channel = MethodChannel(_binaryMessenger, _methodChannelName)
        channel.setMethodCallHandler(this)
        val eventChannel =
            EventChannel(_binaryMessenger, "$_methodChannelName/stream")
        eventChannel.setStreamHandler(this)
    }

    fun getMethodChannelName(): String {
        return _methodChannelName
    }


    private fun _setPortParameters(baudRate: Int, dataBits: Int, stopBits: Int, parity: Int) {
        _usbSerialPort!!.setParameters(baudRate, dataBits, stopBits, parity)
    }

    private fun _open(): Boolean {
        if (!_usbSerialPort!!.isOpen) {
            _usbSerialPort!!.open(_usbConnection)
            return true
        } else {
            return false
        }
    }


    private fun _close(): Boolean {
        _usbSerialPort!!.close()
        return true
    }

    private fun _connect(): Boolean {
        _ioManager = SerialInputOutputManager(_usbSerialPort, SerialListener())
        if (_usbSerialPort!!.isOpen) {
            _ioManager!!.start()
            return true
        }
        return false
    }

    private fun _disconnect() {
        _ioManager?.let {
            it.listener = null
            it.stop()
            _ioManager = null
        }
        _usbSerialPort?.let {
            it.dtr = false
            it.rts = false
            it.close()
        }
        _usbConnection?.let {
            it.close()
            _usbConnection = null
        }

    }

    private fun _write(data: ByteArray, timeout: Int) {
        _usbSerialPort!!.write(data, timeout)
    }

    companion object {
        val TAG = UsbSerialListener::class.simpleName
    }

    /**
     * Listener implementation
     */
    inner class SerialListener : SerialInputOutputManager.Listener {
        override fun onNewData(data: ByteArray?) {
            if (data != null && _eventSink != null) {
                _handler.post(kotlinx.coroutines.Runnable {
                    _eventSink?.success(data)
                })
            }
        }

        override fun onRunError(e: Exception?) {
            Log.e(TAG, "onRunError")
        }
    }

    /**
     * Flutter method call
     */
    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "close" -> {
                result.success(_close())
            }
            "open" -> {
                result.success(_open())
            }
            "write" -> {
                _write(call.argument<ByteArray>("data")!!, call.argument<Int>("timeout")!!)
                result.success(true)
            }
            "setPortParameters" -> {
                _setPortParameters(
                    call.argument<Int>("baudRate")!!,
                    call.argument<Int>("dataBits")!!,
                    call.argument<Int>("stopBits")!!,
                    call.argument<Int>("parity")!!,
                )
                result.success(true)
            }
            "setDTR" -> {
                _usbSerialPort!!.dtr = call.argument<Boolean>("value")!!
                result.success(null)
            }
            "setRTS" -> {
                _usbSerialPort!!.rts = call.argument<Boolean>("value")!!
                result.success(null)
            }
            "connect" -> {
                result.success(_connect())
            }
            "disconnect" -> {
                result.success(_disconnect())
            }
            else -> result.notImplemented()

        }
    }

    /**
     * Flutter events
     */
    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        _eventSink = events
    }

    override fun onCancel(arguments: Any?) {
        _eventSink = null
    }
}