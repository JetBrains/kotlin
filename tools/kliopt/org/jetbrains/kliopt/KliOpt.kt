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
sealed class ArgType(val hasParameter: kotlin.Boolean) {
    abstract val description: kotlin.String
    open fun check(value: kotlin.String, name: kotlin.String) {}
    class Boolean : ArgType(false) {
        override val description: kotlin.String
            get() = ""
    }
    class String : ArgType(true) {
        override val description: kotlin.String
            get() = "{ String }"
    }
    class Int : ArgType(true) {
        override val description: kotlin.String
            get() = "{ Int }"

        override fun check(value: kotlin.String, name: kotlin.String) {
            value.toIntOrNull() ?: error("Option $name is expected to be integer number. $value is provided.")
        }
    }
    class Double : ArgType(true) {
        override val description: kotlin.String
            get() = "{ Double }"

        override fun check(value: kotlin.String, name: kotlin.String) {
            value.toDoubleOrNull() ?: error("Option $name is expected to be double number. $value is provided.")
        }
    }
    class Choice(val values: List<kotlin.String>) : ArgType(true) {
        override val description: kotlin.String
            get() = "{ Value should be one of $values }"

        override fun check(value: kotlin.String, name: kotlin.String) {
            if (value !in values) error("Option $name is expected to be one of $values. $value is provided.")
        }
    }
}

data class Action(val callback: (parser: ArgParser) -> Unit, val parser: ArgParser)

// Common descriptor both for options and positional arguments.
abstract class Descriptor(val type: ArgType,
                          val longName: String,
                          val description: String? = null,
                          val defaultValue: String? = null,
                          val isRequired: Boolean = false,
                          val deprecatedWarning: String? = null) {
    abstract val textDescription: String
    abstract val helpMessage: String
}

class OptionDescriptor(
        type: ArgType,
        longName: String,
        val shortName: String ? = null,
        description: String? = null,
        defaultValue: String? = null,
        isRequired: Boolean = false,
        val isMultiple: Boolean = false,
        val delimiter: String? = null,
        deprecatedWarning: String? = null) : Descriptor (type, longName, description, defaultValue, isRequired, deprecatedWarning) {
    override val textDescription: String
        get() = "option -$longName"

    override val helpMessage: String
        get() {
            val result = StringBuilder()
            result.append("    -${longName}")
            shortName?.let { result.append(", -$it") }
            defaultValue?.let { result.append(" [$it]") }
            description?.let {result.append(" -> ${it}")}
            if (isRequired) result.append(" (always required)")
            result.append(" ${type.description}")
            deprecatedWarning?.let { result.append(" Warning: $it") }
            result.append("\n")
            return result.toString()
        }
}

class ArgDescriptor(
        type: ArgType,
        longName: String,
        description: String? = null,
        defaultValue: String? = null,
        isRequired: Boolean = true,
        deprecatedWarning: String? = null) : Descriptor (type, longName, description, defaultValue, isRequired, deprecatedWarning) {
    override val textDescription: String
        get() = "argument $longName"

    override val helpMessage: String
        get() {
            val result = StringBuilder()
            result.append("    ${longName}")
            defaultValue?.let { result.append(" [$it]") }
            description?.let {result.append(" -> ${it}")}
            if (!isRequired) result.append(" (optional)")
            result.append(" ${type.description}")
            deprecatedWarning?.let { result.append(" Warning: $it") }
            result.append("\n")
            return result.toString()
        }
}

// Arguments parser.
class ArgParser(optionsList: List<OptionDescriptor>, argsList: List<ArgDescriptor> = listOf<ArgDescriptor>(),
                val actions: Map<String, Action> = emptyMap(), useDefaultHelpShortName: Boolean = true) {
    private val options = optionsList.union(if (useDefaultHelpShortName)
        listOf(OptionDescriptor(ArgType.Boolean(), "help", "h", "Usage info"))
            else
        listOf(OptionDescriptor(ArgType.Boolean(), "help", description = "Usage info"))
    ).toList()
    private val arguments = argsList
    private lateinit var parsedValues: MutableMap<String, ParsedArg?>
    private lateinit var valuesOrigin: MutableMap<String, ValueOrigin?>

    enum class ValueOrigin { SET_BY_USER, SET_DEFAULT_VALUE, UNSET }

    inner class ParsedArg(val descriptor: Descriptor, val values: List<String>) {
        init {
            // Check correctness of initialization data.
            if (values.isEmpty()) {
                printError("Parsed value should be provided!")
            }
        }

        // Get value of argument converted to expected type.
        private fun <T : Any> getTyped(value: String): T {
            val typedValue = when (descriptor.type) {
                is ArgType.Int -> value.toInt()
                is ArgType.Double -> value.toDouble()
                is ArgType.Boolean -> value == "true"
                else -> value
            } as? T
            typedValue ?: printError("Argument ${descriptor.longName} has type ${descriptor.type} which differs from expected!")
            return typedValue
        }

        // Get value of argument.
        fun <T : Any> get(): T {
            return getTyped<T>(values[0])
        }

        // Get all values of argument.
        // For options that can be set multiple types.
        fun <T : Any> getAll(): List<T> {
            return values.map { getTyped<T>(it) }
        }
    }

    // Output error. Also adds help usage information for easy understanding of problem.
    fun printError(message: String): Nothing {
        error("$message\n${makeUsage()}")
    }

    // Get origin of option value.
    fun getOrigin(name: String) = valuesOrigin[name] ?: printError("No option/argument $name in list of avaliable options")

    private fun saveAsArg(argDescriptors: Map<String, ArgDescriptor>, arg: String, processedValues: Map<String, MutableList<String>>): Boolean {
        // Find uninitialized arguments.
        val nullArgs = argDescriptors.keys.filter { processedValues.getValue(it).isEmpty() }
        val name = nullArgs.firstOrNull()
        name?. let {
            argDescriptors.getValue(name).type.check(arg, name)
            argDescriptors.getValue(name).deprecatedWarning?.let { println ("Warning: $it") }
            processedValues.getValue(name).add(arg)
            return true
        }
        return false
    }

    private fun saveAsOption(descriptor: OptionDescriptor, value: String, processedValues: Map<String, MutableList<String>>) {
        if (!descriptor.isMultiple && !processedValues.getValue(descriptor.longName).isEmpty()) {
            printError("Option ${descriptor.longName} is used more than one time!")
        }
        descriptor.deprecatedWarning?.let { if (processedValues.getValue(descriptor.longName).isEmpty()) println ("Warning: $it") }
        val savedValues = descriptor.delimiter?.let { value.split(it) } ?: listOf(value)

        savedValues.forEach {
            descriptor.type.check(it, descriptor.longName)
            processedValues.getValue(descriptor.longName).add(it)
        }

    }

    // Parse arguments.
    // Returns true if all arguments were parsed, otherwise return false and print help message.
    fun parse(args: Array<String>): Boolean {
        var index = 0
        val optDescriptors = options.map { it.longName to it }.toMap()
        val shortNames = options.filter { it.shortName != null }.map { it.shortName!! to it.longName }.toMap()
        val argDescriptors = arguments.map { it.longName to it }.toMap()
        val descriptorsKeys = optDescriptors.keys.union(argDescriptors.keys).toList()
        val processedValues = descriptorsKeys.map { it to mutableListOf<String>() }.toMap().toMutableMap()
        parsedValues = descriptorsKeys.map { it to null }.toMap().toMutableMap()
        valuesOrigin = descriptorsKeys.map { it to ValueOrigin.UNSET }.toMap().toMutableMap()
        while (index < args.size) {
            val arg = args[index]
            // Check for actions.
            actions.forEach { (name, action) ->
                if (arg == name) {
                    // Use parser for this action.
                    val parseResult = action.parser.parse(args.slice(index + 1..args.size - 1).toTypedArray())
                    if (parseResult)
                        action.callback(action.parser)
                    return false
                }
            }
            if (arg.startsWith('-')) {
                // Option is found.
                val name = shortNames[arg.substring(1)] ?: arg.substring(1)
                val descriptor = optDescriptors[name]
                descriptor?. let {
                    if (descriptor.type.hasParameter) {
                        if (index < args.size - 1) {
                            saveAsOption(descriptor, args[index + 1], processedValues)
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
                        saveAsOption (descriptor, "true", processedValues)
                    }
                } ?: run {
                    // Try save as argument.
                    if (!saveAsArg(argDescriptors, arg, processedValues)) {
                        printError("Unknown option $arg")
                    }
                }
            } else {
                // Argument is found.
                if (!saveAsArg(argDescriptors, arg, processedValues)) {
                    printError("Too many arguments! Couldn't proccess argument $arg!")
                }
            }
            index++
        }

        processedValues.forEach { (key, value) ->
            val descriptor = optDescriptors[key] ?: argDescriptors.getValue(key)
            // Not inited, append default value if needed.
            if (value.isEmpty()) {
                descriptor.defaultValue?. let {
                    parsedValues[key] = ParsedArg(descriptor, listOf(descriptor.defaultValue))
                    valuesOrigin[key] = ValueOrigin.SET_DEFAULT_VALUE
                } ?: run {
                    if (descriptor.isRequired) {
                        printError("Please, provide value for ${descriptor.textDescription}. It should be always set")
                    } else {
                        parsedValues[key] = null
                    }
                }
            } else {
                parsedValues[key] = ParsedArg(descriptor, value)
                valuesOrigin[key] = ValueOrigin.SET_BY_USER
            }
        }
        return true
    }

    // Get value of argument.
    fun <T : Any> get(name: String): T? {
        if (::parsedValues.isInitialized) {
            val arg = parsedValues[name]
            return arg?.get()
        } else {
            printError("Method parse() of ArgParser class should be called before getting arguments and options.")
        }
    }

    // Get all values of argument.
    // For options that can be set multiple types.
    fun <T : Any> getAll(name: String): List<T>? {
        if (::parsedValues.isInitialized) {
            val arg = parsedValues[name]
            return arg?.getAll()
        } else {
            printError("Method parse() of ArgParser class should be called before getting arguments and options.")
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