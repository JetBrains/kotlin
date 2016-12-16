package org.jetbrains.kotlin.gradle

import org.junit.Test

class Kotlin2JsGradlePluginIT : BaseGradleIT() {
    @Test
    fun testBuildAndClean() {
        val project = Project("kotlin2JsProject", "2.10")

        project.build("build") {
            assertSuccessful()
            assertReportExists()

            assertContains(
                    ":libraryProject:jarSources",
                    ":mainProject:compileKotlin2Js",
                    ":libraryProject:compileKotlin2Js"
            )

            listOf("mainProject/web/js/app.js",
                    "mainProject/web/js/lib/kotlin.js",
                    "libraryProject/build/kotlin2js/main/test-library.js",
                    "mainProject/web/js/app.js.map"
            ).forEach { assertFileExists(it) }
        }

        project.build("build") {
            assertSuccessful()
            assertContains(
                    ":mainProject:compileKotlin2Js UP-TO-DATE",
                    ":libraryProject:compileTestKotlin2Js UP-TO-DATE"
            )
        }

        project.build("clean") {
            assertSuccessful()
            assertReportExists()
            assertContains(":mainProject:cleanCompileKotlin2Js\n")
            assertNoSuchFile("mainProject/web/js/app.js")

            // Test that we don't accidentally remove the containing directory
            // This would fail if we used the default clean task of the copy task
            assertFileExists("mainProject/web/js/lib")

            assertNoSuchFile("main/project/web/js/app.js.map")
            assertNoSuchFile("main/project/web/js/example/main.kt")
        }

        project.build("clean") {
            assertSuccessful()
            assertReportExists()
            assertContains(":mainProject:cleanCompileKotlin2Js UP-TO-DATE")
        }
    }

    @Test
    fun testModuleKind() {
        val project = Project("kotlin2JsModuleKind", "2.10")

        project.build("runRhino") {
            assertSuccessful()
        }
    }

    @Test
    fun testDefaultOutputFile() {
        val project = Project("kotlin2JsNoOutputFileProject", "2.10")

        project.build("build") {
            assertSuccessful()
            assertFileExists("build/classes/main/kotlin2JsNoOutputFileProject_main.js")
            assertFileExists("build/classes/test/kotlin2JsNoOutputFileProject_test.js")
        }
    }

    @Test
    fun testCompileTestCouldAccessProduction() {
        val project = Project("kotlin2JsProjectWithTests", "2.10")

        project.build("build") {
            assertSuccessful()

            assertContains(
                    ":compileKotlin2Js",
                    ":compileTestKotlin2Js"
            )

            assertFileExists("build/kotlin2js/main/module.js")
            assertFileExists("build/kotlin2js/test/module-tests.js")
        }
    }
}
