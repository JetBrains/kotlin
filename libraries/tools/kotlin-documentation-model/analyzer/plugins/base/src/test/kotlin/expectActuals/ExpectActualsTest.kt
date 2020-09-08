package expectActuals

import org.jetbrains.dokka.model.withDescendants
import org.jetbrains.dokka.pages.ClasslikePageNode
import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue


class ExpectActualsTest : AbstractCoreTest() {

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
                val js = sourceSet {
                    name = "js"
                    displayName = "js"
                    analysisPlatform = "js"
                    dependentSourceSets = setOf(commonJ.value.sourceSetID)
                    sourceRoots = listOf("src/jsMain/kotlin/pageMerger/Test.kt")
                }
                val jvm = sourceSet {
                    name = "jvm"
                    displayName = "jvm"
                    analysisPlatform = "jvm"
                    dependentSourceSets = setOf(commonJ.value.sourceSetID)
                    sourceRoots = listOf("src/jvmMain/kotlin/pageMerger/Test.kt")
                }
                val linuxX64 = sourceSet {
                    name = "linuxX64"
                    displayName = "linuxX64"
                    analysisPlatform = "native"
                    dependentSourceSets = setOf(commonN1.value.sourceSetID)
                    sourceRoots = listOf("src/linuxX64Main/kotlin/pageMerger/Test.kt")
                }
                val mingwX64 = sourceSet {
                    name = "mingwX64"
                    displayName = "mingwX64"
                    analysisPlatform = "native"
                    dependentSourceSets = setOf(commonN1.value.sourceSetID)
                    sourceRoots = listOf("src/mingwX64Main/kotlin/pageMerger/Test.kt")
                }
                val iosArm64 = sourceSet {
                    name = "iosArm64"
                    displayName = "iosArm64"
                    analysisPlatform = "native"
                    dependentSourceSets = setOf(commonN2.value.sourceSetID)
                    sourceRoots = listOf("src/iosArm64Main/kotlin/pageMerger/Test.kt")
                }
                val iosX64 = sourceSet {
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
                val commonJ = allChildren.filter { it.name == "A(jvm, js)" }
                val commonN1 = allChildren.filter { it.name == "A(mingwX64, linuxX64)" }
                val commonN2 = allChildren.filter { it.name == "A(iosX64, iosArm64)" }
                val noClass = allChildren.filter { it.name == "A" }
                assertTrue(commonJ.size == 1) { "There can be only one A(jvm, js) page" }
                assertTrue(
                    commonJ.first().documentable?.sourceSets?.map { it.displayName }
                        ?.containsAll(listOf("commonJ", "js", "jvm")) ?: false
                ) { "A(jvm, js)should have commonJ, js, jvm sources" }

                assertTrue(commonN1.size == 1) { "There can be only one A(mingwX64, linuxX64) page" }
                assertTrue(
                    commonN1.first().documentable?.sourceSets?.map { it.displayName }
                        ?.containsAll(listOf("commonN1", "linuxX64", "mingwX64")) ?: false
                ) { "A(mingwX64, linuxX64) should have commonN1, linuxX64, mingwX64 sources" }

                assertTrue(commonN2.size == 1) { "There can be only one A(iosX64, iosArm64) page" }
                assertTrue(
                    commonN2.first().documentable?.sourceSets?.map { it.displayName }
                        ?.containsAll(listOf("commonN2", "iosArm64", "iosX64")) ?: false
                ) { "A(iosX64, iosArm64) should have commonN2, iosArm64, iosX64 sources" }

                assertTrue(noClass.isEmpty()) { "There can't be any A page" }
            }
        }
    }
}
