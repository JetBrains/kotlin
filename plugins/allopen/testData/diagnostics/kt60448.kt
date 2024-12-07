// FIR_IDENTICAL
// WITH_STDLIB
annotation class AllOpen

@AllOpen
class Test {
    @JvmName("g")
    private fun f() {}
}
