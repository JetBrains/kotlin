import kotlin.text.lines
import org.jetbrains.benchmarksLauncher.*

class HelloWorld {
    private val command = buildList {
        add(env("NATIVE_COMPILER")!!)
        add(env("SOURCE_FILE")!!)
        add("-o")
        add(env("OUTPUT_BINARY")!!)
        addAll(env("COMPILER_FLAGS")!!.lines())
    }.joinToString(separator = " ")

    fun helloWorld() {
        val exitCode = launchProcess(command)
        check(exitCode == 0) {
            "`$command` failed with exit code $exitCode"
        }
    }
}

fun main(args: Array<String>) {
    val launcher = object : Launcher() {
        override val baseBenchmarksSet: MutableMap<String, AbstractBenchmarkEntry> = mutableMapOf(
                "HelloWorld" to BenchmarkEntryWithInit.create(::HelloWorld) { helloWorld() }
        )
    }

    BenchmarksRunner.runBenchmarks(args, { arguments: BenchmarkArguments ->
        if (arguments is BaseBenchmarkArguments) {
            launcher.launch(arguments.warmup, arguments.repeat, arguments.prefix,
                    arguments.filter, arguments.filterRegex, arguments.verbose)
        } else emptyList()
    }, benchmarksListAction = launcher::benchmarksListAction)
}
