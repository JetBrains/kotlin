package expectActuals

import org.jetbrains.dokka.pages.ClasslikePageNode
import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue


class ExpectActualsTest : AbstractCoreTest() {

    fun PageNode.childrenRec(): List<PageNode> = listOf(this) + children.flatMap { it.childrenRec() }

    @Test
    fun `three same named expect actual classes`() {

        val configuration = dokkaConfiguration {
            sourceSets {
                val common = sourceSet {
                    moduleName = "example"
                    name = "common"
                    displayName = "common"
                    analysisPlatform = "common"
                    sourceRoots = listOf("src/commonMain/kotlin/pageMerger/Test.kt")
                }
                val commonJ = sourceSet {
                    moduleName = "example"
                    name = "commonJ"
                    displayName = "commonJ"
                    analysisPlatform = "common"
                    sourceRoots = listOf("src/commonJMain/kotlin/pageMerger/Test.kt")
                    dependentSourceSets = setOf(common.sourceSetID)
                }
                val commonN1 = sourceSet {
                    moduleName = "example"
                    name = "commonN1"
                    displayName = "commonN1"
                    analysisPlatform = "common"
                    sourceRoots = listOf("src/commonN1Main/kotlin/pageMerger/Test.kt")
                    dependentSourceSets = setOf(common.sourceSetID)
                }
                val commonN2 = sourceSet {
                    moduleName = "example"
                    name = "commonN2"
                    displayName = "commonN2"
                    analysisPlatform = "common"
                    sourceRoots = listOf("src/commonN2Main/kotlin/pageMerger/Test.kt")
                    dependentSourceSets = setOf(common.sourceSetID)
                }
                val js = sourceSet {
                    moduleName = "example"
                    name = "js"
                    displayName = "js"
                    analysisPlatform = "js"
                    dependentSourceSets = setOf(commonJ.sourceSetID)
                    sourceRoots = listOf("src/jsMain/kotlin/pageMerger/Test.kt")
                }
                val jvm = sourceSet {
                    moduleName = "example"
                    name = "jvm"
                    displayName = "jvm"
                    analysisPlatform = "jvm"
                    dependentSourceSets = setOf(commonJ.sourceSetID)
                    sourceRoots = listOf("src/jvmMain/kotlin/pageMerger/Test.kt")
                }
                val linuxX64 = sourceSet {
                    moduleName = "example"
                    name = "linuxX64"
                    displayName = "linuxX64"
                    analysisPlatform = "native"
                    dependentSourceSets = setOf(commonN1.sourceSetID)
                    sourceRoots = listOf("src/linuxX64Main/kotlin/pageMerger/Test.kt")
                }
                val mingwX64 = sourceSet {
                    moduleName = "example"
                    name = "mingwX64"
                    displayName = "mingwX64"
                    analysisPlatform = "native"
                    dependentSourceSets = setOf(commonN1.sourceSetID)
                    sourceRoots = listOf("src/mingwX64Main/kotlin/pageMerger/Test.kt")
                }
                val iosArm64 = sourceSet {
                    moduleName = "example"
                    name = "iosArm64"
                    displayName = "iosArm64"
                    analysisPlatform = "native"
                    dependentSourceSets = setOf(commonN2.sourceSetID)
                    sourceRoots = listOf("src/iosArm64Main/kotlin/pageMerger/Test.kt")
                }
                val iosX64 = sourceSet {
                    moduleName = "example"
                    name = "iosX64"
                    displayName = "iosX64"
                    analysisPlatform = "native"
                    dependentSourceSets = setOf(commonN2.sourceSetID)
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
                val allChildren = it.childrenRec().filterIsInstance<ClasslikePageNode>()
                val commonJ = allChildren.filter { it.name == "A(js, jvm)" }
                val commonN1 = allChildren.filter { it.name == "A(linuxX64, mingwX64)" }
                val commonN2 = allChildren.filter { it.name == "A(iosArm64, iosX64)" }
                val noClass = allChildren.filter { it.name == "A" }
                assertTrue(commonJ.size == 1) { "There can be only one A(js, jvm) page" }
                assertTrue(
                    commonJ.first().documentable?.sourceSets?.map { it.displayName }
                        ?.containsAll(listOf("commonJ", "js", "jvm")) ?: false
                ) { "A(js, jvm)should have commonJ, js, jvm sources" }

                assertTrue(commonN1.size == 1) { "There can be only one A(linuxX64, mingwX64) page" }
                assertTrue(
                    commonN1.first().documentable?.sourceSets?.map { it.displayName }
                        ?.containsAll(listOf("commonN1", "linuxX64", "mingwX64")) ?: false
                ) { "A(linuxX64, mingwX64) should have commonN1, linuxX64, mingwX64 sources" }

                assertTrue(commonN2.size == 1) { "There can be only one A(iosArm64, iosX64) page" }
                assertTrue(
                    commonN2.first().documentable?.sourceSets?.map { it.displayName }
                        ?.containsAll(listOf("commonN2", "iosArm64", "iosX64")) ?: false
                ) { "A(iosArm64, iosX64) should have commonN2, iosArm64, iosX64 sources" }

                assertTrue(noClass.isEmpty()) { "There can't be any A page" }
            }
        }
    }
}