@file:JvmName("ConvertJMHReportCLI")

import kotlinx.cli.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.report.BenchmarkResult
import java.io.File

// We only care about a subset of the jmh json report.
private val jmh = Json { ignoreUnknownKeys = true }

@Serializable
private class Metric(val scoreUnit: String, val rawData: List<List<Double>>)

@Serializable
private class Benchmark(
        val benchmark: String,
        val warmupIterations: Int,
        val primaryMetric: Metric,
)

private fun Benchmark.toBenchmarkResult(
        namePrefix: String,
        metric: BenchmarkResult.Metric,
        hidePostfix: String?,
): List<BenchmarkResult> {
    check(primaryMetric.scoreUnit == "us/op") {
        "Unexpected scoreUnit `${primaryMetric.scoreUnit}`; needs `us/op`"
    }
    val name = buildString {
        append(namePrefix)

        fun String.shouldHide() = hidePostfix?.let { endsWith(it) } ?: false
        val separator = "."
        benchmark.split(separator).filterNot { it.shouldHide() }.joinTo(this, separator = separator)
    }
    return primaryMetric.rawData.flatten().mapIndexed { index, score ->
        BenchmarkResult(
                name,
                BenchmarkResult.Status.PASSED,
                score,
                metric,
                runtimeInUs = score,
                repeat = index + 1,
                warmupIterations
        )
    }
}

fun main(args: Array<String>) {
    val argParser = ArgParser("convertJMHReport", prefixStyle = ArgParser.OptionPrefixStyle.GNU)

    val jmhReport by argParser.argument(ArgType.String, description = "jmh report to convert")
    val output by argParser.option(ArgType.String, shortName = "o", description = "Where to write the converted report").required()
    val prefix by argParser.option(ArgType.String, shortName = "p", description = "Prepend to each benchmark name").default("")
    val metric by argParser.option(ArgType.Choice<BenchmarkResult.Metric>(), shortName = "m", description = "Type of the metric").default(BenchmarkResult.Metric.EXECUTION_TIME)
    val hidePostfix by argParser.option(ArgType.String, description = "In benchmark names hide components that end with the given postfix")

    argParser.parse(args)

    jmh.decodeFromString<List<Benchmark>>(File(jmhReport).readText()).flatMap {
        it.toBenchmarkResult(prefix, metric, hidePostfix)
    }.joinToString(prefix = "[", postfix = "]") { it.toJson() }.let {
        File(output).writeText(it)
    }
}