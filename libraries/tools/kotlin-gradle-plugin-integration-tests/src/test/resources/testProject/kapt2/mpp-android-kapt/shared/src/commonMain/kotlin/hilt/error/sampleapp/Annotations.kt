package hilt.error.sampleapp

expect annotation class HiltViewModel()

expect interface AutoCloseable {
    fun close()
}

expect interface Closeable : AutoCloseable

expect abstract class ViewModel()

expect annotation class Inject()
