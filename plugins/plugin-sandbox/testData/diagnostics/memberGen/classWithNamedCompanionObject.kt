// RUN_PIPELINE_TILL: CODEGEN
import org.jetbrains.kotlin.plugin.sandbox.CompanionWithFoo

@CompanionWithFoo
class SomeClass {
    companion object Named
}

fun takeInt(x: Int) {}

fun test() {
    takeInt(SomeClass.foo())
    takeInt(SomeClass.Named.foo())
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration */
