package kotlin

public expect inline fun <reified T : Any> VArray<T?>.filterNotNull(): List<T>

