expect class <!LINE_MARKER("descr='Has actuals in JVM'")!>Expect<!>

class Box<out T>(val x: T)

interface <!LINE_MARKER("descr='Is implemented by Derived'")!>Base<!> {
    fun <!LINE_MARKER("descr='Is implemented in Derived'")!>expectInReturnType<!>(): Box<Expect>

    fun expectInArgument(e: Box<Expect>)

    fun Box<Expect>.expectInReceiver()

    val <!LINE_MARKER("descr='Is implemented in Derived'")!>expectVal<!>: Box<Expect>

    var <!LINE_MARKER("descr='Is implemented in Derived'")!>expectVar<!>: Box<Expect>
}