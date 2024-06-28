// RENDER_PREDICATES
// NEVER_VALIDATE

import org.jetbrains.kotlin.formver.plugin.Unique

class T()

open class S()

class Foo(val w: Int, var x: Int, @property:Unique val y: T, @property:Unique var z: T) : S()

fun <!VIPER_TEXT!>use_unique_foo<!>(@Unique foo: Foo) {}
