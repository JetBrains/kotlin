/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/complex/with.2.kt
 */

package test

import kotlin.InlineOption.*

public class Data()

public class Input(val d: Data) : Closeable {
    public fun data() : Int = 100
}
public  class Output(val d: Data) : Closeable {
    public fun doOutput(data: Int): Int = data
}

public interface Closeable {
    open public fun close() {}
}

public inline fun <R> use(block: ()-> R) : R {
    return block()
}

public fun <R> useNoInline(block: ()-> R) : R {
    return block()
}


public fun Input.copyTo(output: Output, size: Int): Int {
    return output.doOutput(this.data())
}

public inline fun with2<T>(receiver : T, inlineOptions(ONLY_LOCAL_RETURN) body :  T.() -> Unit) : Unit = {receiver.body()}()
