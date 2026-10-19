/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package multiplatform

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.DClass
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInAnalysisApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@MustRunOnChangesInAnalysisApi
internal class CrossSourceSetSupertypeReanalysisTest : BaseAbstractTest() {
    @Test
    fun `common subclass inherits members of actual JVM base class`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                val common by sourceSet {
                    name = "common"
                    analysisPlatform = "common"
                    sourceRoots = listOf("src/common/Test.kt")
                }
                sourceSet {
                    name = "jvm"
                    analysisPlatform = "jvm"
                    sourceRoots = listOf("src/jvm/Test.kt")
                    dependentSourceSets = setOf(common.sourceSetID)
                }
            }
        }

        testInline(
            """
                |/src/common/Test.kt
                |package example
                |public expect open class Base()
                |public class Derived: Base() {
                |    public fun bar() {}
                |}
                |
                |/src/jvm/Test.kt
                |package example
                |public actual open class Base {
                |    public fun foo() {}
                |}
                """.trimMargin(),
            configuration
        ) {
            documentablesCreationStage = { modules ->
                val derivedClasses = modules.flatMap { module ->
                    module.packages.flatMap { it.classlikes }.filterIsInstance<DClass>().filter { it.name == "Derived" }
                }.associateBy { it.sourceSets.single().sourceSetID.sourceSetName }
                assertEquals(setOf("common", "jvm"), derivedClasses.keys)

                val commonDerived = derivedClasses.getValue("common")
                assertTrue(commonDerived.functions.any { it.name == "bar" })
                assertFalse(commonDerived.functions.any { it.name == "foo" })

                val jvmDerived = derivedClasses.getValue("jvm")
                assertTrue(jvmDerived.functions.any { it.name == "bar" })
                assertEquals(1, jvmDerived.constructors.size)
                val function = jvmDerived.functions.single { it.name == "foo" }
                assertEquals(setOf("jvm"), function.sourceSets.map { it.sourceSetID.sourceSetName }.toSet())
            }
        }
    }

    @Test
    fun `common subclass inherits members of actual JVM base class from different package`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                val common by sourceSet {
                    name = "common"
                    analysisPlatform = "common"
                    sourceRoots = listOf("src/common")
                }
                sourceSet {
                    name = "jvm"
                    analysisPlatform = "jvm"
                    sourceRoots = listOf("src/jvm/Test.kt")
                    dependentSourceSets = setOf(common.sourceSetID)
                }
            }
        }

        testInline(
            """
                |/src/common/example.kt
                |package example
                |public expect open class Base()
                |
                |/src/common/example2.kt
                |package example2
                |public class Derived: example.Base() {
                |    public fun bar() {}
                |}
                |/src/jvm/Test.kt
                |package example
                |public actual open class Base {
                |    public fun foo() {}
                |}
                """.trimMargin(),
            configuration
        ) {
            documentablesCreationStage = { modules ->
                val derivedClasses = modules.flatMap { module ->
                    module.packages.flatMap { it.classlikes }.filterIsInstance<DClass>().filter { it.name == "Derived" }
                }.associateBy { it.sourceSets.single().sourceSetID.sourceSetName }
                assertEquals(setOf("common", "jvm"), derivedClasses.keys)

                val commonDerived = derivedClasses.getValue("common")
                assertTrue(commonDerived.functions.any { it.name == "bar" })
                assertFalse(commonDerived.functions.any { it.name == "foo" })

                val jvmDerived = derivedClasses.getValue("jvm")
                assertTrue(jvmDerived.functions.any { it.name == "bar" })
                assertEquals(1, jvmDerived.constructors.size)
                val function = jvmDerived.functions.single { it.name == "foo" }
                assertEquals(setOf("jvm"), function.sourceSets.map { it.sourceSetID.sourceSetName }.toSet())
            }
        }
    }

    @Test
    fun `common Throwable subclass inherits JVM members through a transitive source set`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                val common by sourceSet {
                    name = "common"
                    analysisPlatform = "common"
                    sourceRoots = listOf("src/common/Test.kt")
                }
                val shared by sourceSet {
                    name = "shared"
                    analysisPlatform = "common"
                    dependentSourceSets = setOf(common.sourceSetID)
                }
                sourceSet {
                    name = "jvm"
                    analysisPlatform = "jvm"
                    sourceRoots = listOf("src/jvm/Test.kt")
                    dependentSourceSets = setOf(shared.sourceSetID)
                }
            }
        }

        testInline(
            """
                |/src/common/Test.kt
                |package example
                |class SomeClass : Throwable()
                |
                |/src/jvm/Test.kt
                |package example
                |fun useException(exception: SomeClass) {}
                """.trimMargin(),
            configuration
        ) {
            documentablesCreationStage = { modules ->
                val classes = modules.filter { module -> module.packages.any { it.classlikes.isNotEmpty() } }.associate { module ->
                    val sourceSet = module.sourceSets.single()
                    sourceSet.sourceSetID.sourceSetName to (module.packages.single().classlikes.single() as DClass)
                }
                assertEquals(setOf("common", "shared", "jvm"), classes.keys)
                for (sourceSet in classes.keys) {
                    val klass = classes.getValue(sourceSet)
                    assertEquals("SomeClass", klass.name)
                    assertEquals(setOf(sourceSet), klass.sourceSets.map { it.sourceSetID.sourceSetName }.toSet())
                }

                // java.lang.Throwable adds these methods only in the JVM analysis session.
                val jvmOnlyFunctions = setOf("fillInStackTrace", "printStackTrace")
                for (sourceSet in listOf("common", "shared")) {
                    assertFalse(classes.getValue(sourceSet).functions.any { it.name in jvmOnlyFunctions })
                }
                val jvmFunctions = classes.getValue("jvm").functions
                assertTrue(jvmFunctions.map { it.name }.containsAll(jvmOnlyFunctions))
                jvmFunctions.filter { it.name in jvmOnlyFunctions }.forEach { function ->
                    assertEquals(setOf("jvm"), function.sourceSets.map { it.sourceSetID.sourceSetName }.toSet())
                }
            }
        }
    }
}
