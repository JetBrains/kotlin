// RUN_PIPELINE_TILL: FRONTEND
import org.jetbrains.kotlin.plugin.sandbox.GenerateSamInterfaceFunction

@GenerateSamInterfaceFunction
fun interface Foo

fun makeFoo(foo: Foo) {}

fun main() {
    makeFoo(<!ARGUMENT_TYPE_MISMATCH!>{ }<!>)
}

/* GENERATED_FIR_TAGS: funInterface, functionDeclaration, interfaceDeclaration, lambdaLiteral */
