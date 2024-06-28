// WITH_STDLIB
// ISSUE: KT-58049

annotation class AllOpen

@AllOpen
class Test(
    val publicProp: String?,
    protected val protectedProp: String?,
    internal val internalProp: String?,
    private val privateProp: String?,
) {
    fun test() {
        checkNotNull(publicProp)
        checkNotNull(protectedProp)
        checkNotNull(internalProp)
        checkNotNull(privateProp)

        println(<!SMARTCAST_IMPOSSIBLE!>publicProp<!>.length)
        println(<!SMARTCAST_IMPOSSIBLE!>protectedProp<!>.length)
        println(<!SMARTCAST_IMPOSSIBLE!>internalProp<!>.length)
        println(privateProp.length)
    }
}
