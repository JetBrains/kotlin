import org.jetbrains.kotlin.fir.plugin.A

@A
class SomeClass

fun test() {
    TopLevelSomeClass.hello()
}