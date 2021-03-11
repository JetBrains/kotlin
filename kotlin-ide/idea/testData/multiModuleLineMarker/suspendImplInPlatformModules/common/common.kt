interface <!LINE_MARKER("descr='Is implemented by KJs KJvm'")!>I<!> {
    suspend fun <!LINE_MARKER("descr='Is implemented in KJs KJvm'")!>foo<!>(s: String)
}

/*
LINEMARKER: <html><body>Is implemented in <br>&nbsp;&nbsp;&nbsp;&nbsp;KJs<br>&nbsp;&nbsp;&nbsp;&nbsp;KJvm</body></html>
TARGETS:
js.kt
    suspend override fun <1>foo(s: String) {
 jvm.kt
    suspend override fun <2>foo(s: String) {
*/
