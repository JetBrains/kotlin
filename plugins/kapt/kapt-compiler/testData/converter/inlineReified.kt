import kotlin.reflect.typeOf

inline fun <reified T> inlineReifiedFun(t: T) = typeOf<T>()
