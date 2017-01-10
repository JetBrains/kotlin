class WithModifiersOnAccessors {
    @Synchronized private fun methSync() {}
    @Strictfp protected fun methStrict() {}

    @get:Synchronized @set:Synchronized var sync = 0

    @get:Strictfp var strict = 0.0
}