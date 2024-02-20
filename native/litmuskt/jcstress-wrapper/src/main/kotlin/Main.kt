import komem.litmus.generateWrapperFile
import komem.litmus.generated.LitmusTestRegistry
import kotlin.io.path.Path

fun main() {
    var successCnt = 0
    val allTests = LitmusTestRegistry.all()
    for (test in allTests) {
        val success = generateWrapperFile(test, jcstressDirectory)
        if (success) successCnt++
    }
    if (successCnt != allTests.size) {
        System.err.println("WARNING: generated wrappers for $successCnt out of ${allTests.size} known tests")
    }
}

val jcstressDirectory
    get() = Path(System.getenv("JCS_DIR") ?: error("JCS_DIR envvar is not set"))
