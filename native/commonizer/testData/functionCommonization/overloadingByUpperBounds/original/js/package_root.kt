interface I1
interface I2
interface I3<T>

fun <T : I1> functionWithValueParameter(value: T) = Unit
fun <T : I2> functionWithValueParameter(value: T) = Unit
fun <T : I3<String>> functionWithValueParameter(value: T) = Unit
fun <T : I3<in String>> functionWithValueParameter(value: T) = Unit
fun <T : I3<out String>> functionWithValueParameter(value: T) = Unit
fun <T : I3<Int>> functionWithValueParameter(value: T) = Unit
fun <T : I3<in Int>> functionWithValueParameter(value: T) = Unit
fun <T : I3<out Int>> functionWithValueParameter(value: T) = Unit
fun <T : I3<Any>> functionWithValueParameter(value: T) = Unit
fun <T : I3<in Any>> functionWithValueParameter(value: T) = Unit
fun <T : I3<out Any>> functionWithValueParameter(value: T) = Unit
fun <T : I3<Any?>> functionWithValueParameter(value: T) = Unit
fun <T : I3<*>> functionWithValueParameter(value: T) = Unit
fun <T : Any> functionWithValueParameter(value: T) = Unit
fun <T> functionWithValueParameter(value: T) = Unit
fun functionWithValueParameter(value: I1) = Unit
fun functionWithValueParameter(value: I2) = Unit
fun functionWithValueParameter(value: Any) = Unit
fun functionWithValueParameter(value: Any?) = Unit
fun functionWithValueParameter() = Unit

fun <T : I1> T.functionWithReceiver() = Unit
fun <T : I2> T.functionWithReceiver() = Unit
fun <T : I3<String>> T.functionWithReceiver() = Unit
fun <T : I3<in String>> T.functionWithReceiver() = Unit
fun <T : I3<out String>> T.functionWithReceiver() = Unit
fun <T : I3<Int>> T.functionWithReceiver() = Unit
fun <T : I3<in Int>> T.functionWithReceiver() = Unit
fun <T : I3<out Int>> T.functionWithReceiver() = Unit
fun <T : I3<Any>> T.functionWithReceiver() = Unit
fun <T : I3<in Any>> T.functionWithReceiver() = Unit
fun <T : I3<out Any>> T.functionWithReceiver() = Unit
fun <T : I3<Any?>> T.functionWithReceiver() = Unit
fun <T : I3<*>> T.functionWithReceiver() = Unit
fun <T : Any> T.functionWithReceiver() = Unit
fun <T> T.functionWithReceiver() = Unit
fun I1.functionWithReceiver() = Unit
fun I2.functionWithReceiver() = Unit
fun Any.functionWithReceiver() = Unit
fun Any?.functionWithReceiver() = Unit
fun functionWithReceiver() = Unit
