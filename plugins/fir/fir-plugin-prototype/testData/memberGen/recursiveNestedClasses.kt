import org.jetbrains.kotlin.fir.plugin.B

@B
class SomeClass

fun test_1(x: SomeClass.Nested) {}
fun test_2(x: SomeClass.Nested.Nested) {}
fun test_3(x: SomeClass.Nested.Nested.Nested) {}