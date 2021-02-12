expect interface I1
expect interface I2
expect interface I3<T>

expect object Holder {
    val <T : I1> T.propertyWithReceiver: Unit
    val <T : I2> T.propertyWithReceiver: Unit
    val <T : I3<String>> T.propertyWithReceiver: Unit
    val <T : I3<in String>> T.propertyWithReceiver: Unit
    val <T : I3<out String>> T.propertyWithReceiver: Unit
    val <T : I3<Int>> T.propertyWithReceiver: Unit
    val <T : I3<in Int>> T.propertyWithReceiver: Unit
    val <T : I3<out Int>> T.propertyWithReceiver: Unit
    val <T : I3<Any>> T.propertyWithReceiver: Unit
    val <T : I3<in Any>> T.propertyWithReceiver: Unit
    val <T : I3<out Any>> T.propertyWithReceiver: Unit
    val <T : I3<Any?>> T.propertyWithReceiver: Unit
    val <T : I3<*>> T.propertyWithReceiver: Unit
    val <T : Any> T.propertyWithReceiver: Unit
    val <T> T.propertyWithReceiver: Unit
    val I1.propertyWithReceiver: Unit
    val I2.propertyWithReceiver: Unit
    val Any.propertyWithReceiver: Unit
    val Any?.propertyWithReceiver: Unit
    val propertyWithReceiver: Unit
}
