package test

class Foo

fun main() {
    val cls = Foo::class
    val cls2 = Foo::class
    val cls3 = Foo::class.java

    //Breakpoint!
    foo(cls2)
}

private fun foo(a: Any?) {}

// EXPRESSION: cls
// RESULT: instance of java.lang.Class(reflected class=test.Foo, id=ID): Ljava/lang/Class;

// EXPRESSION: cls2
// RESULT: instance of kotlin.jvm.internal.ClassReference(id=ID): Lkotlin/jvm/internal/ClassReference;

// EXPRESSION: cls3
// RESULT: instance of java.lang.Class(reflected class=test.Foo, id=ID): Ljava/lang/Class;