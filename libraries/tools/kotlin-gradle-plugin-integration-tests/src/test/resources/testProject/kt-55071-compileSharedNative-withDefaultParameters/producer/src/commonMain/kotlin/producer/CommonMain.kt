package producer

expect class Producer constructor() {
    fun foo(value: String, optionalParameter: Boolean = false)
}