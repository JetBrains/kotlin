actual interface I1
actual interface I2
actual interface I3<T>

actual object Holder {
    actual val <T : I1> T.propertyWithReceiver: Unit get() = Unit
    actual val <T : I2> T.propertyWithReceiver: Unit get() = Unit
    actual val <T : I3<String>> T.propertyWithReceiver: Unit get() = Unit
    actual val <T : I3<in String>> T.propertyWithReceiver: Unit get() = Unit
    actual val <T : I3<out String>> T.propertyWithReceiver: Unit get() = Unit
    actual val <T : I3<Int>> T.propertyWithReceiver: Unit get() = Unit
    actual val <T : I3<in Int>> T.propertyWithReceiver: Unit get() = Unit
    actual val <T : I3<out Int>> T.propertyWithReceiver: Unit get() = Unit
    actual val <T : I3<Any>> T.propertyWithReceiver: Unit get() = Unit
    actual val <T : I3<in Any>> T.propertyWithReceiver: Unit get() = Unit
    actual val <T : I3<out Any>> T.propertyWithReceiver: Unit get() = Unit
    actual val <T : I3<Any?>> T.propertyWithReceiver: Unit get() = Unit
    actual val <T : I3<*>> T.propertyWithReceiver: Unit get() = Unit
    actual val <T : Any> T.propertyWithReceiver: Unit get() = Unit
    actual val <T> T.propertyWithReceiver: Unit get() = Unit
    actual val I1.propertyWithReceiver: Unit get() = Unit
    actual val I2.propertyWithReceiver: Unit get() = Unit
    actual val Any.propertyWithReceiver: Unit get() = Unit
    actual val Any?.propertyWithReceiver: Unit get() = Unit
    actual val propertyWithReceiver: Unit get() = Unit
}
