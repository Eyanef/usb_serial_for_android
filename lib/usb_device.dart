import 'package:usb_serial_for_android/usb_port.dart';
import 'package:usb_serial_for_android/usb_serial_for_android.dart';

class UsbDevice {
  final String deviceName;

  /// Vendor Id
  final int? vid;

  /// Product Id
  final int? pid;
  final String? productName;
  final String? manufacturerName;

  /// The device id is unique to this Usb Device until it is unplugged.
  /// when replugged this ID will be different.
  final int? deviceId;

  // save the device port
  UsbPort? _port;
  // port getter
  UsbPort? get port => _port;

  /// The Serial number from the USB device.
  final String? serial;

  /// The number of interfaces on this UsbPort
  final int? interfaceCount;

  UsbDevice(this.deviceName, this.vid, this.pid, this.productName,
      this.manufacturerName, this.deviceId, this.serial, this.interfaceCount);

  static UsbDevice fromJSON(dynamic json) {
    return UsbDevice(
        json["deviceName"],
        json["vid"],
        json["pid"],
        json["productName"],
        json["manufacturerName"],
        json["deviceId"],
        json["serialNumber"],
        json["interfaceCount"]);
  }

  @override
  String toString() {
    return "UsbDevice: $deviceName, ${vid!.toRadixString(16)}-${pid!.toRadixString(16)} $productName, $manufacturerName $serial";
  }

  /// Creates a UsbPort from the UsbDevice.
  ///
  /// [type] can be any of the [UsbSerial.CDC], [UsbSerial.CH34x], [UsbSerial.CP210x], [UsbSerial.FTDI] or [USBSerial.PL2303] values or empty for auto detection.
  /// returns the new UsbPort or throws an error on open failure.

  Future<UsbPort?> create([String? type, int? portNum]) async {
    _port = await UsbSerial.createFromDeviceId(deviceId, type, portNum);
    return _port;
  }

  @override
  bool operator ==(other) {
    if (!(other is UsbDevice)) {
      return false;
    }
    return this.deviceName == other.deviceName;
  }

  @override
  int get hashCode {
    return this.deviceName.hashCode;
  }
}
