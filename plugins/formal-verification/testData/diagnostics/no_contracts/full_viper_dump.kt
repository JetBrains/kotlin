// FULL_VIPER_DUMP
// NEVER_VALIDATE

import org.jetbrains.kotlin.formver.plugin.DumpExpEmbeddings
// Check class generation.
class Foo(val x: Int)

@DumpExpEmbeddings
fun <!EXP_EMBEDDING, VIPER_TEXT!>f<!>() {
    val foo = Foo(0)
}