// RUN_PIPELINE_TILL: BACKEND
import org.jetbrains.kotlin.plugin.sandbox.CompanionWithFoo

@CompanionWithFoo
class SomeClass

fun takeInt(x: Int) {}

fun test() {
    takeInt(SomeClass.foo())
    takeInt(SomeClass.Companion.foo())
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration */
