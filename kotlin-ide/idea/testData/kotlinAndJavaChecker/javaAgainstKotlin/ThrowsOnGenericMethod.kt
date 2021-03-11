package foo;
import java.io.IOException

class A {
    @Throws(IOException::class)
    fun <E> foo(y: E) {}
}

// ALLOW_AST_ACCESS
