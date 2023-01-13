package producer

actual class Producer {
    actual fun foo(value: String, optionalParameter: Boolean) = Unit
}

fun inProducerNativeMain() {
    producerSecondCommonMain()
    Producer().foo("") // <-  No value passed for parameter 'optionalParameter'
}