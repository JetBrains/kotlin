/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.modelx.util

import org.jetbrains.kotlin.cli.common.arguments.Argument
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.DeprecatedOption
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import java.io.Writer
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.outputStream
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.typeOf

class CompilerSettingsGenerator(
    private val packageFqn: String = "org.jetbrains.kotlin.project.modelx.compilerSetting",
    private val relationTypeFqn: String = "org.jetbrains.kotlin.project.modelx.RelationType"
) {
    fun generate(settingsArrayName: String, classToScan: KClass<*>, dst: Path) {
        val settings = classToScan
            .declaredMemberProperties
            .asSequence()
            .filter { prop -> !prop.hasAnnotation<DeprecatedOption>() }
            .map { prop -> prop to prop.findAnnotation<Argument>() }
            .filterOutNulls()
            .map { (prop, argAnno) ->
                val key = argAnno.value.formatCompilerSettingKey()
                val descr = argAnno.description
                val valueType = prop.returnType

                fillInTemplate(key, descr, valueType)
            }

        dst.resolve("$settingsArrayName.kt")
            .outputStream().use {
            val writer = it.bufferedWriter()
            writer.writeSettingsFile(settingsArrayName, settings)
            writer.flush()
        }
    }

    private fun Writer.writeSettingsFile(settingsArrayName: String, settings: Sequence<String>) {
        val indent = "    "
        write("package $packageFqn\n\n")
        write("import kotlin.reflect.typeOf\n")
        write("import $relationTypeFqn\n")
        write("import $relationTypeFqn.*\n\n")
        write("@ExperimentalStdlibApi\n")
        write("val $settingsArrayName = with(DirectArgumentsContributor) {\n")
        write("arrayOf(\n".prependIndent(indent))
        settings.forEach { write("${it.prependIndent(indent.repeat(2))},\n") }
        write(")\n".prependIndent(indent))
        write("}\n")
    }

    private fun String.formatCompilerSettingKey() = removePrefix("-X").removePrefix("-")

    private fun fillInTemplate(
        key: String,
        descr: String,
        valueType: KType
    ) = """
        CompilerSettingContainer(
            SimpleCompilerSetting(
                key = "$key",
                description = ""${'"'}${descr.replace("\n", " ")}${'"'}"",
                consistencyRule = DONT_CARE,
                valueType = typeOf<${valueType}>()
            ),
            ${fillSerializerTemplate(valueType)},
            TODO() // CommonCompilerArguments::allowResultReturnType.asContributor()
        )
    """.trimIndent()

    @OptIn(ExperimentalStdlibApi::class)
    private fun fillSerializerTemplate(valueType: KType): String = when(valueType) {
        typeOf<Boolean>() -> "BoolSerializer"
        else -> """
            DelegatedSerializer(
                serialize = TODO(), // LanguageVersion::description
                deserialize = TODO() // LanguageVersion.Companion::fromVersionString
            )
        """.trimIndent()
    }
}

private fun <A, B> Sequence<Pair<A?, B?>>.filterOutNulls(): Sequence<Pair<A, B>> = mapNotNull { p ->
    if (p.first == null || p.second == null) {
        null
    } else {
        p as Pair<A, B>
    }
}

fun main() {
    val generator = CompilerSettingsGenerator()

    val out = Paths.get(
        "/Users/anton.lakotka/Projects" +
                "/kotlin/libraries/tools/kotlin-project-model-impl" +
                "/src/main/kotlin/compilerSetting"
    )

    generator.generate("jvmCompilerSettings2", K2JVMCompilerArguments::class, out)
    generator.generate("commonCompilerSettings2", CommonCompilerArguments::class, out)
}

/*
        CompilerSettingContainer(
            SimpleCompilerSetting(
                key = "language-version",
                description = """Provide source compatibility with the specified version of Kotlin""",
                consistencyRule = DONT_CARE,
                valueType = typeOf<LanguageVersion>()
            ),
            DelegatedSerializer(
                serialize = LanguageVersion::description,
                deserialize = LanguageVersion.Companion::fromVersionString
            ),
            CommonCompilerArguments::languageVersion.asContributor()
        ),
 */