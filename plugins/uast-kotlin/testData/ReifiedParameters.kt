// !IGNORE_FIR

inline fun <reified T> functionWithLambda(t: T, process: (T) -> Int): Int = process(t)

inline fun <reified T> functionWithVararg(i: Int?, vararg t: T): T = t[0]

inline fun <reified T> T.functionWithReceiver(i: Int?): T = this

inline fun <reified T> T.`name with spaces`(i: Int?): T = this

inline fun <reified T> functionWithParamAnnotation(@Suppress("s") t: T): T = t

inline fun <reified T> functionUnresolved(@Suppress("s") t: Unresolved<T>): T = t
