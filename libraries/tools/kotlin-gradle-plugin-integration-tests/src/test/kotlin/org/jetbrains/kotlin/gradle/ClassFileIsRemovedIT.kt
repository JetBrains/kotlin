package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.findFileByName
import org.jetbrains.kotlin.gradle.util.getFileByName
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Test
import java.io.File
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClassFileIsRemovedIT : BaseGradleIT() {
    @Test
    fun testClassIsRemovedNonIC() {
        doTestClassIsRemoved(defaultBuildOptions())
    }

    @Test
    fun testClassIsRemovedIC() {
        doTestClassIsRemoved(defaultBuildOptions().copy(incremental = true))
    }

    private fun doTestClassIsRemoved(buildOptions: BuildOptions) {
        doTest(buildOptions) { dummyFile ->
            assertTrue(dummyFile.delete(), "Could not delete $dummyFile")
        }
    }

    @Test
    fun testClassIsRenamedNonIC() {
        doTestClassIsRenamed(defaultBuildOptions())
    }

    @Test
    fun testClassIsRenamedIC() {
        doTestClassIsRenamed(defaultBuildOptions().copy(incremental = true))
    }

    fun doTestClassIsRenamed(buildOptions: BuildOptions) {
        doTest(buildOptions) { dummyFile ->
            dummyFile.modify { it.replace("Dummy", "ForDummies") }
        }
    }

    fun doTest(buildOptions: BuildOptions, transformDummy: (File)->Unit) {
        val project = Project("kotlinInJavaRoot", NoSpecificGradleVersion)
        project.build("build", options = buildOptions) {
            assertSuccessful()
        }

        val dummyFile = project.projectDir.getFileByName("Dummy.kt")
        transformDummy(dummyFile)

        project.build("build", options = buildOptions) {
            assertSuccessful()
            val dummyClassFile = project.projectDir.findFileByName("Dummy.class")
            assertNull(dummyClassFile, "$dummyClassFile should not exist!")
        }

        // check that class removal does not trigger rebuild
        project.build("build", options = buildOptions) {
            assertSuccessful()
            assertContains(":compileKotlin UP-TO-DATE", ":compileJava UP-TO-DATE")
        }
    }
}