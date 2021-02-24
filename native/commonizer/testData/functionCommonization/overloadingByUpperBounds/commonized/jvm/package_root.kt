actual interface I1
actual interface I2
actual interface I3<T>

actual fun <T : I1> functionWithValueParameter(value: T) = Unit
actual fun <T : I2> functionWithValueParameter(value: T) = Unit
actual fun <T : I3<String>> functionWithValueParameter(value: T) = Unit
actual fun <T : I3<in String>> functionWithValueParameter(value: T) = Unit
actual fun <T : I3<out String>> functionWithValueParameter(value: T) = Unit
actual fun <T : I3<Int>> functionWithValueParameter(value: T) = Unit
actual fun <T : I3<in Int>> functionWithValueParameter(value: T) = Unit
actual fun <T : I3<out Int>> functionWithValueParameter(value: T) = Unit
actual fun <T : I3<Any>> functionWithValueParameter(value: T) = Unit
actual fun <T : I3<in Any>> functionWithValueParameter(value: T) = Unit
actual fun <T : I3<out Any>> functionWithValueParameter(value: T) = Unit
actual fun <T : I3<Any?>> functionWithValueParameter(value: T) = Unit
actual fun <T : I3<*>> functionWithValueParameter(value: T) = Unit
actual fun <T : Any> functionWithValueParameter(value: T) = Unit
actual fun <T> functionWithValueParameter(value: T) = Unit
actual fun functionWithValueParameter(value: I1) = Unit
actual fun functionWithValueParameter(value: I2) = Unit
actual fun functionWithValueParameter(value: Any) = Unit
actual fun functionWithValueParameter(value: Any?) = Unit
actual fun functionWithValueParameter() = Unit

actual fun <T : I1> T.functionWithReceiver() = Unit
actual fun <T : I2> T.functionWithReceiver() = Unit
actual fun <T : I3<String>> T.functionWithReceiver() = Unit
actual fun <T : I3<in String>> T.functionWithReceiver() = Unit
actual fun <T : I3<out String>> T.functionWithReceiver() = Unit
actual fun <T : I3<Int>> T.functionWithReceiver() = Unit
actual fun <T : I3<in Int>> T.functionWithReceiver() = Unit
actual fun <T : I3<out Int>> T.functionWithReceiver() = Unit
actual fun <T : I3<Any>> T.functionWithReceiver() = Unit
actual fun <T : I3<in Any>> T.functionWithReceiver() = Unit
actual fun <T : I3<out Any>> T.functionWithReceiver() = Unit
actual fun <T : I3<Any?>> T.functionWithReceiver() = Unit
actual fun <T : I3<*>> T.functionWithReceiver() = Unit
actual fun <T : Any> T.functionWithReceiver() = Unit
actual fun <T> T.functionWithReceiver() = Unit
actual fun I1.functionWithReceiver() = Unit
actual fun I2.functionWithReceiver() = Unit
actual fun Any.functionWithReceiver() = Unit
actual fun Any?.functionWithReceiver() = Unit
actual fun functionWithReceiver() = Unit
