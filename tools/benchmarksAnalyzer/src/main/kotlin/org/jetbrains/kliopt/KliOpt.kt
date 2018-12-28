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


package org.jetbrains.kliopt

// Possible types of arguments.
enum class ArgType(val hasParameter: Boolean) {
    BOOLEAN(false),
    STRING(true),
    INT(true),
    DOUBLE(true)
}

// Common descriptor both for options and positional arguments.
abstract class Descriptor(val type: ArgType,
                          val longName: String,
                          val description: String? = null,
                          val defaultValue: String? = null,
                          val isRequired: Boolean = false) {
    abstract val textDescription: String
    abstract val helpMessage: String
}

class OptionDescriptor(
        type: ArgType,
        longName: String,
        val shortName: String ? = null,
        description: String? = null,
        defaultValue: String? = null,
        isRequired: Boolean = false) : Descriptor (type, longName, description, defaultValue, isRequired) {
    override val textDescription: String
        get() = "option -$longName"

    override val helpMessage: String
        get() {
            val result = StringBuilder()
            result.append("    -${longName}")
            shortName?.let { result.append(", -$it") }
            defaultValue?.let { result.append(" [$it]") }
            description?.let {result.append(" -> ${it}")}
            if (!isRequired) result.append(" (optional)")
            result.append("\n")
            return result.toString()
        }
}

class ArgDescriptor(
        type: ArgType,
        longName: String,
        description: String? = null,
        defaultValue: String? = null,
        isRequired: Boolean = true) : Descriptor (type, longName, description, defaultValue, isRequired) {
    override val textDescription: String
        get() = "argument $longName"

    override val helpMessage: String
        get() {
            val result = StringBuilder()
            result.append("    ${longName}")
            defaultValue?.let { result.append(" [$it]") }
            description?.let {result.append(" -> ${it}")}
            if (!isRequired) result.append(" (optional)")
            result.append("\n")
            return result.toString()
        }
}

// Arguments parser.
class ArgParser(optionsList: List<OptionDescriptor>, argsList: List<ArgDescriptor> = listOf<ArgDescriptor>()) {
    private val options = optionsList.union(listOf(OptionDescriptor(ArgType.BOOLEAN, "help",
                                            "h", "Usage info")))
                            .toList()
    private val arguments = argsList
    private lateinit var parsedValues: MutableMap<String, ParsedArg?>

    inner class ParsedArg(val descriptor: Descriptor, val value: String) {
        val intValue: Int
            get() {
                if (descriptor.type != ArgType.INT)
                    error("Incorrect value for ${descriptor.textDescription}, must be an int")
                return value.toInt()
            }

        val stringValue: String
            get() {
                if (descriptor.type != ArgType.STRING)
                    printError("Incorrect value for ${descriptor.textDescription}, must be a string")
                return value
            }

        val booleanValue: Boolean
            get() {
                if (descriptor.type != ArgType.BOOLEAN)
                    printError("Incorrect value for ${descriptor.textDescription}, must be a boolean")
                return value == "true"
            }

        val doubleValue: Double
            get() {
                if (descriptor.type != ArgType.DOUBLE)
                    printError("Incorrect option value for ${descriptor.textDescription}, must be a double")
                return value.toDouble()
            }
    }

    // Output error. Also adds help usage information for easy understanding of problem.
    private fun printError(message: String) {
        error("$message\n${makeUsage()}")
    }

    private fun saveAsArg(argDescriptors: Map<String, ArgDescriptor>, arg: String): Boolean {
        // Find uninitialized arguments.
        val nullArgs = argDescriptors.keys.filter { parsedValues[it] == null }
        val name = nullArgs.firstOrNull()
        name?. let {
            parsedValues[name] = ParsedArg(argDescriptors[name]!!, arg)
            return true
        }
        return false
    }

    // Parse arguments.
    // Returns true if all arguments were parsed, otherwise return false and print help message.
    fun parse(args: Array<String>): Boolean {
        var index = 0
        val optDescriptors = options.map { it.longName to it }.toMap()
        val shortNames = options.filter { it.shortName != null }.map { it.shortName!! to it.longName }.toMap()
        val argDescriptors = arguments.map { it.longName to it }.toMap()
        parsedValues = optDescriptors.keys.union(argDescriptors.keys).toList().map { it to null }.toMap().toMutableMap()
        while (index < args.size) {
            val arg = args[index]
            if (arg.startsWith('-')) {
                // Option is found.
                val name = shortNames[arg.substring(1)] ?: arg.substring(1)
                val descriptor = optDescriptors[name]
                descriptor?. let {
                    if (descriptor.type.hasParameter) {
                        if (index < args.size - 1) {
                            parsedValues[name] = ParsedArg(descriptor, args[index + 1])
                            index++
                        } else {
                            // An error, option with value without value.
                            printError("No value for ${descriptor.textDescription}")
                        }
                    } else {
                        if (name == "help") {
                            println(makeUsage())
                            return false
                        }
                        parsedValues[name] = ParsedArg(descriptor, "true")
                    }
                } ?: run {
                    // Try save as argument.
                    if (!saveAsArg(argDescriptors, arg)) {
                        printError("Unknown option $arg")
                    }
                }
            } else {
                // Argument is found.
                if (!saveAsArg(argDescriptors, arg)) {
                    printError("Too many arguments!")
                }
            }
            index++
        }

        parsedValues.forEach { (key, value) ->
            val descriptor = optDescriptors[key] ?: argDescriptors[key]!!
            // Not inited, append default value if needed.
            parsedValues[key] = value ?: run {
                descriptor.defaultValue?. let {
                    ParsedArg(descriptor, descriptor.defaultValue)
                }
            }
            // Check if arg is always required.
            parsedValues[key] ?: run {
                if (descriptor.isRequired) {
                    printError("Please, provide value for ${descriptor.textDescription}. It should be always set")
                }
            }
        }

        return true
    }

    fun get(name: String): ParsedArg? {
        if (::parsedValues.isInitialized) {
            return parsedValues[name]
        } else {
            println("Method parse() of ArgParser class should be called before getting arguments and options.")
            return null
        }
    }

    private fun makeUsage(): String {
        val result = StringBuilder()
        result.append("Usage: \n")
        if (!arguments.isEmpty()) {
            result.append("Arguments: \n")
            arguments.forEach {
                result.append(it.helpMessage)
            }
        }
        result.append("Options: \n")
        options.forEach {
            result.append(it.helpMessage)
        }
        return result.toString()
    }
}