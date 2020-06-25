package expectActuals

import org.jetbrains.dokka.pages.ClasslikePageNode
import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue


class ExpectActualsTest : AbstractCoreTest() {

    fun PageNode.childrenRec(): List<PageNode> = listOf(this) + children.flatMap { it.childrenRec() }

    @Test
    fun `two same named expect actual classes`() {

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
                val commonN = sourceSet {
                    moduleName = "example"
                    name = "commonN"
                    displayName = "commonN"
                    analysisPlatform = "common"
                    sourceRoots = listOf("src/commonNMain/kotlin/pageMerger/Test.kt")
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
                    dependentSourceSets = setOf(commonN.sourceSetID)
                    sourceRoots = listOf("src/linuxX64Main/kotlin/pageMerger/Test.kt")
                }
                val mingwX64 = sourceSet {
                    moduleName = "example"
                    name = "mingwX64"
                    displayName = "mingwX64"
                    analysisPlatform = "native"
                    dependentSourceSets = setOf(commonN.sourceSetID)
                    sourceRoots = listOf("src/mingwX64Main/kotlin/pageMerger/Test.kt")
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
                |/src/commonNMain/kotlin/pageMerger/Test.kt
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
                |/src/linuxX64/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |actual class A
                |
                |/src/mingwX64Main/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |actual class A
                |
            """.trimMargin(),
            configuration
        ) {
            pagesTransformationStage = {
                println(it)
                val allChildren = it.childrenRec().filterIsInstance<ClasslikePageNode>()
                val jvmClass = allChildren.filter { it.name == "DoNotMerge(jvm)" }
                val jsClass = allChildren.filter { it.name == "DoNotMerge(js)" }
                val noClass = allChildren.filter { it.name == "DoNotMerge" }
                assertTrue(jvmClass.size == 1) { "There can be only one DoNotMerge(jvm) page" }
                assertTrue(jvmClass.first().documentable?.sourceSets?.single()?.analysisPlatform?.key == "jvm") { "DoNotMerge(jvm) should have only jvm sources" }

                assertTrue(jsClass.size == 1) { "There can be only one DoNotMerge(js) page" }
                assertTrue(jsClass.first().documentable?.sourceSets?.single()?.analysisPlatform?.key == "js") { "DoNotMerge(js) should have only js sources" }

                assertTrue(noClass.isEmpty()) { "There can't be any DoNotMerge page" }
            }
        }
    }
}