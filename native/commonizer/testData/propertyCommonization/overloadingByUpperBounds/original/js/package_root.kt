interface I1
interface I2
interface I3<T>

object Holder {
    val <T : I1> T.propertyWithReceiver: Unit get() = Unit
    val <T : I2> T.propertyWithReceiver: Unit get() = Unit
    val <T : I3<String>> T.propertyWithReceiver: Unit get() = Unit
    val <T : I3<in String>> T.propertyWithReceiver: Unit get() = Unit
    val <T : I3<out String>> T.propertyWithReceiver: Unit get() = Unit
    val <T : I3<Int>> T.propertyWithReceiver: Unit get() = Unit
    val <T : I3<in Int>> T.propertyWithReceiver: Unit get() = Unit
    val <T : I3<out Int>> T.propertyWithReceiver: Unit get() = Unit
    val <T : I3<Any>> T.propertyWithReceiver: Unit get() = Unit
    val <T : I3<in Any>> T.propertyWithReceiver: Unit get() = Unit
    val <T : I3<out Any>> T.propertyWithReceiver: Unit get() = Unit
    val <T : I3<Any?>> T.propertyWithReceiver: Unit get() = Unit
    val <T : I3<*>> T.propertyWithReceiver: Unit get() = Unit
    val <T : Any> T.propertyWithReceiver: Unit get() = Unit
    val <T> T.propertyWithReceiver: Unit get() = Unit
    val I1.propertyWithReceiver: Unit get() = Unit
    val I2.propertyWithReceiver: Unit get() = Unit
    val Any.propertyWithReceiver: Unit get() = Unit
    val Any?.propertyWithReceiver: Unit get() = Unit
    val propertyWithReceiver: Unit get() = Unit
}
