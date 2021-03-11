//ALLOW_AST_ACCESS
package test

public open class AnnotatedMethod() {
    public open @Deprecated("Deprecated in Java") fun f(): Unit { }
}
