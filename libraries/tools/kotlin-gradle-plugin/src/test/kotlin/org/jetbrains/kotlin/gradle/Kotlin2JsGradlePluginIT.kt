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
                    ":libraryProject:jarSources\n",
                    ":mainProject:compileKotlin2Js\n",
                    ":libraryProject:compileKotlin2Js\n"
            )

            listOf("mainProject/web/js/app.js",
                    "mainProject/web/js/lib/kotlin.js",
                    "libraryProject/build/kotlin2js/main/test-library.js",
                    "mainProject/web/js/app.js.map"
            ).forEach { assertFileExists(it) }


            // TODO Should be updated to `new _.example.library.Counter` once namespaced imports from libraryFiles are implemented
            // TODO It would be better to test these by behavior instead of implementation, for example by loading the files
            //      into Rhino and running assertions on that. See https://github.com/abesto/kotlin/commit/120ec1bda3d95630189d4d33d0b2afb4253b5186
            //      for the (original) discussion on this.
            assertFileContains("libraryProject/build/kotlin2js/main/test-library.js", "Counter: Kotlin.createClass")
            assertFileContains("mainProject/web/js/app.js", "var counter = new \$module\$test_library.example.library.Counter(counterText);")
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
    fun testNoOutputFileFails() {
        val project = Project("kotlin2JsNoOutputFileProject", "2.10")
        project.build("build") {
            assertFailed()
            assertReportExists()
            assertContains("compileKotlin2Js.kotlinOptions.outputFile should be specified.")
        }
    }
}

