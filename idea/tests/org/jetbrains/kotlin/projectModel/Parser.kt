/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.projectModel

import com.intellij.util.text.nullize
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import java.io.File
import java.io.InputStreamReader
import java.io.Reader

open class ProjectStructureParser(private val projectRoot: File) {
    private val builderByName: MutableMap<String, ResolveModule.Builder> = hashMapOf()
    private val predefinedBuilders: Map<String, ResolveLibrary.Builder> = hashMapOf(
        "STDLIB_JS" to ResolveLibrary.Builder(Stdlib.JsStdlib),
        "STDLIB_JVM" to ResolveLibrary.Builder(Stdlib.JvmStdlib),
        "STDLIB_COMMON" to ResolveLibrary.Builder(Stdlib.CommonStdlib),
        "FULL_JDK" to ResolveLibrary.Builder(FullJdk),
        "MOCK_JDK" to ResolveLibrary.Builder(MockJdk),
        "KOTLIN_TEST_JS" to ResolveLibrary.Builder(KotlinTest.JsKotlinTest),
        "KOTLIN_TEST_JVM" to ResolveLibrary.Builder(KotlinTest.JvmKotlinTest)
    )

    fun parse(text: String): ProjectResolveModel {
        val reader = InputStreamReader(text.byteInputStream())
        while (reader.parseNextDeclaration()) {
        }

        val projectBuilder = ProjectResolveModel.Builder()
        projectBuilder.modules.addAll(builderByName.values)

        return projectBuilder.build()
    }

    private fun Reader.parseNextDeclaration(): Boolean {
        val firstWord = nextWord() ?: return false
        when (firstWord) {
            DEFINE_MODULE_KEYWORD -> parseModuleDefinition()
            LINE_COMMENT_TOKEN -> consumeUntilFirst { it == '\n' }
            else -> parseDependenciesDefinition(firstWord)
        }
        return true
    }

    private fun Reader.parseModuleDefinition() {
        val name = nextWord()!!

        // skip until attributes list begins
        consumeUntilFirst { it.toString() == ATTRIBUTES_OPENING_BRACKET }

        // read whole attributes list
        val attributesMap = readAttributes()

        require(builderByName[name] == null) { "Redefinition of module $name" }
        builderByName[name] = ResolveModule.Builder().also {
            it.name = name
            initializeModuleByAttributes(it, attributesMap)
        }
    }

    protected open fun initializeModuleByAttributes(builder: ResolveModule.Builder, attributes: Map<String, String>) {
        val platformAttribute = attributes["platform"]
        requireNotNull(platformAttribute) { "Missing required attribute 'platform' for module ${builder.name}" }
        builder.platform = parsePlatform(platformAttribute)

        val root = attributes["root"] ?: builder.name!!
        builder.root = File(projectRoot, root)

        builder.additionalCompilerArgs = attributes["additionalCompilerArgs"]

        val testRoot = attributes["testRoot"]
        if (testRoot != null) builder.testRoot = File(projectRoot, testRoot)
    }

    private fun Reader.parseDependenciesDefinition(fromName: String) {
        fun getDeclaredBuilder(name: String): ResolveModule.Builder =
            requireNotNull(builderByName[name] ?: predefinedBuilders[name]) {
                "Module $name wasn't declared. All modules should be declared explicitly"
            }

        val fromBuilder = getDeclaredBuilder(fromName)

        val arrow = nextWord()
        require(arrow == DEPENDENCIES_ARROW) {
            "Malformed declaration: '$fromName $arrow ...' \n$HELP_TEXT"
        }

        val dependencies = consumeUntilFirst { it == '{' }.split(",").map {
            val toBuilder = getDeclaredBuilder(it.trim())
            ResolveDependency.Builder().apply {
                to = toBuilder
            }
        }

        fromBuilder.dependencies.addAll(dependencies)

        val attributes = readAttributes()
        initializeDependenciesByAttributes(dependencies, attributes)
    }

    protected open fun initializeDependenciesByAttributes(dependencies: List<ResolveDependency.Builder>, attributes: Map<String, String>) {
        fun applyForEach(action: (ResolveDependency.Builder).() -> Unit) {
            dependencies.forEach(action)
        }

        val kindAttribute = attributes["kind"]
        requireNotNull(kindAttribute) { "Missing required attribute 'kind' for dependencies ${dependencies.joinToString()}" }
        applyForEach { kind = ResolveDependency.Kind.valueOf(kindAttribute) }

    }

    private fun Reader.readAttributes(): Map<String, String> {
        val attributesString = consumeUntilFirst { it.toString() == ATTRIBUTES_CLOSING_BRACKET }

        return attributesString
            .split(ATTRIBUTES_SEPARATOR)
            .map { it.splitIntoTwoParts(ATTRIBUTE_VALUE_SEPARATOR) }
            .toMap()
    }

    private fun parsePlatform(platformString: String): TargetPlatform {
        val platformsByPlatformName = CommonPlatforms.allSimplePlatforms
            .map { it.single().toString() to it.single() }
            .toMap()

        val platforms = parseRepeatableAttribute(platformString).map {
            when (it) {
                "JVM" -> JvmPlatforms.defaultJvmPlatform.single()
                "Native" -> NativePlatforms.unspecifiedNativePlatform.single()
                else -> platformsByPlatformName[it] ?: error(
                    "Unknown platform $it. Available platforms: ${platformsByPlatformName.keys.joinToString()}"
                )
            }
        }.toSet()

        return TargetPlatform(platforms)
    }

    protected fun parseRepeatableAttribute(value: String): List<String> {
        require(value.startsWith(REPEATABLE_ATTRIBUTE_OPENING_BRACKET) && value.endsWith(REPEATABLE_ATTRIBUTE_CLOSING_BRACKET)) {
            "Value of repeatable attribute should be declared in square brackets: [foo, bar, baz]"
        }
        return value.removePrefix(REPEATABLE_ATTRIBUTE_OPENING_BRACKET)
            .removeSuffix(REPEATABLE_ATTRIBUTE_CLOSING_BRACKET)
            .split(REPEATABLE_ATTRIBUTE_VALUES_SEPARATOR)
            .map { it.trim() }
    }


    companion object {
        const val DEFINE_MODULE_KEYWORD = "MODULE"
        const val DEPENDENCIES_ARROW = "->"

        const val ATTRIBUTES_OPENING_BRACKET = "{"
        const val ATTRIBUTES_CLOSING_BRACKET = "}"
        const val ATTRIBUTES_SEPARATOR = ";"
        const val ATTRIBUTE_VALUE_SEPARATOR = "="

        const val REPEATABLE_ATTRIBUTE_OPENING_BRACKET = "["
        const val REPEATABLE_ATTRIBUTE_CLOSING_BRACKET = "]"
        const val REPEATABLE_ATTRIBUTE_VALUES_SEPARATOR = ","

        const val LINE_COMMENT_TOKEN = "//"

        val HELP_TEXT = "Possible declarations:\n" +
                "- Module declaration: 'MODULE myModuleName { ...attributes... }\n" +
                "- Module dependencies: myModuleName -> otherModule1, otherModule2, ..." +
                "Note that each module should be explicitly declared before referring to it in dependencies"
    }
}

fun Reader.consumeUntilFirst(shouldStop: (Char) -> Boolean): String {
    var char = nextChar()
    return buildString {
        while (char != null && !shouldStop(char!!)) {
            append(char!!)
            char = nextChar()
        }
    }.trim()
}

private fun Reader.nextChar(): Char? =
    read().takeUnless { it == -1 }?.toChar()

private fun Reader.nextWord(): String? {
    var char = nextChar()
    return buildString {
        // Skip all separators
        while (char != null && char!!.isSeparator()) {
            char = nextChar()
        }

        // Read the word
        while (char != null && !char!!.isSeparator()) {
            append(char!!)
            char = nextChar()
        }
    }.nullize()
}

private fun Char.isSeparator() = isWhitespace() || this == '\n'

private fun String.splitIntoTwoParts(separator: String): Pair<String, String> {
    val result = split(separator)
    val name = substringBefore(separator, "")
    val value = substringAfter(separator, "")
    require(name.isNotEmpty()) { "$this can not be split into two parts with separator $separator" }
    return name.trim() to value.trim()
}