/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/complex/use.2.kt
 */

package test

public class Data()

public data class Input(val d: Data) : Closeable {
    public fun data() : Int = 100
}

public  class Output(val d: Data) : Closeable {
    public fun doOutput(data: Int): Int = data
}

public interface Closeable {
    open public fun close() {}
}

public fun Input.copyTo(output: Output, size: Int): Int {
    return output.doOutput(this.data())
}

public inline fun <T: Closeable, R> T.use(block: (T)-> R) : R {
    var closed = false
    try {
        return block(this)
    } catch (e: Exception) {
        closed = true
        try {
            this.close()
        } catch (closeException: Exception) {

        }
        throw e
    } finally {
        if (!closed) {
            this.close()
        }
    }
}



