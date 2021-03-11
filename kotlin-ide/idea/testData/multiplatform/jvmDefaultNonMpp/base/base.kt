package base

interface <!LINE_MARKER("descr='Is implemented by CheckClass SubCheck SubCheckClass'")!>Check<!> {
    fun <!LINE_MARKER("descr='Is overridden in SubCheck'")!>test<!>(): String {
        return "fail";
    }
}

open class <!LINE_MARKER("descr='Is subclassed by SubCheckClass'")!>CheckClass<!> : Check