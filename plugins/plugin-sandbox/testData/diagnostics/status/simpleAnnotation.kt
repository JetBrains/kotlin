// RUN_PIPELINE_TILL: BACKEND
import org.jetbrains.kotlin.plugin.sandbox.AllOpen

@AllOpen
class A {
    fun foo() {

    }
}

@AllOpen
class B : A() {
    override fun foo() {

    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, override */
