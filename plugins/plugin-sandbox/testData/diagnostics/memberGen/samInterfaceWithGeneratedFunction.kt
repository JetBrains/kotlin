// RUN_PIPELINE_TILL: BACKEND
import org.jetbrains.kotlin.plugin.sandbox.GenerateSamInterfaceFunction

@GenerateSamInterfaceFunction
fun interface Foo

fun makeFoo(foo: Foo) {}

fun main() {
    makeFoo { }
}

/* GENERATED_FIR_TAGS: funInterface, functionDeclaration, interfaceDeclaration, lambdaLiteral */
