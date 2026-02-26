/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.*
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.io.path.walk
import kotlin.streams.asStream

/**
 * Test utility for writing tests for all files within the maven repository.
 * Usage
 * ```
 * @RepositoryFileTest
 * fun test(file: Path) {
 *     // test file
 * }
 * ```
 *
 * @param glob Glob pattern to match files.
 */
@TestTemplate
@ExtendWith(RepositoryFileTestContextProvider::class)
annotation class RepositoryFileTest(val glob: String = "*")

private class RepositoryFileTestContextProvider : TestTemplateInvocationContextProvider {

    override fun supportsTestTemplate(context: ExtensionContext): Boolean {
        return context.requiredTestMethod.isAnnotationPresent(RepositoryFileTest::class.java)
    }

    override fun provideTestTemplateInvocationContexts(context: ExtensionContext): Stream<TestTemplateInvocationContext> {
        val glob = context.requiredTestMethod.getAnnotation(RepositoryFileTest::class.java).glob
        val mavenLocal = Path(mavenLocal)
        val matcher = mavenLocal.fileSystem.getPathMatcher("glob:$glob")
        return mavenLocal.walk().filter { file -> matcher.matches(file) }.asStream().map { file ->
            object : TestTemplateInvocationContext {
                override fun getDisplayName(invocationIndex: Int): String {
                    return file.name
                }

                override fun getAdditionalExtensions(): List<Extension> {
                    return listOf(object : ParameterResolver {
                        override fun supportsParameter(
                            parameterContext: ParameterContext?,
                            extensionContext: ExtensionContext?,
                        ): Boolean {
                            return parameterContext?.parameter?.type == Path::class.java
                        }

                        override fun resolveParameter(
                            parameterContext: ParameterContext?,
                            extensionContext: ExtensionContext?,
                        ): Any? {
                            return file
                        }
                    })
                }
            }
        }
    }
}
