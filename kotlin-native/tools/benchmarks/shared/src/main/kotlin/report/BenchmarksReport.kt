/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.report

import org.jetbrains.report.json.*

interface JsonSerializable {
    fun serializeFields(): String

    fun toJson(): String {
        return """
        {
            ${serializeFields()}
        }
        """
    }

    // Convert iterable objects arrays, lists to json.
    fun <T> arrayToJson(data: Iterable<T>): String {
        return data.joinToString(prefix = "[", postfix = "]") {
            if (it is JsonSerializable) it.toJson() else it.toString()
        }
    }
}

interface EntityFromJsonFactory<T> : ConvertedFromJson {
    fun create(data: JsonElement): T
}

// Parse array with benchmarks to list
fun parseBenchmarksArray(data: JsonElement): List<BenchmarkResult> {
    if (data is JsonArray) {
        return data.jsonArray.map {
            if (MeanVarianceBenchmark.isMeanVarianceBenchmark(it))
                MeanVarianceBenchmark.create(it as JsonObject)
            else BenchmarkResult.create(it as JsonObject)
        }
    } else {
        error("Benchmarks field is expected to be an array. Please, check origin files.")
    }
}

// Class for benchmarks report with all information of run.
open class BenchmarksReport(val env: Environment, benchmarksList: List<BenchmarkResult>, val compiler: Compiler) :
        JsonSerializable {

    companion object : EntityFromJsonFactory<BenchmarksReport> {
        override fun create(data: JsonElement): BenchmarksReport {
            if (data is JsonObject) {
                val env = Environment.create(data.getRequiredField("env"))
                val benchmarksObj = data.getRequiredField("benchmarks")
                val compiler = Compiler.create(data.getRequiredField("kotlin"))
                val buildNumberField = data.getOptionalField("buildNumber")
                val benchmarksList = parseBenchmarksArray(benchmarksObj)
                val report = BenchmarksReport(env, benchmarksList, compiler)
                buildNumberField?.let { report.buildNumber = (it as JsonLiteral).unquoted() }
                return report
            } else {
                error("Top level entity is expected to be an object. Please, check origin files.")
            }
        }

        // Made a map of becnhmarks with name as key from list.
        private fun structBenchmarks(benchmarksList: List<BenchmarkResult>) =
                benchmarksList.groupBy { it.name }
    }

    val benchmarks = structBenchmarks(benchmarksList)

    var buildNumber: String? = null

    override fun serializeFields(): String {
        val buildNumberField = buildNumber?.let {
            """,
            "buildNumber": "$buildNumber"
            """
        } ?: ""
        return """
            "env": ${env.toJson()},
            "kotlin": ${compiler.toJson()},
            "benchmarks": ${arrayToJson(benchmarks.flatMap { it.value })}$buildNumberField
        """.trimIndent()
    }

    fun merge(other: BenchmarksReport): BenchmarksReport {
        val mergedBenchmarks = HashMap(benchmarks)
        other.benchmarks.forEach {
            if (it.key in mergedBenchmarks) {
                error("${it.key} already exists in report!")
            }
        }
        mergedBenchmarks.putAll(other.benchmarks)
        return BenchmarksReport(env, mergedBenchmarks.flatMap { it.value }, compiler)
    }

    // Concatenate benchmarks report if they have same environment and compiler.
    operator fun plus(other: BenchmarksReport): BenchmarksReport {
        if (compiler != other.compiler || env != other.env) {
            error("It's impossible to concat reports from different machines!")
        }
        return merge(other)
    }
}

// Class for kotlin compiler
data class Compiler(val backend: Backend, val kotlinVersion: String) : JsonSerializable {

    enum class BackendType(val type: String) {
        JVM("jvm"),
        NATIVE("native")
    }

    companion object : EntityFromJsonFactory<Compiler> {
        override fun create(data: JsonElement): Compiler {
            if (data is JsonObject) {
                val backend = Backend.create(data.getRequiredField("backend"))
                val kotlinVersion = elementToString(data.getRequiredField("kotlinVersion"), "kotlinVersion")

                return Compiler(backend, kotlinVersion)
            } else {
                error("Kotlin entity is expected to be an object. Please, check origin files.")
            }
        }

        fun backendTypeFromString(s: String): BackendType? = BackendType.values().find { it.type == s }
    }

    // Class for compiler backend
    data class Backend(val type: BackendType, val version: String, val flags: List<String>) : JsonSerializable {
        companion object : EntityFromJsonFactory<Backend> {
            override fun create(data: JsonElement): Backend {
                if (data is JsonObject) {
                    val typeElement = data.getRequiredField("type")
                    if (typeElement is JsonLiteral) {
                        val type = backendTypeFromString(typeElement.unquoted())
                                ?: error("Backend type should be 'jvm' or 'native'")
                        val version = elementToString(data.getRequiredField("version"), "version")
                        val flagsArray = data.getOptionalField("flags")
                        var flags: List<String> = emptyList()
                        if (flagsArray != null && flagsArray is JsonArray) {
                            flags = flagsArray.jsonArray.map { it.toString() }
                        }
                        return Backend(type, version, flags)
                    } else {
                        error("Backend type should be string literal.")
                    }
                } else {
                    error("Backend entity is expected to be an object. Please, check origin files.")
                }
            }
        }

        override fun serializeFields(): String {
            val result = """
                "type": "${type.type}",
                "version": "${version}""""
            // Don't print flags field if there is no one.
            if (flags.isEmpty()) {
                return """$result
                """
            } else {
                return """
                    $result,
                "flags": ${arrayToJson(flags.map { if (it.startsWith("\"")) it else "\"$it\"" })}
                """
            }
        }
    }

    override fun serializeFields(): String {
        return """
            "backend": ${backend.toJson()},
            "kotlinVersion": "${kotlinVersion}"
        """
    }
}

// Class for description of environment of benchmarks run
data class Environment(val machine: Machine, val jdk: JDKInstance) : JsonSerializable {

    companion object : EntityFromJsonFactory<Environment> {
        override fun create(data: JsonElement): Environment {
            if (data is JsonObject) {
                val machine = Machine.create(data.getRequiredField("machine"))
                val jdk = JDKInstance.create(data.getRequiredField("jdk"))

                return Environment(machine, jdk)
            } else {
                error("Environment entity is expected to be an object. Please, check origin files.")
            }
        }
    }

    // Class for description of machine used for benchmarks run.
    data class Machine(val cpu: String, val os: String) : JsonSerializable {
        companion object : EntityFromJsonFactory<Machine> {
            override fun create(data: JsonElement): Machine {
                if (data is JsonObject) {
                    val cpu = elementToString(data.getRequiredField("cpu"), "cpu")
                    val os = elementToString(data.getRequiredField("os"), "os")

                    return Machine(cpu, os)
                } else {
                    error("Machine entity is expected to be an object. Please, check origin files.")
                }
            }
        }

        override fun serializeFields(): String {
            return """
                "cpu": "$cpu",
                "os": "$os"
            """
        }
    }

    // Class for description of jdk used for benchmarks run.
    data class JDKInstance(val version: String, val vendor: String) : JsonSerializable {
        companion object : EntityFromJsonFactory<JDKInstance> {
            override fun create(data: JsonElement): JDKInstance {
                if (data is JsonObject) {
                    val version = elementToString(data.getRequiredField("version"), "version")
                    val vendor = elementToString(data.getRequiredField("vendor"), "vendor")

                    return JDKInstance(version, vendor)
                } else {
                    error("JDK entity is expected to be an object. Please, check origin files.")
                }
            }
        }

        override fun serializeFields(): String {
            return """
                "version": "$version",
                "vendor": "$vendor"
            """
        }
    }

    override fun serializeFields(): String {
        return """
                "machine": ${machine.toJson()},
                "jdk": ${jdk.toJson()}
            """
    }
}

open class BenchmarkResult(val name: String, val status: Status,
                           val score: Double, val metric: Metric, val runtimeInUs: Double,
                           val repeat: Int, val warmup: Int) : JsonSerializable {

    enum class Metric(val suffix: String, val value: String) {
        EXECUTION_TIME("", "EXECUTION_TIME"),
        CODE_SIZE(".codeSize", "CODE_SIZE"),
        COMPILE_TIME(".compileTime", "COMPILE_TIME"),
        BUNDLE_SIZE(".bundleSize", "BUNDLE_SIZE")
    }

    constructor(name: String, score: Double) : this(name, Status.PASSED, score, Metric.EXECUTION_TIME, 0.0, 0, 0)

    companion object : EntityFromJsonFactory<BenchmarkResult> {

        override fun create(data: JsonElement): BenchmarkResult {
            if (data is JsonObject) {
                var name = elementToString(data.getRequiredField("name"), "name")
                val metricElement = data.getOptionalField("metric")
                val metric = if (metricElement != null && metricElement is JsonLiteral)
                    metricFromString(metricElement.unquoted()) ?: Metric.EXECUTION_TIME
                else Metric.EXECUTION_TIME
                name += metric.suffix
                val statusElement = data.getRequiredField("status")
                if (statusElement is JsonLiteral) {
                    val status = statusFromString(statusElement.unquoted())
                            ?: error("Status should be PASSED or FAILED")

                    val score = elementToDouble(data.getRequiredField("score"), "score")
                    val runtimeInUs = elementToDouble(data.getRequiredField("runtimeInUs"), "runtimeInUs")
                    val repeat = elementToInt(data.getRequiredField("repeat"), "repeat")
                    val warmup = elementToInt(data.getRequiredField("warmup"), "warmup")

                    return BenchmarkResult(name, status, score, metric, runtimeInUs, repeat, warmup)
                } else {
                    error("Status should be string literal.")
                }
            } else {
                error("Benchmark entity is expected to be an object. Please, check origin files.")
            }
        }

        fun statusFromString(s: String): Status? = Status.values().find { it.value == s }
        fun metricFromString(s: String): Metric? = Metric.values().find { it.value == s }
    }

    enum class Status(val value: String) {
        PASSED("PASSED"),
        FAILED("FAILED")
    }

    override fun serializeFields(): String {
        return """
            "name": "${name.removeSuffix(metric.suffix)}",
            "status": "${status.value}",
            "score": ${score},
            "metric": "${metric.value}",
            "runtimeInUs": ${runtimeInUs},
            "repeat": ${repeat},
            "warmup": ${warmup}
        """
    }

    val shortName: String
        get() = name.removeSuffix(metric.suffix)
}

// Entity to describe avarage values which conssists of mean and variance values.
data class MeanVariance(val mean: Double, val variance: Double)

// Processed benchmark result with calculated mean and variance value.
open class MeanVarianceBenchmark(name: String, status: BenchmarkResult.Status, score: Double, metric: BenchmarkResult.Metric,
                                 runtimeInUs: Double, repeat: Int, warmup: Int, val variance: Double) :
        BenchmarkResult(name, status, score, metric, runtimeInUs, repeat, warmup) {

    constructor(name: String, score: Double, variance: Double) : this(name, BenchmarkResult.Status.PASSED, score,
            BenchmarkResult.Metric.EXECUTION_TIME, 0.0, 0, 0, variance)

    companion object : EntityFromJsonFactory<MeanVarianceBenchmark> {

        fun isMeanVarianceBenchmark(data: JsonElement) = data is JsonObject && data.getOptionalField("variance") != null

        override fun create(data: JsonElement): MeanVarianceBenchmark {
            if (data is JsonObject) {
                val baseBenchmark = BenchmarkResult.create(data)
                val variance = elementToDouble(data.getRequiredField("variance"), "variance")
                return MeanVarianceBenchmark(baseBenchmark.name, baseBenchmark.status, baseBenchmark.score, baseBenchmark.metric,
                        baseBenchmark.runtimeInUs, baseBenchmark.repeat, baseBenchmark.warmup, variance)
            } else {
                error("Benchmark entity is expected to be an object. Please, check origin files.")
            }
        }
    }

    override fun serializeFields(): String {
        return """
            ${super.serializeFields()},
            "variance": $variance
            """
    }
}