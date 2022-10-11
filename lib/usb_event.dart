import 'package:usb_serial_for_android/usb_device.dart';

class UsbEvent {
  /// Event passed to usbEventStream when a USB device is attached.
  static const String ACTION_USB_ATTACHED =
      "android.hardware.usb.action.USB_DEVICE_ATTACHED";

  /// Event passed to usbEventStream when a USB device is detached.
  static const String ACTION_USB_DETACHED =
      "android.hardware.usb.action.USB_DEVICE_DETACHED";

  /// either ACTION_USB_ATTACHED or ACTION_USB_DETACHED
  String? event;

  /// The device for which the event was fired.
  UsbDevice? device;

  @override
  String toString() {
    return "UsbEvent: $event, $device";
  }
}
