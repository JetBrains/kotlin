package bar

import foo.AllOpenGenerated
import org.jetbrains.kotlin.plugin.sandbox.ExternalClassWithNested

@ExternalClassWithNested
class A {
    @ExternalClassWithNested
    class C
}