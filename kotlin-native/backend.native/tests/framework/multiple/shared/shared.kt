@file:OptIn(ObsoleteWorkersApi::class)
import kotlin.native.concurrent.*

object RuntimeState {
    fun produceChange() {
        Worker.current.executeAfter {}
    }

    fun consumeChange(): Boolean {
        return Worker.current.processQueue()
    }
}

// Note: this assumes that IntRange class is not exposed by the enclosing framework.
fun getUnexposedStdlibClassInstance(): Any = 0..2
