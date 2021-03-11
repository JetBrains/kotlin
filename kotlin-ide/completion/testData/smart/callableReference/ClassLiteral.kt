import kotlin.reflect.KClass

fun<T : Any> foo(p: KClass<T>){}

fun bar() {
    foo(String::<caret>)
}

// EXIST: { lookupString: "class", itemText: "class", attributes: "bold" }
// NOTHING_ELSE
