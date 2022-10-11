import 'package:collection/collection.dart';

class Equality {
  static final Function deepEq = const DeepCollectionEquality().equals;
  static final Function deepHash = const DeepCollectionEquality().hash;
}
