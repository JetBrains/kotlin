import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

object Jvm {
    operator fun invoke() = runBlocking {
        withContext(Dispatchers.Default) {
            println("Jvm: OK!")
        }
    }
}