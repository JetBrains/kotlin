import kotlin.text.lines
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Param
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State

@State(Scope.Benchmark)
class HelloWorldHideName {
    @Param("")
    var nativeCompiler: String = ""

    @Param("")
    var sourceFile: String = ""

    @Param("")
    var outputBinary: String = ""

    @Param("")
    var compilerFlags: String = ""

    private var command: String? = null

    val flags: List<String>
        get() = compilerFlags.split('!').map {
            it.replace('#', '=')
        }

    @Setup
    fun prepare() {
        command = buildList {
            add(nativeCompiler)
            add(sourceFile)
            add("-o")
            add(outputBinary)
            addAll(flags)
        }.joinToString(separator = " ")
    }

    @Benchmark
    fun HelloWorld() {
        val exitCode = launchProcess(command!!)
        check(exitCode == 0) {
            "`$command` failed with exit code $exitCode"
        }
    }
}
