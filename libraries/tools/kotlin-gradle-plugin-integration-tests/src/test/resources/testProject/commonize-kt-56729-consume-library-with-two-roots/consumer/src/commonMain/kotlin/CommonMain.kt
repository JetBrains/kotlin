object CommonMain {
    init {
        @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
        ProducerNativeCommonMain.fromCInterop().value
    }
}
