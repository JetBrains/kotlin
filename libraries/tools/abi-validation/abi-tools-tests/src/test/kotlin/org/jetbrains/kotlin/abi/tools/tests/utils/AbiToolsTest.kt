/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.tests.utils

import org.jetbrains.kotlin.abi.tools.AbiTools
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream

/**
 * Marks a test method to be executed with both Original and Embeddable [AbiTools] instances.
 *
 * The annotated method must have [AbiTools] as its first parameter (or receiver).
 * An optional parameter of type [Path] will receive a temporary directory.
 *
 * Two subtests will be created: `Original` and `Embeddable`.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@TestTemplate
@ExtendWith(AbiToolsTestInvocationProvider::class)
annotation class AbiToolsTest

private class AbiToolsTestInvocationProvider : TestTemplateInvocationContextProvider {
    override fun supportsTestTemplate(context: ExtensionContext): Boolean {
        return context.testMethod.isPresent &&
                context.testMethod.get().isAnnotationPresent(AbiToolsTest::class.java)
    }

    override fun provideTestTemplateInvocationContexts(context: ExtensionContext): Stream<TestTemplateInvocationContext> {
        return Stream.of(
            abiToolsInvocationContext("Original", abiToolsOriginal),
            abiToolsInvocationContext("Embeddable", abiToolsEmbeddable),
        )
    }
}

private fun abiToolsInvocationContext(displayName: String, abiTools: AbiTools): TestTemplateInvocationContext {
    return object : TestTemplateInvocationContext {
        override fun getDisplayName(invocationIndex: Int): String = "[$displayName]"

        override fun getAdditionalExtensions(): List<Extension> {
            return listOf(AbiToolsParameterResolver(abiTools))
        }
    }
}

private class AbiToolsParameterResolver(private val abiTools: AbiTools) : ParameterResolver {
    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        val type = parameterContext.parameter.type
        return type == AbiTools::class.java || type == Path::class.java
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        val type = parameterContext.parameter.type
        if (type == AbiTools::class.java) {
            return abiTools
        }
        if (type == Path::class.java) {
            val store = extensionContext.getStore(ExtensionContext.Namespace.create(AbiToolsTestInvocationProvider::class.java))
            return store.getOrComputeIfAbsent("tempDir", { Files.createTempDirectory("abi-tools-test") }, Path::class.java)
        }
        throw ParameterResolutionException("Unsupported parameter type: $type")
    }
}
