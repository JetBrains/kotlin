package collections

fun <V : Any> Iterator<V>.nextOrNull(): V? = if (hasNext()) next() else null