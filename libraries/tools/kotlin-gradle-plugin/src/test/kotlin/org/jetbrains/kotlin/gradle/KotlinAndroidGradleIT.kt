package org.jetbrains.kotlin.gradle

import com.google.common.io.Files
import com.intellij.openapi.util.SystemInfo
import java.io.File
import java.util.Arrays
import java.util.Scanner
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.Ignore
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.fail
import org.gradle.testfixtures.ProjectBuilder

Ignore("Requires Android SDK")
class KotlinAndroidGradleIT {

    var workingDir: File = File(".")

        Before fun setUp() {
            workingDir = Files.createTempDir()
            workingDir.mkdirs()
            copyRecursively(File("src/test/resources/testProject/AndroidAlfaProject"), workingDir)
        }


        After fun tearDown() {
            deleteRecursively(workingDir)
        }

        Test fun testSimpleCompile() {
            val projectDir = File(workingDir, "AndroidAlfaProject")

            val pathToKotlinPlugin = "-PpathToKotlinPlugin=" + File("local-repo").getAbsolutePath()
            val cmd = if (SystemInfo.isWindows)
                listOf("cmd", "/C", "gradlew.bat", "build", pathToKotlinPlugin, "--no-daemon", "--debug")
            else
                listOf("/bin/bash", "./gradlew", "build", pathToKotlinPlugin, "--no-daemon", "--debug")

            val builder = ProcessBuilder(cmd)
            builder.directory(projectDir)
            builder.redirectErrorStream(true)
            val process = builder.start()

            val s = Scanner(process.getInputStream()!!)
            val text = StringBuilder()
            while (s.hasNextLine()) {
                text append s.nextLine()
                text append "\n"
            }
            s.close()

            val result = process.waitFor()
            val buildOutput = text.toString()

            println(buildOutput)

            assertEquals(result, 0)
            assertTrue(buildOutput.contains(":compileFlavor1DebugKotlin"), "Should contain ':compileFlavor1DebugKotlin'")
            assertTrue(buildOutput.contains(":compileFlavor2DebugKotlin"), "Should contain ':compileFlavor2DebugKotlin'")
            assertTrue(buildOutput.contains(":compileFlavor1JnidebugKotlin"), "Should contain ':compileFlavor1JnidebugKotlin'")
            assertTrue(buildOutput.contains(":compileFlavor1ReleaseKotlin"), "Should contain ':compileFlavor1ReleaseKotlin'")
            assertTrue(buildOutput.contains(":compileFlavor2JnidebugKotlin"), "Should contain ':compileFlavor2JnidebugKotlin'")
            assertTrue(buildOutput.contains(":compileFlavor2ReleaseKotlin"), "Should contain ':compileFlavor2ReleaseKotlin'")
            assertTrue(buildOutput.contains(":compileFlavor1Debug"), "Should contain ':compileFlavor1Debug'")
            assertTrue(buildOutput.contains(":compileFlavor2Debug"), "Should contain ':compileFlavor2Debug'")
            assertTrue(buildOutput.contains(":compileFlavor1Jnidebug"), "Should contain ':compileFlavor1Jnidebug'")
            assertTrue(buildOutput.contains(":compileFlavor2Jnidebug"), "Should contain ':compileFlavor2Jnidebug'")
            assertTrue(buildOutput.contains(":compileFlavor1Release"), "Should contain ':compileFlavor1Release'")
            assertTrue(buildOutput.contains(":compileFlavor2Release"), "Should contain ':compileFlavor2Release'")

            // Run the build second time, assert everything is up-to-date

            val up2dateBuilder = ProcessBuilder(cmd)
            up2dateBuilder.directory(projectDir)
            up2dateBuilder.redirectErrorStream(true)
            val up2dateProcess = up2dateBuilder.start()

            val up2dateProcessScanner = Scanner(up2dateProcess.getInputStream()!!)
            val up2dateText = StringBuilder()
            while (up2dateProcessScanner.hasNextLine()) {
                up2dateText append up2dateProcessScanner.nextLine()
                up2dateText append "\n"
            }
            up2dateProcessScanner.close()

            val up2dateResult = up2dateProcess.waitFor()
            val up2dateBuildOutput = up2dateText.toString()

            println(up2dateBuildOutput)

            assertEquals(up2dateResult, 0)
            assertTrue(up2dateBuildOutput.contains(":compileFlavor1DebugKotlin UP-TO-DATE"), "Should contain ':compileFlavor1DebugKotlin UP-TO-DATE'")
            assertTrue(up2dateBuildOutput.contains(":compileFlavor2DebugKotlin UP-TO-DATE"), "Should contain ':compileFlavor2DebugKotlin UP-TO-DATE'")
            assertTrue(up2dateBuildOutput.contains(":compileFlavor1JnidebugKotlin UP-TO-DATE"), "Should contain ':compileFlavor1JnidebugKotlin' UP-TO-DATE")
            assertTrue(up2dateBuildOutput.contains(":compileFlavor1ReleaseKotlin UP-TO-DATE"), "Should contain ':compileFlavor1ReleaseKotlin UP-TO-DATE'")
            assertTrue(up2dateBuildOutput.contains(":compileFlavor2JnidebugKotlin UP-TO-DATE"), "Should contain ':compileFlavor2JnidebugKotlin UP-TO-DATE'")
            assertTrue(up2dateBuildOutput.contains(":compileFlavor2ReleaseKotlin UP-TO-DATE"), "Should contain ':compileFlavor2ReleaseKotlin UP-TO-DATE'")
            assertTrue(up2dateBuildOutput.contains(":compileFlavor1Debug UP-TO-DATE"), "Should contain ':compileFlavor1Debug UP-TO-DATE'")
            assertTrue(up2dateBuildOutput.contains(":compileFlavor2Debug UP-TO-DATE"), "Should contain ':compileFlavor2Debug UP-TO-DATE'")
            assertTrue(up2dateBuildOutput.contains(":compileFlavor1Jnidebug UP-TO-DATE"), "Should contain ':compileFlavor1Jnidebug UP-TO-DATE'")
            assertTrue(up2dateBuildOutput.contains(":compileFlavor2Jnidebug UP-TO-DATE"), "Should contain ':compileFlavor2Jnidebug UP-TO-DATE'")
            assertTrue(up2dateBuildOutput.contains(":compileFlavor1Release UP-TO-DATE"), "Should contain ':compileFlavor1Release UP-TO-DATE'")
            assertTrue(up2dateBuildOutput.contains(":compileFlavor2Release UP-TO-DATE"), "Should contain ':compileFlavor2Release UP-TO-DATE'")

            // Run the build third time, re-run tasks

            val rebuilder = ProcessBuilder(cmd + "--rerun-tasks")
            rebuilder.directory(projectDir)
            rebuilder.redirectErrorStream(true)
            val reprocess = rebuilder.start()

            val rescanner = Scanner(reprocess.getInputStream()!!)
            val retext = StringBuilder()
            while (rescanner.hasNextLine()) {
                retext append rescanner.nextLine()
                retext append "\n"
            }
            rescanner.close()

            val reresult = reprocess.waitFor()
            val rebuildOutput = retext.toString()

            println(rebuildOutput)

            assertEquals(reresult, 0)
            assertTrue(rebuildOutput.contains(":compileFlavor1DebugKotlin"), "Should contain ':compileFlavor1DebugKotlin'")
            assertTrue(rebuildOutput.contains(":compileFlavor2DebugKotlin"), "Should contain ':compileFlavor2DebugKotlin'")
            assertTrue(rebuildOutput.contains(":compileFlavor1JnidebugKotlin"), "Should contain ':compileFlavor1JnidebugKotlin'")
            assertTrue(rebuildOutput.contains(":compileFlavor1ReleaseKotlin"), "Should contain ':compileFlavor1ReleaseKotlin'")
            assertTrue(rebuildOutput.contains(":compileFlavor2JnidebugKotlin"), "Should contain ':compileFlavor2JnidebugKotlin'")
            assertTrue(rebuildOutput.contains(":compileFlavor2ReleaseKotlin"), "Should contain ':compileFlavor2ReleaseKotlin'")
            assertTrue(rebuildOutput.contains(":compileFlavor1Debug"), "Should contain ':compileFlavor1Debug'")
            assertTrue(rebuildOutput.contains(":compileFlavor2Debug"), "Should contain ':compileFlavor2Debug'")
            assertTrue(rebuildOutput.contains(":compileFlavor1Jnidebug"), "Should contain ':compileFlavor1Jnidebug'")
            assertTrue(rebuildOutput.contains(":compileFlavor2Jnidebug"), "Should contain ':compileFlavor2Jnidebug'")
            assertTrue(rebuildOutput.contains(":compileFlavor1Release"), "Should contain ':compileFlavor1Release'")
            assertTrue(rebuildOutput.contains(":compileFlavor2Release"), "Should contain ':compileFlavor2Release'")


        }


        fun copyRecursively(source: File, target: File) {
            assertTrue(target.isDirectory())
            val targetFile = File(target, source.getName())
            if (source.isDirectory()) {
                targetFile.mkdir()
                val array = source.listFiles()
                if (array != null) {
                    for (child in array) {
                        copyRecursively(child, targetFile)
                    }
                }
            } else {
                Files.copy(source, targetFile)
            }
        }


        fun deleteRecursively(f: File): Unit {
            if (f.isDirectory()) {
                val children = f.listFiles()
                if (children != null) {
                    for (child in children) {
                        deleteRecursively(child)
                    }
                }
                val shouldBeEmpty = f.listFiles()
                if (shouldBeEmpty != null) {
                    assertTrue(shouldBeEmpty.isEmpty())
                } else {
                    fail("Error listing directory content")
                }
            }
            f.delete()
        }
}