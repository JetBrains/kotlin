/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.documentable

import org.jetbrains.dokka.analysis.test.OnlyDescriptors
import org.jetbrains.dokka.analysis.test.OnlySymbols
import org.jetbrains.dokka.analysis.test.api.kotlinJvmTestProject
import org.jetbrains.dokka.analysis.test.api.parse
import org.jetbrains.dokka.analysis.test.api.useServices
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.ObviousMember
import org.junit.jupiter.api.Nested
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ObviousFunctionsTest {

    @OnlyDescriptors("#3354")
    @Test
    fun `kotlin_Any should not have obvious members`() {
        val project = kotlinJvmTestProject {
            ktFile("kotlin/Any.kt") {
                +"""
                    package kotlin
                    public open class Any {
                        public open fun equals(other: Any?): Boolean
                        public open fun hashCode(): Int
                        public open fun toString(): String
                    }
                """
            }
        }

        val module = project.parse()

        val pkg = module.packages.single()
        val any = pkg.classlikes.single()

        assertEquals("Any", any.name)

        assertObviousFunctions(
            expectedObviousFunctions = emptySet(),
            expectedNonObviousFunctions = setOf("equals", "hashCode", "toString"),
            actualFunctions = any.functions
        )
    }

    @Test
    fun `kotlin_Any should not have obvious members via external documentable provider`() {
        val project = kotlinJvmTestProject {
            ktFile("SomeClass.kt") {
                +"class SomeClass"
            }
        }

        project.useServices {
            val any = externalDocumentableProvider.getClasslike(
                DRI("kotlin", "Any"),
                it.context.configuration.sourceSets.single()
            )
            assertNotNull(any)
            assertEquals("Any", any.name)

            assertObviousFunctions(
                expectedObviousFunctions = emptySet(),
                expectedNonObviousFunctions = setOf("equals", "hashCode", "toString"),
                actualFunctions = any.functions
            )
        }
    }

    // when running with K2 - kotlin package is skipped
    @OnlyDescriptors("#3354")
    @Test
    fun `kotlin_Enum should not have obvious members`() {
        val project = kotlinJvmTestProject {
            ktFile("kotlin/Any.kt") {
                +"""
                    package kotlin
                    public abstract class Enum<E : Enum<E>>(name: String, ordinal: Int): Comparable<E> {
                        public override final fun compareTo(other: E): Int
                        public override final fun equals(other: Any?): Boolean
                        public override final fun hashCode(): Int
                        public override fun toString(): String
                    }
                """
            }
        }

        val module = project.parse()

        val pkg = module.packages.single()
        val any = pkg.classlikes.single()

        assertEquals("Enum", any.name)

        assertObviousFunctions(
            expectedObviousFunctions = emptySet(),
            expectedNonObviousFunctions = setOf("compareTo", "equals", "hashCode", "toString"),
            actualFunctions = any.functions
        )
    }

    @Nested
    inner class KotlinEnumHaveNoObviousMembersViaExternalDocumentableTest {
        @OnlyDescriptors("In K2 there is finalize method with the Deprecated annotation.  In K1 it is unavailable")
        @Test
        fun `K1 - kotlin_Enum should not have obvious members via external documentable provider`() {
            `kotlin_Enum should not have obvious members via external documentable provider`(false)
        }

        @OnlySymbols("In K2 there is finalize method with the Deprecated annotation.  In K1 it is unavailable")
        @Test
        fun `K2 - kotlin_Enum should not have obvious members via external documentable provider`() {
            `kotlin_Enum should not have obvious members via external documentable provider`(true)
        }

        private fun `kotlin_Enum should not have obvious members via external documentable provider`(
            isFinalizeAvailableJDK18: Boolean
        ) {
            val project = kotlinJvmTestProject {
                ktFile("SomeClass.kt") {
                    +"class SomeClass"
                }
            }

            project.useServices {
                val enum = externalDocumentableProvider.getClasslike(
                    DRI("kotlin", "Enum"),
                    it.context.configuration.sourceSets.single()
                )
                assertNotNull(enum)
                assertEquals("Enum", enum.name)

                val javaVersion = when (val specVersion = System.getProperty("java.specification.version")) {
                    "1.8" -> 8
                    else -> specVersion.toInt()
                }

                // inherited from java enum
                val jdkEnumInheritedFunctions = when {
                    // starting from JDK 18,
                    // In K1 'finalize' is not available, but in K2 it is deprecated (finalization is deprecated in JDK 18)
                    javaVersion >= 18 -> if (isFinalizeAvailableJDK18) setOf(
                        "clone", "getDeclaringClass", "describeConstable", "finalize"
                    ) else setOf("clone", "getDeclaringClass", "describeConstable")
                    // starting from JDK 12, there is a new member in enum 'describeConstable'
                    javaVersion >= 12 -> setOf("clone", "getDeclaringClass", "describeConstable", "finalize")
                    else -> setOf("clone", "getDeclaringClass", "finalize")
                }

                assertObviousFunctions(
                    expectedObviousFunctions = emptySet(),
                    expectedNonObviousFunctions = setOf("compareTo", "equals", "hashCode", "toString") +
                            jdkEnumInheritedFunctions,
                    actualFunctions = enum.functions
                )
            }
        }
    }

    @Test
    fun `should mark only toString, equals and hashcode as obvious for class`() {
        val project = kotlinJvmTestProject {
            ktFile("SomeClass.kt") {
                +"""
                    class SomeClass {
                        fun custom() {}
                    }
                """
            }
        }

        val module = project.parse()

        val pkg = module.packages.single()
        val cls = pkg.classlikes.single()

        assertEquals("SomeClass", cls.name)

        assertObviousFunctions(
            expectedObviousFunctions = setOf("equals", "hashCode", "toString"),
            expectedNonObviousFunctions = setOf("custom"),
            actualFunctions = cls.functions
        )
    }

    @Test
    fun `should mark only toString, equals and hashcode as obvious for interface`() {
        val project = kotlinJvmTestProject {
            ktFile("SomeClass.kt") {
                +"""
                    interface SomeClass {
                        fun custom()
                    }
                """
            }
        }

        val module = project.parse()

        val pkg = module.packages.single()
        val cls = pkg.classlikes.single()

        assertEquals("SomeClass", cls.name)

        assertObviousFunctions(
            expectedObviousFunctions = setOf("equals", "hashCode", "toString"),
            expectedNonObviousFunctions = setOf("custom"),
            actualFunctions = cls.functions
        )
    }

    @Test
    fun `should mark data class generated functions as obvious`() {
        val project = kotlinJvmTestProject {
            ktFile("SomeClass.kt") {
                +"""
                    data class SomeClass(val x: String) {
                        fun custom() {}
                    }
                """
            }
        }

        val module = project.parse()

        val pkg = module.packages.single()
        val cls = pkg.classlikes.single()

        assertEquals("SomeClass", cls.name)

        assertObviousFunctions(
            expectedObviousFunctions = setOf("equals", "hashCode", "toString", "component1", "copy"),
            expectedNonObviousFunctions = setOf("custom"),
            actualFunctions = cls.functions
        )
    }

    @Test
    fun `should not mark as obvious if override`() {
        val project = kotlinJvmTestProject {
            ktFile("SomeClass.kt") {
                +"""
                    data class SomeClass(val x: String) {
                        override fun toString(): String = x
                    }
                """
            }
        }

        val module = project.parse()

        val pkg = module.packages.single()
        val cls = pkg.classlikes.single()

        assertEquals("SomeClass", cls.name)

        assertObviousFunctions(
            expectedObviousFunctions = setOf("equals", "hashCode", "component1", "copy"),
            expectedNonObviousFunctions = setOf("toString"),
            actualFunctions = cls.functions
        )
    }

    private fun assertObviousFunctions(
        expectedObviousFunctions: Set<String>,
        expectedNonObviousFunctions: Set<String>,
        actualFunctions: List<DFunction>
    ) {
        val (notObviousFunctions, obviousFunctions) = actualFunctions.partition {
            it.extra[ObviousMember] == null
        }

        assertEquals(
            expectedNonObviousFunctions,
            notObviousFunctions.map { it.name }.toSet()
        )

        assertEquals(
            expectedObviousFunctions,
            obviousFunctions.map { it.name }.toSet()
        )
    }

}
