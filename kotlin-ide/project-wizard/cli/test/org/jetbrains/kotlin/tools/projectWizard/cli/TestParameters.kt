/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.cli

import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor


interface TestParameters {
    companion object {
        inline fun <reified T : TestParameters> fromTestDataOrDefault(
            directory: Path,
            parametersFileName: String = "parameters.txt"
        ): T =
            fromTestData(directory, parametersFileName, T::class) ?: T::class.DEFAULT_VALUE

        val <T : TestParameters> KClass<T>.DEFAULT_VALUE
            get() = primaryConstructorOrError.callBy(emptyMap())

        fun <T : TestParameters> fromTestData(directory: Path, parametersFileName: String, klass: KClass<T>): T? {
            val file = directory.resolve(parametersFileName)
            if (!Files.exists(file) || !Files.isRegularFile(file)) return null
            val text = file.readFile()
            return parseTestParameters(text, klass)
        }

        private fun <T : TestParameters> parseTestParameters(text: String, klass: KClass<T>): T {
            val constructor = klass.primaryConstructorOrError
            val parameters = constructor.parameters.associateWith { parameter ->
                parameter.parseValue(text)
            }
            return constructor.callBy(parameters)
        }

        private fun KParameter.parseValue(text: String): Any? = when (type.classifier as? KClass<*>) {
            Boolean::class -> InTextDirectivesUtils.getPrefixedBoolean(text, name) ?: false
            else -> error("Invalid test parameter $name with type $type")
        }

        private val <T : Any> KClass<T>.primaryConstructorOrError
            get() = primaryConstructor ?: error("Primary constructor should present")
    }
}