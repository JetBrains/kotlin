// RUNTIME_WITH_FULL_JDK
@FunctionalInterface
interface MyRunnable {
    fun <T> process(t: T)
}