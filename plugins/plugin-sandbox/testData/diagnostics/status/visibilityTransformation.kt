// RUN_PIPELINE_TILL: BACKEND
import org.jetbrains.kotlin.plugin.sandbox.AllPublic
import org.jetbrains.kotlin.plugin.sandbox.Visibility

@AllPublic(Visibility.Protected)
class A {
    val x: String = ""

    fun foo() {}

    class Nested {
        fun bar() {

        }
    }
}

@AllPublic(Visibility.Private)
class B {
    val x: String = ""

    fun foo() {

    }

    class Nested {
        fun bar() {

        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nestedClass, propertyDeclaration, stringLiteral */
