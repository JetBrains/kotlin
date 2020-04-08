interface I<T>

interface MyMap {
    fun <T : Any> get(`type`: I<T>): T?
}

class A(val map: MyMap) {
    fun <T : Any> foo(`type`: I<T>) {
        val value = map.get<caret><T>(`type`)
    }
}