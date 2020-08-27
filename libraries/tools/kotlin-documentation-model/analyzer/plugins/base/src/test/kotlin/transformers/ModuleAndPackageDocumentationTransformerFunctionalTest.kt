package transformers

import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import transformers.AbstractContextModuleAndPackageDocumentationReaderTest.Companion.texts
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
            
            # Package [root]
            This is also the root package
            
            # Package common
            This is the common package
            
            # Package jvm 
            This is the jvm package
            
            # Package js
            This is the js package
            """.trimIndent()
        )
        val configuration = dokkaConfiguration {
            moduleName = "moduleA"
            sourceSets {
                sourceSet {
                    name = "commonMain"
                    displayName = "common"
                    analysisPlatform = "common"
                    sourceRoots = listOf("src/commonMain/kotlin")
                    includes = listOf(include.canonicalPath)
                }
                sourceSet {
                    name = "jsMain"
                    displayName = "js"
                    analysisPlatform = "js"
                    sourceRoots = listOf("src/jsMain/kotlin")
                    dependentSourceSets = setOf(DokkaSourceSetID("moduleA", "commonMain"))
                    includes = listOf(include.canonicalPath)
                }
                sourceSet {
                    name = "jvmMain"
                    displayName = "jvm"
                    analysisPlatform = "jvm"
                    sourceRoots = listOf("src/jvmMain/kotlin")
                    dependentSourceSets = setOf(DokkaSourceSetID("moduleA", "commonMain"))
                    includes = listOf(include.canonicalPath)
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

                /* Assert module documentation */
                assertEquals(3, module.documentation.keys.size, "Expected all three source sets")
                assertEquals("This is moduleA", module.documentation.texts.distinct().joinToString())

                /* Assert root package */
                val rootPackage = module.packages.single { it.dri.packageName == "" }
                assertEquals(3, rootPackage.documentation.keys.size, "Expected all three source sets")
                assertEquals(
                    listOf("This is the root package", "This is also the root package"),
                    rootPackage.documentation.texts.distinct()
                )

                /* Assert common package */
                val commonPackage = module.packages.single { it.dri.packageName == "common" }
                assertEquals(3, commonPackage.documentation.keys.size, "Expected all three source sets")
                assertEquals("This is the common package", commonPackage.documentation.texts.distinct().joinToString())

                /* Assert js package */
                val jsPackage = module.packages.single { it.dri.packageName == "js" }
                assertEquals(
                    "This is the js package",
                    jsPackage.documentation.texts.joinToString()
                )

                /* Assert the jvm package */
                val jvmPackage = module.packages.single { it.dri.packageName == "jvm" }
                assertEquals(
                    "This is the jvm package",
                    jvmPackage.documentation.texts.joinToString()
                )
            }
        }
    }
}
