/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package expectActuals

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.withDescendants
import org.jetbrains.dokka.pages.ClasslikePageNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class ExpectActualsTest : BaseAbstractTest() {

    private val multiplatformConfiguration = dokkaConfiguration {
        sourceSets {
            val commonId = sourceSet {
                sourceRoots = listOf("src/common/")
                analysisPlatform = "common"
                name = "common"
                displayName = "common"
            }.value.sourceSetID
            sourceSet {
                sourceRoots = listOf("src/jvm/")
                analysisPlatform = "jvm"
                name = "jvm"
                displayName = "jvm"
                dependentSourceSets = setOf(commonId)
            }
            sourceSet {
                sourceRoots = listOf("src/native/")
                analysisPlatform = "native"
                name = "native"
                displayName = "native"
                dependentSourceSets = setOf(commonId)
            }
        }
    }
    private val commonSourceSetId =
        multiplatformConfiguration.sourceSets.single { it.displayName == "common" }.sourceSetID

    @Test
    fun `property inside expect class should be marked as expect`() = testInline(
        """
        /src/common/test.kt
        expect class ExpectActualClass {
          val property: String?
        }
        
        /src/jvm/test.kt
        actual class ExpectActualClass {
          actual val property: String? = null
        }
        
        /src/native/test.kt
        actual class ExpectActualClass {
          actual val property: String? = null
        }
        """.trimMargin(),
        multiplatformConfiguration
    ) {
        documentablesTransformationStage = { module ->
            val cls = module.packages.single().classlikes.single { it.name == "ExpectActualClass" }
            assertTrue(cls.isExpectActual)
            assertEquals(commonSourceSetId, cls.expectPresentInSet?.sourceSetID)
            val property = cls.properties.single { it.name == "property" }
            assertTrue(property.isExpectActual)
            assertEquals(commonSourceSetId, property.expectPresentInSet?.sourceSetID)
        }
    }

    @Test
    fun `function inside expect class should be marked as expect`() = testInline(
        """
        /src/common/test.kt
        expect class ExpectActualClass {
          fun function(): String?
        }
        
        /src/jvm/test.kt
        actual class ExpectActualClass {
          actual fun function(): String? = null
        }
        
        /src/native/test.kt
        actual class ExpectActualClass {
          actual fun function(): String? = null
        }
        """.trimMargin(),
        multiplatformConfiguration
    ) {
        documentablesTransformationStage = { module ->
            val cls = module.packages.single().classlikes.single { it.name == "ExpectActualClass" }
            assertTrue(cls.isExpectActual)
            assertEquals(commonSourceSetId, cls.expectPresentInSet?.sourceSetID)
            val function = cls.functions.single { it.name == "function" }
            assertTrue(function.isExpectActual)
            assertEquals(commonSourceSetId, function.expectPresentInSet?.sourceSetID)
        }
    }

    @Test
    fun `top level expect property should be marked as expect`() = testInline(
        """
        /src/common/test.kt
        expect val property: String?
        
        /src/jvm/test.kt
        actual val property: String? = null
        
        /src/native/test.kt
        actual val property: String? = null
        """.trimMargin(),
        multiplatformConfiguration
    ) {
        documentablesTransformationStage = { module ->
            val property = module.packages.single().properties.single { it.name == "property" }
            assertTrue(property.isExpectActual)
            assertEquals(commonSourceSetId, property.expectPresentInSet?.sourceSetID)
        }
    }

    @Test
    fun `top level expect function should be marked as expect`() = testInline(
        """
        /src/common/test.kt
        expect fun function(): String?
        
        /src/jvm/test.kt
        actual fun function(): String? = null
        
        /src/native/test.kt
        actual fun function(): String? = null
        """.trimMargin(),
        multiplatformConfiguration
    ) {
        documentablesTransformationStage = { module ->
            val function = module.packages.single().functions.single { it.name == "function" }
            assertTrue(function.isExpectActual)
            assertEquals(commonSourceSetId, function.expectPresentInSet?.sourceSetID)
        }
    }

    @Test
    fun `three same named expect actual classes`() {

        val configuration = dokkaConfiguration {
            moduleName = "example"
            sourceSets {
                val common = sourceSet {
                    name = "common"
                    displayName = "common"
                    analysisPlatform = "common"
                    sourceRoots = listOf("src/commonMain/kotlin/pageMerger/Test.kt")
                }
                val commonJ = sourceSet {
                    name = "commonJ"
                    displayName = "commonJ"
                    analysisPlatform = "common"
                    sourceRoots = listOf("src/commonJMain/kotlin/pageMerger/Test.kt")
                    dependentSourceSets = setOf(common.value.sourceSetID)
                }
                val commonN1 = sourceSet {
                    name = "commonN1"
                    displayName = "commonN1"
                    analysisPlatform = "common"
                    sourceRoots = listOf("src/commonN1Main/kotlin/pageMerger/Test.kt")
                    dependentSourceSets = setOf(common.value.sourceSetID)
                }
                val commonN2 = sourceSet {
                    name = "commonN2"
                    displayName = "commonN2"
                    analysisPlatform = "common"
                    sourceRoots = listOf("src/commonN2Main/kotlin/pageMerger/Test.kt")
                    dependentSourceSets = setOf(common.value.sourceSetID)
                }
                sourceSet {
                    name = "js"
                    displayName = "js"
                    analysisPlatform = "js"
                    dependentSourceSets = setOf(commonJ.value.sourceSetID)
                    sourceRoots = listOf("src/jsMain/kotlin/pageMerger/Test.kt")
                }
                sourceSet {
                    name = "jvm"
                    displayName = "jvm"
                    analysisPlatform = "jvm"
                    dependentSourceSets = setOf(commonJ.value.sourceSetID)
                    sourceRoots = listOf("src/jvmMain/kotlin/pageMerger/Test.kt")
                }
                sourceSet {
                    name = "linuxX64"
                    displayName = "linuxX64"
                    analysisPlatform = "native"
                    dependentSourceSets = setOf(commonN1.value.sourceSetID)
                    sourceRoots = listOf("src/linuxX64Main/kotlin/pageMerger/Test.kt")
                }
                sourceSet {
                    name = "mingwX64"
                    displayName = "mingwX64"
                    analysisPlatform = "native"
                    dependentSourceSets = setOf(commonN1.value.sourceSetID)
                    sourceRoots = listOf("src/mingwX64Main/kotlin/pageMerger/Test.kt")
                }
                sourceSet {
                    name = "iosArm64"
                    displayName = "iosArm64"
                    analysisPlatform = "native"
                    dependentSourceSets = setOf(commonN2.value.sourceSetID)
                    sourceRoots = listOf("src/iosArm64Main/kotlin/pageMerger/Test.kt")
                }
                sourceSet {
                    name = "iosX64"
                    displayName = "iosX64"
                    analysisPlatform = "native"
                    dependentSourceSets = setOf(commonN2.value.sourceSetID)
                    sourceRoots = listOf("src/iosX64Main/kotlin/pageMerger/Test.kt")
                }
            }
        }

        testInline(
            """
                |/src/commonMain/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |/src/commonJMain/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |expect class A
                |
                |/src/commonN1Main/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |expect class A
                |
                |/src/commonN2Main/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |expect class A
                |
                |/src/jsMain/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |actual class A
                |
                |/src/jvmMain/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |actual class A
                |
                |/src/linuxX64Main/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |actual class A
                |
                |/src/mingwX64Main/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |actual class A
                |
                |/src/iosArm64Main/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |actual class A
                |
                |/src/iosX64Main/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |actual class A
                |
            """.trimMargin(),
            configuration
        ) {
            pagesTransformationStage = {
                val allChildren = it.withDescendants().filterIsInstance<ClasslikePageNode>().toList()
                val commonJ = allChildren.filter { it.name == "[jvm, js]A" }
                val commonN1 = allChildren.filter { it.name == "[mingwX64, linuxX64]A" }
                val commonN2 = allChildren.filter { it.name == "[iosX64, iosArm64]A" }
                val noClass = allChildren.filter { it.name == "A" }
                assertEquals(1, commonJ.size, "There can be only one [jvm, js]A page")
                assertTrue(
                    commonJ.first().documentables.firstOrNull()?.sourceSets?.map { it.displayName }
                        ?.containsAll(listOf("commonJ", "js", "jvm")) ?: false,
                    "A(jvm, js)should have commonJ, js, jvm sources"
                )

                assertEquals(1, commonN1.size, "There can be only one [mingwX64, linuxX64]A page")
                assertTrue(
                    commonN1.first().documentables.firstOrNull()?.sourceSets?.map { it.displayName }
                        ?.containsAll(listOf("commonN1", "linuxX64", "mingwX64")) ?: false,
                    "[mingwX64, linuxX64]A should have commonN1, linuxX64, mingwX64 sources"
                )

                assertEquals(1, commonN2.size, "There can be only one [iosX64, iosArm64]A page")
                assertTrue(
                    commonN2.first().documentables.firstOrNull()?.sourceSets?.map { it.displayName }
                        ?.containsAll(listOf("commonN2", "iosArm64", "iosX64")) ?: false,
                    "[iosX64, iosArm64]A should have commonN2, iosArm64, iosX64 sources"
                )

                assertTrue(noClass.isEmpty(), "There can't be any A page")
            }
        }
    }
}
