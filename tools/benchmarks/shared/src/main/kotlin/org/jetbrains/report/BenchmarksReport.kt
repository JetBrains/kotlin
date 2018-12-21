/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.jetbrains.report

import org.jetbrains.report.json.*

interface JsonSerializable {
    fun toJson(): String

    // Convert iterable objects arrays, lists to json.
    fun <T> arrayToJson(data: Iterable<T>): String {
        return data.joinToString(prefix = "[", postfix = "]") {
            if (it is JsonSerializable) it.toJson() else it.toString()
        }
    }
}

// Entity can be created from json description.
interface ConvertedFromJson {
    fun getRequiredField(data: JsonObject, fieldName: String): JsonElement {
        return data.getOrNull(fieldName) ?: error("Field '$fieldName' doesn't exist in '$data'. Please, check origin files.")
    }

    fun getOptionalField(data: JsonObject, fieldName: String): JsonElement? {
        return data.getOrNull(fieldName)
    }

    // Parse json array to list.
    // Takes function to convert elements in array to expected type.
    fun <T> arrayToList(array: JsonArray, convert: JsonArray.(Int) -> T?): List<T> {
        var results = mutableListOf<T>()
        var index = 0
        var current: T? = array.convert(index)
        while (current != null) {
            results.add(current)
            index++
            current = array.convert(index)
        }
        return results
    }

    // Methods for conversion to expected type with checks of possibility of such conversions.
    fun elementToDouble(element: JsonElement, name: String): Double =
            if (element is JsonPrimitive)
                element.double
            else
                error("Field '$name' in '$element' is expected to be a double number. Please, check origin files.")

    fun elementToInt(element: JsonElement, name: String): Int =
        if (element is JsonPrimitive)
            element.int
        else
            error("Field '$name' in '$element' is expected to be an integer number. Please, check origin files.")

    fun elementToString(element: JsonElement, name:String): String =
            if (element is JsonLiteral)
                element.unquoted()
            else
                error("Field '$name' in '$element' is expected to be a string. Please, check origin files.")
}

interface EntityFromJsonFactory<T>: ConvertedFromJson {
    fun create(data: JsonElement): T
}

// Class for benchcmarks report with all information of run.
data class BenchmarksReport(val env: Environment, val benchmarks: List<BenchmarkResult>, val compiler: Compiler):
        JsonSerializable {

    companion object: EntityFromJsonFactory<BenchmarksReport> {
        override fun create(data: JsonElement): BenchmarksReport {
            if (data is JsonObject) {
                val env = Environment.create(getRequiredField(data, "env"))
                val benchmarksObj = getRequiredField(data, "benchmarks")
                val compiler = Compiler.create(getRequiredField(data, "kotlin"))
                val benchmarks = parseBenchmarksArray(benchmarksObj)
                return BenchmarksReport(env, benchmarks, compiler)
            } else {
                error("Top level entity is expected to be an object. Please, check origin files.")
            }
        }

        // Parse array with benchmarks to list
        fun parseBenchmarksArray(data: JsonElement): List<BenchmarkResult> {
            if (data is JsonArray) {
                return arrayToList(data.jsonArray, { index ->
                    if (this.getObjectOrNull(index) != null)
                        BenchmarkResult.create(this.getObjectOrNull(index) as JsonObject)
                    else null
                })
            } else {
                error("Benchmarks field is expected to be an array. Please, check origin files.")
            }
        }
    }

    override fun toJson(): String {
        return """
        {
            "env": ${env.toJson()},
            "kotlin": ${compiler.toJson()},
            "benchmarks": ${arrayToJson(benchmarks)}
        }
        """
    }
}

// Class for kotlin compiler
data class Compiler(val backend: Backend, val kotlinVersion: String): JsonSerializable {

    enum class BackendType(val type: String) {
        JVM("jvm"),
        NATIVE("native")
    }

    companion object: EntityFromJsonFactory<Compiler> {
        override fun create(data: JsonElement): Compiler {
            if (data is JsonObject) {
                val backend = Backend.create(getRequiredField(data, "backend"))
                val kotlinVersion = elementToString(getRequiredField(data, "kotlinVersion"), "kotlinVersion")

                return Compiler(backend, kotlinVersion)
            } else {
                error("Kotlin entity is expected to be an object. Please, check origin files.")
            }
        }

        fun backendTypeFromString(s: String): BackendType? = BackendType.values().find { it.type == s }
    }

    // Class for compiler backend
    data class Backend(val type: BackendType, val version: String, val flags: List<String>): JsonSerializable {
        companion object: EntityFromJsonFactory<Backend> {
            override fun create(data: JsonElement): Backend {
                if (data is JsonObject) {
                    val typeElement = getRequiredField(data, "type")
                    if (typeElement is JsonLiteral) {
                        val type = backendTypeFromString(typeElement.unquoted()) ?: error("Backend type should be 'jvm' or 'native'")
                        val version = elementToString(getRequiredField(data, "version"), "version")
                        val flagsArray = getOptionalField(data, "flags")
                        var flags: List<String> = emptyList()
                        if (flagsArray != null && flagsArray is JsonArray) {
                            flags = arrayToList(flagsArray.jsonArray, { index ->
                                this.getPrimitiveOrNull(index)?.toString()
                            })
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

        override fun toJson(): String {
            val result = """
            {
                "type": "${type.type}",
                "version": "${version}""""
            // Don't print flags field if there is no one.
            if (flags.isEmpty()) {
                return """$result
                }"""
            }
            else {
                return """
                    $result,
                "flags": ${arrayToJson(flags)}
                }
                """
            }
        }
    }

    override fun toJson(): String {
        return """
        {
            "backend": ${backend.toJson()},
            "kotlinVersion": "${kotlinVersion}"
        }
        """
    }
}

// Class for description of environment of benchmarks run
data class Environment(val machine: Machine, val jdk: JDKInstance): JsonSerializable {

    companion object: EntityFromJsonFactory<Environment> {
        override fun create(data: JsonElement): Environment {
            if (data is JsonObject) {
                val machine = Machine.create(getRequiredField(data, "machine"))
                val jdk = JDKInstance.create(getRequiredField(data, "jdk"))

                return Environment(machine, jdk)
            } else {
                error("Environment entity is expected to be an object. Please, check origin files.")
            }
        }
    }

    // Class for description of machine used for benchmarks run.
    data class Machine(val cpu: String, val os: String): JsonSerializable {
        companion object: EntityFromJsonFactory<Machine> {
            override fun create(data: JsonElement): Machine {
                if (data is JsonObject) {
                    val cpu = elementToString(getRequiredField(data, "cpu"), "cpu")
                    val os = elementToString(getRequiredField(data, "os"), "os")

                    return Machine(cpu, os)
                } else {
                    error("Machine entity is expected to be an object. Please, check origin files.")
                }
            }
        }

        override fun toJson(): String {
            return """
            {
                "cpu": "$cpu",
                "os": "$os"
            }
            """
        }
    }

    // Class for description of jdk used for benchmarks run.
    data class JDKInstance(val version: String, val vendor: String): JsonSerializable {
        companion object: EntityFromJsonFactory<JDKInstance> {
            override fun create(data: JsonElement): JDKInstance {
                if (data is JsonObject) {
                    val version = elementToString(getRequiredField(data, "version"), "version")
                    val vendor = elementToString(getRequiredField(data, "vendor"), "vendor")

                    return JDKInstance(version, vendor)
                } else {
                    error("JDK entity is expected to be an object. Please, check origin files.")
                }
            }
        }

        override fun toJson(): String {
            return """
            {
                "version": "$version",
                "vendor": "$vendor"
            }
            """
        }
    }

    override fun toJson(): String {
        return """
            {
                "machine": ${machine.toJson()},
                "jdk": ${jdk.toJson()}
            }
            """
    }
}

class BenchmarkResult(val name: String, val status: Status,
                      val score: Double, val runtimeInUs: Double,
                      val repeat: Int, val warmup: Int): JsonSerializable {

    companion object: EntityFromJsonFactory<BenchmarkResult> {

        override fun create(data: JsonElement): BenchmarkResult {
            if (data is JsonObject) {
                val name = elementToString(getRequiredField(data, "name"), "name")
                val statusElement = getRequiredField(data, "status")
                if (statusElement is JsonLiteral) {
                    val status = statusFromString(statusElement.unquoted())
                            ?: error("Status should be PASSED or FAILED")
                    val score = elementToDouble(getRequiredField(data, "score"), "score")
                    val runtimeInUs = elementToDouble(getRequiredField(data, "runtimeInUs"), "runtimeInUs")
                    val repeat = elementToInt(getRequiredField(data, "repeat"), "repeat")
                    val warmup = elementToInt(getRequiredField(data, "warmup"), "warmup")

                    return BenchmarkResult(name, status, score, runtimeInUs, repeat, warmup)
                } else {
                    error("Status should be string literal.")
                }
            } else {
                error("Benchmark entity is expected to be an object. Please, check origin files.")
            }
        }

        fun statusFromString(s: String): Status? = Status.values().find { it.value == s }
    }

    enum class Status(val value: String) {
        PASSED("PASSED"),
        FAILED("FAILED")
    }

    override fun toJson(): String {
        return """
        {
            "name": "$name",
            "status": "${status.value}",
            "score": ${score},
            "runtimeInUs": ${runtimeInUs},
            "repeat": ${repeat},
            "warmup": ${warmup}
        }
        """
    }
}