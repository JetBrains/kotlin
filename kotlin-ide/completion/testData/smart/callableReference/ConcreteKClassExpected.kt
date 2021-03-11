import kotlin.reflect.KClass

open class A<T : Any>(val kClass: KClass<T>)

class B : A<String>(<caret>)

// EXIST: { lookupString: "String", itemText: "String::class", attributes: "" }
