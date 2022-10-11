import 'package:flutter/services.dart';
import 'package:usb_serial_for_android/usb_device.dart';
import 'package:usb_serial_for_android/usb_event.dart';
import 'package:usb_serial_for_android/usb_port.dart';

class UsbSerial {
  /// CDC hardware type. Used by [create]
  // ignore: constant_identifier_names
  static const String CDC = "cdc";

  /// CH34X hardware type. Used by [create]
  // ignore: constant_identifier_names
  static const String CH34x = "ch34x";

  /// CP210x hardware type. Used by [create]
  // ignore: constant_identifier_names
  static const String CP21xx = "cp21xx";

  /// FTDI Hardware USB to Uart bridge. (Very common) Used by [create]
  // ignore: constant_identifier_names
  static const String FTDI = "ftdi";

  static const MethodChannel _channel = MethodChannel('usb_serial_for_android');
  static const EventChannel _eventChannel =
      EventChannel('usb_serial_for_android/usb_events');
  static Stream<UsbEvent>? _eventStream;

  static Future<UsbPort?> createFromDeviceId(int? deviceId,
      [String? type, int? portNum]) async {
    String? methodChannelName = await _channel.invokeMethod("create", {
      "type": type,
      "vid": -1,
      "pid": -1,
      "deviceId": deviceId,
      "portNum": portNum
    });

    if (methodChannelName == null) {
      return null;
    }

    return UsbPort(methodChannelName);
  }

  static Future<List<UsbDevice>> listDevices() async {
    List<dynamic> devices = await (_channel.invokeMethod("listDevices"));
    return devices.map<UsbDevice>(UsbDevice.fromJSON).toList();
  }

  static Stream<UsbEvent>? get usbEventStream {
    _eventStream ??=
        _eventChannel.receiveBroadcastStream().map<UsbEvent>((value) {
      UsbEvent msg = UsbEvent();
      msg.device = UsbDevice.fromJSON(value);
      msg.event = value["event"];
      return msg;
    });
    return _eventStream;
  }
}
