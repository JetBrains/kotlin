class WithModifiersOnAccessors {

    @get:Synchronized
    @set:Synchronized
    var sync = 0

    @get:Strictfp
    val strict = 0.0

    @Synchronized
    private fun methSync() {
    }

    @Strictfp
    protected fun methStrict() {
    }
}