//ALLOW_AST_ACCESS
package test

public interface A<T> {
    fun bar(): T
    fun foo(): T = bar()
}

public class B : A<String> {
    override fun bar() = ""
}
