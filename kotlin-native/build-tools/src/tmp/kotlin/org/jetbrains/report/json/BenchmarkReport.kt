/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.report.json

open class JsonSerializable
data class JsonObject(val content: Map<String, JsonElement>) : JsonElement(), Map<String, JsonElement> by content {
    fun getOrNull(key: String): JsonElement? = null
    fun getObject(key: String): JsonObject = JsonObject(emptyMap<String, JsonElement>())
    fun getArray(key: String): JsonArray = JsonArray(emptyList())
    fun getPrimitive(key: String): JsonPrimitive = JsonNull
}
data class JsonLiteral internal constructor(
    private val body: Any,
    private val isString: Boolean
) : JsonPrimitive() {
    override val content = body.toString()
    override val contentOrNull: String = content
    constructor(number: Number) : this(number, false)
    constructor(boolean: Boolean) : this(boolean, false)
    constructor(string: String) : this(string, true)
    fun unquoted() = ""
}

sealed class JsonPrimitive():JsonElement(){
    abstract val content: String
    abstract val contentOrNull: String?
    val int: Int get() = 0
}

object JsonNull : JsonPrimitive() {
    override val content: String = "null"
    override val contentOrNull: String? = null
}
open class JsonElement:JsonSerializable() {
    open val jsonObject: JsonObject
        get() = error("JsonObject")
    open val jsonArray: JsonArray
        get() = error("JsonArray")
}
open class JsonTreeParser:JsonSerializable() {
    companion object{
        fun parse(benchDesc:String) = JsonElement()
    }
}

open class JsonArray(val content: List<JsonElement>):JsonElement(), List<JsonElement> by content {
    fun getObject(index: Int): JsonObject = JsonObject(emptyMap<String, JsonElement>())
}

open class BenchmarksReport(val env: Environment, benchmarksList: List<BenchmarkResult>, val compiler: Compiler) : JsonSerializable() {
    constructor() : this(Environment(Environment.Machine("Pentium Pro", "Haiku OS"), Environment.JDKInstance("KaffeJVM", "Kaffe")),
                         emptyList<BenchmarkResult>(),
                        Compiler(Compiler.Backend(Compiler.BackendType.NATIVE, "1.0", emptyList()),"1.0"))
    companion object {
        fun create(data: JsonElement): BenchmarksReport = BenchmarksReport()
    }
    fun toJson() = "{}"
    operator fun plus(other: BenchmarksReport): BenchmarksReport = this
}
open class BenchmarkResult(val name: String, val status: Status,
                           val score: Double, val metric: Metric, val runtimeInUs: Double,
                           val repeat: Int, val warmup: Int) : JsonSerializable() {
    enum class Status(val value: String) {
        PASSED("PASSED"),
        FAILED("FAILED")
    }
    enum class Metric(val suffix: String, val value: String) {
        EXECUTION_TIME("", "EXECUTION_TIME"),
        CODE_SIZE(".codeSize", "CODE_SIZE"),
        COMPILE_TIME(".compileTime", "COMPILE_TIME"),
        BUNDLE_SIZE(".bundleSize", "BUNDLE_SIZE")
    }
}

data class Environment(val machine: Machine, val jdk: JDKInstance) : JsonSerializable() {
    data class Machine(val cpu: String, val os: String) : JsonSerializable()
    data class JDKInstance(val version: String, val vendor: String) : JsonSerializable()
}

data class Compiler(val backend: Backend, val kotlinVersion: String) : JsonSerializable() {

    enum class BackendType(val type: String) {
        JVM("jvm"),
        NATIVE("native")
    }
    data class Backend(val type: BackendType, val version: String, val flags: List<String>) : JsonSerializable()
    companion object{
        fun backendTypeFromString(ignored0:String? = null , ignored1:String? = null) = Compiler.BackendType.NATIVE
    }
}

fun parseBenchmarksArray(data: JsonElement): List<BenchmarkResult> = emptyList<BenchmarkResult>()
