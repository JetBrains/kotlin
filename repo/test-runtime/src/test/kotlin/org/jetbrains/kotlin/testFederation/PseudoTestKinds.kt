/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.junit.jupiter.params.provider.ValueSource
import org.junit.jupiter.params.support.ParameterDeclarations
import java.util.stream.Stream

class PseudoParameterizedTest {
    @ParameterizedTest
    @ValueSource(ints = [1, 2])
    fun domain(value: Int) = println("Executed: domain $value")

    @MustRunAlways
    @ParameterizedTest
    @ValueSource(ints = [1, 2])
    fun smoke(value: Int) = println("Executed: smoke $value")

    @MustRunOnChangesInJs
    @ParameterizedTest
    @ValueSource(ints = [1, 2])
    fun contract(value: Int) = println("Executed: contract $value")
}

class PseudoRepeatedTest {
    @RepeatedTest(2)
    fun domain() = println("Executed: domain")

    @MustRunAlways
    @RepeatedTest(2)
    fun smoke() = println("Executed: smoke")

    @MustRunOnChangesInJs
    @RepeatedTest(2)
    fun contract() = println("Executed: contract")
}

@ExtendWith(PseudoTemplateContextProvider::class)
class PseudoTemplateTest {
    @TestTemplate
    fun domain() = println("Executed: domain")

    @MustRunAlways
    @TestTemplate
    fun smoke() = println("Executed: smoke")

    @MustRunOnChangesInJs
    @TestTemplate
    fun contract() = println("Executed: contract")
}

class PseudoTemplateContextProvider : TestTemplateInvocationContextProvider {
    override fun supportsTestTemplate(context: ExtensionContext): Boolean = true

    override fun provideTestTemplateInvocationContexts(context: ExtensionContext): Stream<TestTemplateInvocationContext> =
        Stream.of(object : TestTemplateInvocationContext {}, object : TestTemplateInvocationContext {})
}

class PseudoDynamicTest {
    @TestFactory
    fun domain() = listOf(
        dynamicTest("first") { println("Executed: domain 1") },
        dynamicTest("second") { println("Executed: domain 2") },
    ).also { println("Created: domain") }

    @MustRunAlways
    @TestFactory
    fun smoke() = listOf(
        dynamicTest("first") { println("Executed: smoke 1") },
        dynamicTest("second") { println("Executed: smoke 2") },
    ).also { println("Created: smoke") }

    @MustRunOnChangesInJs
    @TestFactory
    fun contract() = listOf(
        dynamicTest("first") { println("Executed: contract 1") },
        dynamicTest("second") { println("Executed: contract 2") },
    ).also { println("Created: contract") }
}

class PseudoDynamicContainerTest {
    @TestFactory
    fun domain(): DynamicContainer = dynamicContainer(
        "container", listOf(
            dynamicTest("first") { println("Executed: domain 1") },
            dynamicTest("second") { println("Executed: domain 2") },
        )
    ).also { println("Created: domain") }

    @MustRunAlways
    @TestFactory
    fun smoke(): DynamicContainer = dynamicContainer(
        "container", listOf(
            dynamicTest("first") { println("Executed: smoke 1") },
            dynamicTest("second") { println("Executed: smoke 2") },
        )
    ).also { println("Created: smoke") }

    @MustRunOnChangesInJs
    @TestFactory
    fun contract(): DynamicContainer = dynamicContainer(
        "container", listOf(
            dynamicTest("first") { println("Executed: contract 1") },
            dynamicTest("second") { println("Executed: contract 2") },
        )
    ).also { println("Created: contract") }
}

class PseudoNestedTest {
    @Nested
    inner class DomainTests {
        @Test
        fun domain() = println("Executed: domain")
    }

    @MustRunAlways
    @Nested
    inner class SmokeTests {
        @Test
        fun smoke() = println("Executed: smoke")
    }

    @MustRunOnChangesInJs
    @Nested
    inner class ContractTests {
        @Test
        fun contract() = println("Executed: contract")
    }
}

open class PseudoLifecycleTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun beforeAll() = println("Lifecycle: beforeAll")

        @JvmStatic
        @AfterAll
        fun afterAll() = println("Lifecycle: afterAll")
    }

    @Test
    fun domain() = println("Executed: domain")

    @MustRunAlways
    @Test
    fun smoke() = println("Executed: smoke")

    @MustRunOnChangesInJs
    @Test
    fun contract() = println("Executed: contract")
}

@Suppress("JUnitTestCaseWithNoTests")
class PseudoInheritedTest : PseudoLifecycleTest()
