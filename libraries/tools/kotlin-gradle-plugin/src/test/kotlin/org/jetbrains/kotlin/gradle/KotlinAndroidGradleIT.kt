package org.jetbrains.kotlin.gradle

import com.google.common.io.Files
import com.intellij.openapi.util.SystemInfo
import java.io.File
import java.util.Arrays
import java.util.Scanner
import org.junit.Before
import org.junit.After
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.fail
import org.gradle.testfixtures.ProjectBuilder

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
            assertTrue(buildOutput.contains(":compileDebugKotlin"), "Should contain ':compileDebugKotlin'")
            assertTrue(buildOutput.contains(":compileDebug"), "Should contain ':compileDebug'")
            assertTrue(buildOutput.contains(":compileReleaseKotlin"), "Should contain ':compileReleaseKotlin'")
            assertTrue(buildOutput.contains(":compileRelease"), "Should contain ':compileRelease'")

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
            assertTrue(up2dateBuildOutput.contains(":compileDebugKotlin UP-TO-DATE"), "Should contain ':compileDebugKotlin UP-TO-DATE'")
            assertTrue(up2dateBuildOutput.contains(":compileDebug UP-TO-DATE"), "Should contain ':compileDebug UP-TO-DATE'")
            assertTrue(up2dateBuildOutput.contains(":compileReleaseKotlin UP-TO-DATE"), "Should contain ':compileReleaseKotlin UP-TO-DATE'")
            assertTrue(up2dateBuildOutput.contains(":compileRelease UP-TO-DATE"), "Should contain ':compileRelease UP-TO-DATE'")
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