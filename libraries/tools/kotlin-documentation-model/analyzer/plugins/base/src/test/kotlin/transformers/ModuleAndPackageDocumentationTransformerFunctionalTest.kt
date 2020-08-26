package transformers

import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals

class ModuleAndPackageDocumentationTransformerFunctionalTest : AbstractCoreTest() {

    @Test
    fun `multiplatform project`(@TempDir tempDir: Path) {
        val include = tempDir.resolve("include.md").toFile()
        include.writeText(
            """
            # Module moduleA
            This is moduleA
            
            # Package
            This is the root package
            
            # Package common
            This is the common package
            
            # Package jvm 
            This is the jvm package
            
            # Package js
            This is the js package
            """.trimIndent()
        )
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    moduleName = "moduleA"
                    name = "commonMain"
                    displayName = "common"
                    analysisPlatform = "common"
                    sourceRoots = listOf("src/commonMain/kotlin")
                    includes = listOf(include.canonicalPath)
                }
                sourceSet {
                    moduleName = "moduleA"
                    name = "jsMain"
                    displayName = "js"
                    analysisPlatform = "js"
                    sourceRoots = listOf("src/jsMain/kotlin")
                    dependentSourceSets = setOf(DokkaSourceSetID("moduleA", "commonMain"))
                }
                sourceSet {
                    moduleName = "moduleA"
                    name = "jvmMain"
                    displayName = "jvm"
                    analysisPlatform = "jvm"
                    sourceRoots = listOf("src/jvmMain/kotlin")
                    dependentSourceSets = setOf(DokkaSourceSetID("moduleA", "commonMain"))
                }
            }
        }

        testInline(
            """
            /src/commonMain/kotlin/common/CommonApi.kt
            package common
            val commonApi = "common"
            
            /src/jsMain/kotlin/js/JsApi.kt
            package js
            val jsApi = "js"
            
            /src/jvmMain/kotlin/jvm/JvmApi.kt
            package jvm
            val jvmApi = "jvm"
            
            /src/commonMain/kotlin/CommonRoot.kt
            val commonRoot = "commonRoot"
            
            /src/jsMain/kotlin/JsRoot.kt
            val jsRoot = "jsRoot"
            
            /src/jvmMain/kotlin/JvmRoot.kt
            val jvmRoot = "jvmRoot"
            """.trimIndent(),
            configuration
        ) {
            this.documentablesMergingStage = { module ->
                val packageNames = module.packages.map { it.dri.packageName ?: "NULL" }
                assertEquals(
                    listOf("", "common", "js", "jvm").sorted(), packageNames.sorted(),
                    "Expected all packages to be present"
                )
            }
        }
    }
}
