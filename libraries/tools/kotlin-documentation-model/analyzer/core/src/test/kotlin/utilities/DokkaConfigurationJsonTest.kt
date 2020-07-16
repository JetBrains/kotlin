package utilities

import org.jetbrains.dokka.*
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class DokkaConfigurationJsonTest {
    @Test
    fun `simple configuration toJsonString then parseJson`() {
        val configuration = DokkaConfigurationImpl(
            outputDir = "customOutputDir",
            pluginsClasspath = listOf(File("plugins/customPlugin.jar")),
            sourceSets = listOf(
                DokkaSourceSetImpl(
                    moduleDisplayName = "customModuleDisplayName",
                    sourceRoots = listOf(SourceRootImpl("customSourceRoot")),
                    sourceSetID = DokkaSourceSetID("customModuleName", "customSourceSetName")
                )
            )
        )

        val jsonString = configuration.toJsonString()
        val parsedConfiguration = DokkaConfigurationImpl(jsonString)
        assertEquals(configuration, parsedConfiguration)
    }

    @Test
    fun `parse simple configuration json`() {
        val json = """
            {
              "outputDir": "customOutputDir",
              "pluginsClasspath": [ "plugins/customPlugin.jar" ],
              "sourceSets": [
                {
                  "moduleDisplayName": "customModuleDisplayName",
                  "sourceSetID": {
                    "moduleName": "customModuleName",
                    "sourceSetName": "customSourceSetName"
                  },
                  "classpath": [],
                  "sourceRoots": [
                    {
                      "path": "customSourceRoot"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val parsedConfiguration = DokkaConfigurationImpl(json)
        assertEquals(
            DokkaConfigurationImpl(
                outputDir = "customOutputDir",
                pluginsClasspath = listOf(File("plugins/customPlugin.jar")),
                sourceSets = listOf(
                    DokkaSourceSetImpl(
                        moduleDisplayName = "customModuleDisplayName",
                        sourceRoots = listOf(SourceRootImpl("customSourceRoot")),
                        sourceSetID = DokkaSourceSetID("customModuleName", "customSourceSetName")
                    )
                )
            ),
            parsedConfiguration
        )
    }
}
