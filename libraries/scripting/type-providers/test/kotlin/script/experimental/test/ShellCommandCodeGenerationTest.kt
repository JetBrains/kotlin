/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.test

import java.io.File
import java.io.Serializable
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure
import kotlin.script.experimental.typeProviders.generatedCode.GeneratedCode
import kotlin.script.experimental.typeProviders.generatedCode.IdentifiableMember
import kotlin.script.experimental.typeProviders.generatedCode.impl.`interface`
import kotlin.script.experimental.typeProviders.generatedCode.impl.extensionProperty
import kotlin.script.experimental.typeProviders.generatedCode.impl.lazyProperty
import kotlin.script.experimental.typeProviders.generatedCode.implement
import kotlin.script.experimental.typeProviders.generatedCode.withParameters

class ShellCommandCodeGenerationTest : GeneratedCodeTestsBase() {

    // Tests a more complex example of code generation with interfaces, extensions, etc
    // This is intended as a larger integration test of the Code Generation DSL
    // Example taken from: https://github.com/nerdsupremacist/kotlin-shell-command-type-provider
    fun testCommandLineExample() {
        val command = ShellCommand(
            name = listOf("docker"),
            subCommands = listOf(
                ShellCommand(
                    name = listOf("docker", "build"),
                    subCommands = emptyList(),
                    options = listOf(
                        ShellCommand.Option.Value("tag")
                    )
                )
            ),
            options = listOf(
                ShellCommand.Option.Flag("noCache"),
                ShellCommand.Option.Flag("compress")
            )
        )

        // Big Bang Test that goes over most of the previous scenarios together in strange combinations
        val out = runScriptOrFail {
            +command

            +"""
                val command = docker.build {
                  +noCache
                  +compress
                  
                  +tag("MyImage")
            
                  +"path/to/Dockerfile"
                }
                
                print(command)
            """.trimIndent()
        }.trim()

        assertEquals(out, "docker build --noCache --compress --tag MyImage path/to/Dockerfile")
    }

}

data class ShellCommand(
    val name: List<String>,
    val subCommands: List<ShellCommand>,
    val options: List<Option>
) : GeneratedCode, Serializable {
    sealed class Option : Serializable {
        abstract val name: String

        data class Flag(override val name: String) : Option()
        data class Value(override val name: String) : Option()
    }

    override fun GeneratedCode.Builder.body() {
        val interfaceName = name.joinToString("") + "CommandBuilder"

        val parentInterfaceName = name
            .dropLast(1)
            .takeIf { it.isNotEmpty() }
            ?.joinToString("")
            ?.let { it + "CommandBuilder" }

        val currentInterface = `interface`(interfaceName) {
            parentInterfaceName?.let { implement(it) }
        }

        val commandName = name.joinToString(" ")
        val singleName = name.last()

        options.forEach { option ->
            when (option) {
                is Option.Flag -> {
                    extensionProperty(
                        name = option.name,
                        receiverType = CommandBuilder::class.withParameters(IdentifiableMember(interfaceName)),
                        getter = {
                            Flag("--${option.name}")
                        }
                    )
                }
                is Option.Value -> {
                    extensionProperty(
                        name = option.name,
                        receiverType = CommandBuilder::class.withParameters(IdentifiableMember(interfaceName)),
                        getter = {
                            Value("--${option.name}")
                        }
                    )
                }
            }
        }

        if (parentInterfaceName != null) {
            extensionProperty(
                name = singleName,
                receiverType = TypeSafeCommand::class.withParameters(IdentifiableMember(parentInterfaceName)),
                type = TypeSafeCommand::class.withParameters(IdentifiableMember(interfaceName)),
                getter = {
                    it.jvmErasure.primaryConstructor!!.call(commandName)
                }
            )
        } else {
            lazyProperty(singleName, TypeSafeCommand::class.withParameters(currentInterface)) {
                it.jvmErasure.primaryConstructor!!.call(commandName)
            }
        }

        subCommands.forEach {
            +it
        }
    }
}

data class TypeSafeCommand<C>(internal val name: String) {
    operator fun invoke(init: CommandBuilder<C>.() -> Unit = {}): String {
        return CommandBuilder<C>(name).apply {
            init()
        }.build()
    }
}

data class Flag(internal val name: String)

data class Value(internal val name: String?) {
    operator fun invoke(value: String) = listOf(name, value).mapNotNull { it }.joinToString(" ")
    operator fun invoke(file: File) = this(file.absolutePath)
}

class CommandBuilder<out C>(name: String) {
    private val builder = StringBuilder().apply { append(name) }

    operator fun String.unaryPlus() {
        builder.append(' ')
        builder.append(this)
    }

    operator fun File.unaryPlus() {
        +absolutePath
    }

    operator fun Flag.unaryPlus() {
        +name
    }

    internal fun build(): String {
        return builder.toString()
    }
}