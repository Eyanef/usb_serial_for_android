import 'dart:ffi';

import 'package:flutter/services.dart';
import 'package:usb_serial_for_android/equality.dart';
import 'package:usb_serial_for_android/types.dart';

class UsbPort extends AsyncDataSinkSource {
  /// Constant to configure port with 5 databits.
  static const int DATABITS_5 = 5;

  /// Constant to configure port with 6 databits.
  static const int DATABITS_6 = 6;

  /// Constant to configure port with 7 databits.
  static const int DATABITS_7 = 7;

  /// Constant to configure port with 8 databits.
  static const int DATABITS_8 = 8;

  /// Constant to configure port with no flow control
  static const int FLOW_CONTROL_OFF = 0;

  /// Constant to configure port with flow control RTS/CTS
  static const int FLOW_CONTROL_RTS_CTS = 1;

  /// Constant to configure port with flow contorl DSR / DTR
  static const int FLOW_CONTROL_DSR_DTR = 2;

  /// Constant to configure port with flow control XON XOFF
  static const int FLOW_CONTROL_XON_XOFF = 3;

  /// Constant to configure port with parity none
  static const int PARITY_NONE = 0;

  /// Constant to configure port with event parity.
  static const int PARITY_EVEN = 2;

  /// Constant to configure port with odd parity.
  static const int PARITY_ODD = 1;

  /// Constant to configure port with mark parity.
  static const int PARITY_MARK = 3;

  /// Constant to configure port with space parity.
  static const int PARITY_SPACE = 4;

  /// Constant to configure port with 1 stop bits
  static const int STOPBITS_1 = 1;

  /// Constant to configure port with 1.5 stop bits
  static const int STOPBITS_1_5 = 3;

  /// Constant to configure port with 2 stop bits
  static const int STOPBITS_2 = 2;

  final MethodChannel _channel;
  final EventChannel _eventChannel;
  Stream<Uint8List>? _inputStream;

  int _baudRate = 115200;
  int _dataBits = UsbPort.DATABITS_8;
  int _stopBits = UsbPort.STOPBITS_1;
  int _parity = UsbPort.PARITY_NONE;

  int _flowControl = UsbPort.FLOW_CONTROL_OFF;

  bool _dtr = false;
  bool _rts = false;

  int get baudRate => _baudRate;
  int get dataBits => _dataBits;
  int get stopBits => _stopBits;
  int get parity => _parity;

  UsbPort._internal(this._channel, this._eventChannel);

  /// Factory to create UsbPort object.
  ///
  /// You don't need to use this directly as you get UsbPort from
  /// [UsbDevice.create].
  factory UsbPort(String methodChannelName) {
    return UsbPort._internal(MethodChannel(methodChannelName),
        EventChannel("$methodChannelName/stream"));
  }

  /// returns the asynchronous input stream.
  ///
  /// Example
  ///
  /// ```dart
  /// UsbPort port = await device.create();
  /// await port.open();
  /// port.inputStream.listen( (Uint8List data) { print(data); } );
  /// ```
  ///
  /// This will print out the data as it arrives from the uart.
  ///
  @override
  Stream<Uint8List>? get inputStream {
    _inputStream ??= _eventChannel
        .receiveBroadcastStream()
        .map<Uint8List>((dynamic value) => value);
    return _inputStream;
  }

  /// Opens the uart communication channel.
  ///
  /// returns true if successful or false if failed.
  Future<bool> open() async {
    return await _channel.invokeMethod("open");
  }

  Future<bool> connect() async {
    return await _channel.invokeMethod("connect");
  }

  /// Closes the com port.
  Future<bool> close() async {
    return await _channel.invokeMethod("close");
  }

  /// Sets or clears the DTR port to value [dtr].
  Future<void> setDTR(bool dtr) async {
    _dtr = dtr;
    return await _channel.invokeMethod("setDTR", {"value": dtr});
  }

  /// Sets or clears the RTS port to value [rts].
  Future<void> setRTS(bool rts) async {
    _rts = rts;
    return await _channel.invokeMethod("setRTS", {"value": rts});
  }

  /// Asynchronously writes [data].
  @override
  Future<void> write(Uint8List data, {int timeout = 1000}) async {
    return await _channel
        .invokeMethod("write", {"data": data, "timeout": timeout});
  }

  /// Sets the port parameters to the requested values.
  ///
  /// ```dart
  /// _port.setPortParameters(115200, UsbPort.DATABITS_8, UsbPort.STOPBITS_1, UsbPort.PARITY_NONE);
  /// ```
  Future<void> setPortParameters(
      int baudRate, int dataBits, int stopBits, int parity) async {
    _baudRate = baudRate;
    _dataBits = dataBits;
    _stopBits = stopBits;
    _parity = parity;

    return await _channel.invokeMethod("setPortParameters", {
      "baudRate": baudRate,
      "dataBits": dataBits,
      "stopBits": stopBits,
      "parity": parity
    });
  }

  /// Sets the flow control parameter.
  Future<void> setFlowControl(int flowControl) async {
    _flowControl = flowControl;
    return await _channel
        .invokeMethod("setFlowControl", {"flowControl": flowControl});
  }

  /// return string name of databits
  String dataBitToString() {
    switch (this._dataBits) {
      case (5):
        return "DATABITS_5";
      case (6):
        return "DATABITS_6";
      case (7):
        return "DATABITS_7";
      case (8):
        return "DATABITS_8";
      default:
        return "unknown";
    }
  }

  /// return string name of flowcontrol
  String flowControlToString() {
    switch (_flowControl) {
      case (0):
        return "FLOW_CONTROL_OFF";
      case (1):
        return "FLOW_CONTROL_RTS_CTS";
      case (2):
        return "FLOW_CONTROL_DSR_DTR";
      case (3):
        return "FLOW_CONTROL_XON_XOFF";
      default:
        return "unknown";
    }
  }

  /// return string name of parity
  String parityToString() {
    switch (_parity) {
      case (0):
        return "PARITY_NONE";
      case (1):
        return "PARITY_ODD";
      case (2):
        return "PARITY_EVEN";
      case (3):
        return "PARITY_MARK";
      case (4):
        return "PARITY_SPACE";
      default:
        return "unknown";
    }
  }

  /// return string name of stop bits
  String stopBitsToString() {
    switch (_stopBits) {
      case (1):
        return "STOPBITS_1";
      case (2):
        return "STOPBITS_2";
      case (3):
        return "STOPBITS_1_5";
      default:
        return "unknown";
    }
  }

  List<Object> get _props =>
      [_baudRate, _dataBits, _stopBits, _parity, _flowControl, _rts, _dtr];

  @override
  bool operator ==(other) {
    if (!(other is UsbPort)) {
      return false;
    }
    return Equality.deepEq(_props, other._props);
  }

  @override
  int get hashCode {
    return Equality.deepHash(_props);
  }

  @override
  String toString() {
    return "Buad rate : $_baudRate, Data bits : ${dataBitToString()}, Stop bits : ${stopBitsToString()}, Parity : ${parityToString()}, Flow control : ${flowControlToString()}, RTS : ${_rts.toString()}, DTR : ${_dtr.toString()} ";
  }
}
