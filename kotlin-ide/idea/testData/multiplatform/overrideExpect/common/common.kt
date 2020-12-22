expect class <!LINE_MARKER("descr='Has actuals in JVM'")!>Expect<!>

interface <!LINE_MARKER("descr='Is implemented by Derived'")!>Base<!> {
    fun <!LINE_MARKER("descr='Is implemented in Derived'")!>expectInReturnType<!>(): Expect

    fun expectInArgument(e: Expect)

    fun Expect.expectInReceiver()

    val <!LINE_MARKER("descr='Is implemented in Derived'")!>expectVal<!>: Expect

    var <!LINE_MARKER("descr='Is implemented in Derived'")!>expectVar<!>: Expect
}