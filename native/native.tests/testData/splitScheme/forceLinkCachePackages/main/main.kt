import org.jetbrains.kotlin.native.test.facade.facadeProbe

fun main() {
    check(facadeProbe() == 42)
}
