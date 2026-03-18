// SNIPPET

operator fun <E> Map<String, E>.getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): E {
    return this[property.name]!!
}

val a: Int by mapOf()
