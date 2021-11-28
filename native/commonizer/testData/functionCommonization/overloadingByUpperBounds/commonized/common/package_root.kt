expect interface I1
expect interface I2
expect interface I3<T>

expect fun <T : I1> functionWithValueParameter(value: T): Unit
expect fun <T : I2> functionWithValueParameter(value: T): Unit
expect fun <T : I3<String>> functionWithValueParameter(value: T): Unit
expect fun <T : I3<in String>> functionWithValueParameter(value: T): Unit
expect fun <T : I3<out String>> functionWithValueParameter(value: T): Unit
expect fun <T : I3<Int>> functionWithValueParameter(value: T): Unit
expect fun <T : I3<in Int>> functionWithValueParameter(value: T): Unit
expect fun <T : I3<out Int>> functionWithValueParameter(value: T): Unit
expect fun <T : I3<Any>> functionWithValueParameter(value: T): Unit
expect fun <T : I3<in Any>> functionWithValueParameter(value: T): Unit
expect fun <T : I3<out Any>> functionWithValueParameter(value: T): Unit
expect fun <T : I3<Any?>> functionWithValueParameter(value: T): Unit
expect fun <T : I3<*>> functionWithValueParameter(value: T): Unit
expect fun <T : Any> functionWithValueParameter(value: T): Unit
expect fun <T> functionWithValueParameter(value: T): Unit
expect fun functionWithValueParameter(value: I1): Unit
expect fun functionWithValueParameter(value: I2): Unit
expect fun functionWithValueParameter(value: Any): Unit
expect fun functionWithValueParameter(value: Any?): Unit
expect fun functionWithValueParameter(): Unit

expect fun <T : I1> T.functionWithReceiver(): Unit
expect fun <T : I2> T.functionWithReceiver(): Unit
expect fun <T : I3<String>> T.functionWithReceiver(): Unit
expect fun <T : I3<in String>> T.functionWithReceiver(): Unit
expect fun <T : I3<out String>> T.functionWithReceiver(): Unit
expect fun <T : I3<Int>> T.functionWithReceiver(): Unit
expect fun <T : I3<in Int>> T.functionWithReceiver(): Unit
expect fun <T : I3<out Int>> T.functionWithReceiver(): Unit
expect fun <T : I3<Any>> T.functionWithReceiver(): Unit
expect fun <T : I3<in Any>> T.functionWithReceiver(): Unit
expect fun <T : I3<out Any>> T.functionWithReceiver(): Unit
expect fun <T : I3<Any?>> T.functionWithReceiver(): Unit
expect fun <T : I3<*>> T.functionWithReceiver(): Unit
expect fun <T : Any> T.functionWithReceiver(): Unit
expect fun <T> T.functionWithReceiver(): Unit
expect fun I1.functionWithReceiver(): Unit
expect fun I2.functionWithReceiver(): Unit
expect fun Any.functionWithReceiver(): Unit
expect fun Any?.functionWithReceiver(): Unit
expect fun functionWithReceiver(): Unit
