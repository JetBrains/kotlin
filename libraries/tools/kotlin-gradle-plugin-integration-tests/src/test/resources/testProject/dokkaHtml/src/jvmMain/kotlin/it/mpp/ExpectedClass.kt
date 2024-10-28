package it.mpp

actual class ExpectedClass {
    actual val platform: String = "jvm"

    /**
     * This function can only be used by JVM consumers
     */
    fun jvmOnlyFunction() = Unit

}

actual class ExpectedClass2 {
    actual val platform: String = "jvm"
}
