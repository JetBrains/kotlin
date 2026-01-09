/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven.test

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.junit.jupiter.params.support.ParameterDeclarations
import java.util.stream.Stream
import kotlin.streams.asStream

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MavenVersions
@ParameterizedTest(name = "{0}: {displayName}")
@ArgumentsSource(MavenTestArgumentsProvider::class)
annotation class MavenTest

class MavenTestArgumentsProvider : ArgumentsProvider {
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
        val mavenVersionsAnnotation = findAnnotationOrNull<MavenVersions>(context)
            ?: throw IllegalArgumentException("@MavenVersions annotation is required when maven parameter is present")

        val mavenVersions = buildSet {
            add(mavenVersionsAnnotation.min)
            addAll(mavenVersionsAnnotation.additional.toList())
            add(mavenVersionsAnnotation.max)
        }

        return mavenVersions.asSequence().map { mavenVersion ->
            parameters.all.map { parameter ->
                when (parameter.parameterType) {
                    TestVersions.Maven::class.java -> mavenVersion
                    else -> throw IllegalArgumentException("Unexpected test parameter: $parameter")
                }
            }.let { Arguments.of(*it.toTypedArray<Any>()) }
        }.asStream()
    }
}

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class MavenVersions(
    val min: String = TestVersions.Maven.MIN_SUPPORTED,
    val max: String = TestVersions.Maven.MAX_SUPPORTED,
    val additional: Array<String> = [],
)