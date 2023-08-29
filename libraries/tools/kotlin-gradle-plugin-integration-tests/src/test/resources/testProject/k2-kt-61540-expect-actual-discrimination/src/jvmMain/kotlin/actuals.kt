import java.util.concurrent.locks.ReentrantLock

actual class MyExpectClass {
    actual val myExpectClassProperty: Int = 0
    val myJvmProperty: ReentrantLock = ReentrantLock()
}