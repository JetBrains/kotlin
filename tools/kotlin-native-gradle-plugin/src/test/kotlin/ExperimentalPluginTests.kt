package org.jetbrains.kotlin.gradle.plugin.test

import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.test.*

class ExperimentalPluginTests {

    val tmpFolder = TemporaryFolder()
        @Rule get

    val projectDirectory: File
        get() = tmpFolder.root

    val exeSuffix = HostManager.host.family.exeSuffix

    @Test
    fun `Plugin should compile one executable`() {
        val project = KonanProject.create(projectDirectory).apply {
            settingsFile.appendText("\nrootProject.name = 'test'")
            buildFile.writeText("""
                plugins { id 'kotlin-native' }

                sourceSets.main.component {
                    outputKinds = [ EXECUTABLE, KLIBRARY ]
                }
            """.trimIndent())
        }
        val assembleResult = project.createRunner().withArguments("assemble").build()

        assertEquals(TaskOutcome.SUCCESS, assembleResult.task(":compileDebugKotlinNative")?.outcome)
        assertTrue(projectDirectory.resolve("build/exe/main/debug/test.$exeSuffix").exists())
    }

    @Test
    fun `Plugin should build a klibrary and support a project dependency on it`() {
        val libraryDir = tmpFolder.newFolder("library")
        val libraryProject = KonanProject.createEmpty(libraryDir).apply {
            buildFile.writeText("""
                plugins { id 'kotlin-native' }
            """.trimIndent())
            generateSrcFile("library.kt", "fun foo() = 42")
        }

        val project = KonanProject.createEmpty(projectDirectory).apply {
            settingsFile.writeText("""
                include ':library'
                rootProject.name = 'test'
            """.trimIndent())
            buildFile.writeText("""
                plugins { id 'kotlin-native' }

                dependencies {
                    implementation project(':library')
                }

                sourceSets.main.component {
                    outputKinds = [ EXECUTABLE ]
                }
            """.trimIndent())
            generateSrcFile("main.kt", "fun main(args: Array<String>) { println(foo()) }")
        }

        val compileDebugResult = project.createRunner().withArguments("compileDebugKotlinNative").build()
        assertEquals(TaskOutcome.SUCCESS, compileDebugResult.task(":compileDebugKotlinNative")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, compileDebugResult.task(":library:compileDebugKotlinNative")?.outcome)
        assertTrue(projectDirectory.resolve("build/exe/main/debug/test.$exeSuffix").exists())
        assertTrue(libraryDir.resolve("build/lib/main/debug/library.klib").exists())

        val compileReleaseResult = project.createRunner().withArguments("compileReleaseKotlinNative").build()
        assertEquals(TaskOutcome.SUCCESS, compileReleaseResult.task(":compileReleaseKotlinNative")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, compileReleaseResult.task(":library:compileReleaseKotlinNative")?.outcome)
        assertTrue(projectDirectory.resolve("build/exe/main/release/test.$exeSuffix").exists())
        assertTrue(libraryDir.resolve("build/lib/main/release/library.klib").exists())
    }

    @Test
    fun `Plugin should be able to publish a component and support a maven dependency on it`() {
        val libraryDir = tmpFolder.newFolder("library")
        val libraryProject = KonanProject.createEmpty(libraryDir).apply {
            buildFile.writeText("""
                plugins {
                    id 'kotlin-native'
                    id 'maven-publish'
                }

                group 'test'
                version '1.0'

                sourceSets.main.component {
                    target 'host', 'wasm32'
                }

                publishing {
                    repositories {
                        maven {
                            url = '../repo'
                        }
                    }
                }
            """.trimIndent())
            generateSrcFile("library.kt", "fun foo() = 42")
        }

        val project = KonanProject.createEmpty(projectDirectory).apply {
            settingsFile.writeText("""
                enableFeaturePreview('GRADLE_METADATA')
                include ':library'
                rootProject.name = 'test'
            """.trimIndent())
            buildFile.writeText("""
                plugins { id 'kotlin-native' }

                repositories {
                    maven {
                        url = 'repo'
                    }
                }

                dependencies {
                    implementation 'test:library:1.0'
                }

                sourceSets.main.component {
                    target 'host', 'wasm32'
                    outputKinds = [ EXECUTABLE ]
                }
            """.trimIndent())
            generateSrcFile("main.kt", "fun main(args: Array<String>) { println(foo()) }")
        }

        val publishResult = project.createRunner().withArguments("library:publish").build()
        assertEquals(TaskOutcome.SUCCESS, publishResult.task(":library:compileDebugWasm32KotlinNative")?.outcome)
        assertEquals(TaskOutcome.SUCCESS,
               publishResult.task(":library:compileDebug${HostManager.hostName.capitalize()}KotlinNative")?.outcome)

        project.createRunner().withArguments(":assemble").build()
        project.createRunner().withArguments(":compileDebugWasm32KotlinNative").build()
        val wasm32ExeSuffix = HostManager().targetByName("wasm32").family.exeSuffix

        assertTrue(projectDirectory.resolve("build/exe/main/debug/${HostManager.hostName}/test.$exeSuffix").exists())
        assertTrue(projectDirectory.resolve("build/exe/main/debug/wasm32/test.$wasm32ExeSuffix").exists())
    }

    @Test
    fun `Plugin should be able to build a component for different targets with target-specific sources`() {
        val project = KonanProject.createEmpty(projectDirectory).apply {
            settingsFile.appendText("\nrootProject.name = 'test'")
            buildFile.writeText("""
                plugins { id 'kotlin-native' }

                sourceSets.main {
                    target('macos_x64').srcDir 'src/main/macos_x64'
                    target('linux_x64').srcDir 'src/main/linux_x64'
                    target('mingw_x64').srcDir 'src/main/mingw_x64'
                    component {
                        outputKinds = [ EXECUTABLE ]
                    }
                }
            """.trimIndent())
            generateSrcFile("main.kt", """
                fun main(args: Array<String>) {
                    print(foo())
                }

                expect fun foo(): String
            """.trimIndent())
            listOf(KonanTarget.MACOS_X64, KonanTarget.LINUX_X64, KonanTarget.MINGW_X64).forEach {
                val target = it.name
                generateSrcFile(Paths.get("src/main/$target"),"foo.kt", "actual fun foo() = \"$target\"")
            }
        }
        project.createRunner().withArguments("assemble").build()

        val process = ProcessBuilder(
                projectDirectory.resolve("build/exe/main/debug/${HostManager.hostName}/test.$exeSuffix").absolutePath
        ).start()
        process.waitFor(10, TimeUnit.SECONDS)

        assertEquals(HostManager.hostName, process.inputStream.reader().readText())
    }

    @Test
    fun `Plugin should support transitive project klib dependencies`() {
        val fooDir = tmpFolder.newFolder("foo")
        val barDir = tmpFolder.newFolder("bar")
        val foo = KonanProject.createEmpty(fooDir).apply {
            buildFile.writeText("""
                plugins { id 'kotlin-native' }

                sourceSets.main.component {
                    target 'host', 'wasm32'
                }

                dependencies {
                    implementation project(':bar')
                }
            """.trimIndent())
            generateSrcFile("foo.kt", "fun foo() = bar()")
        }

        val bar = KonanProject.createEmpty(barDir).apply {
            buildFile.writeText("""
                plugins { id 'kotlin-native'}

                sourceSets.main.component {
                    target 'host', 'wasm32'
                }

            """.trimIndent())
            generateSrcFile("bar.kt", "fun bar() = \"Bar\"")
        }

        val project = KonanProject.createEmpty(projectDirectory).apply {
            settingsFile.writeText("""
                include ':foo'
                include ':bar'
            """.trimIndent())
            buildFile.writeText("""
                plugins { id 'kotlin-native' }

                sourceSets.main.component {
                    target 'host', 'wasm32'
                    outputKinds = [ EXECUTABLE ]
                }

                dependencies {
                    implementation project('foo')
                }
            """.trimIndent())
            generateSrcFile("main.kt", "fun main(args: Array<String>) { println(foo()) }")
        }

        project.createRunner().withArguments(
                ":assembleReleaseWasm32",
                ":assembleRelease${HostManager.hostName.capitalize()}"
        ).build()
    }

    @Test
    fun `Project should support transitive maven klib dependencies`() {
        val fooDir = tmpFolder.newFolder("foo")
        val barDir = tmpFolder.newFolder("bar")
        val foo = KonanProject.createEmpty(fooDir).apply {
            buildFile.writeText("""
                plugins {
                    id 'kotlin-native'
                    id 'maven-publish'
                }

                repositories {
                    maven {
                        url = '../repo'
                    }
                }

                group 'test'
                version '1.0'

                sourceSets.main.component {
                    target 'host', 'wasm32'
                }

                dependencies {
                    implementation 'test:bar:1.0'
                }

                publishing {
                    repositories {
                        maven {
                            url = '../repo'
                        }
                    }
                }
            """.trimIndent())
            generateSrcFile("foo.kt", "fun foo() = bar()")
        }

        val bar = KonanProject.createEmpty(barDir).apply {
            buildFile.writeText("""
                plugins {
                    id 'kotlin-native'
                    id 'maven-publish'
                }

                group 'test'
                version '1.0'

                sourceSets.main.component {
                    target 'host', 'wasm32'
                }

                publishing {
                    repositories {
                        maven {
                            url = '../repo'
                        }
                    }
                }
            """.trimIndent())
            generateSrcFile("bar.kt", "fun bar() = \"Bar\"")
        }

        val project = KonanProject.createEmpty(projectDirectory).apply {
            settingsFile.writeText("""
                enableFeaturePreview('GRADLE_METADATA')
                include ':foo'
                include ':bar'
                rootProject.name = 'test'
            """.trimIndent())
            buildFile.writeText("""
                plugins { id 'kotlin-native' }

                repositories {
                    maven {
                        url = 'repo'
                    }
                }

                dependencies {
                    implementation 'test:foo:1.0'
                }

                sourceSets.main.component {
                    target 'host', 'wasm32'
                    outputKinds = [ EXECUTABLE ]
                }
            """.trimIndent())
            generateSrcFile("main.kt", "fun main(args: Array<String>) { println(foo()) }")
        }

        project.createRunner().withArguments(":bar:publish").build()
        project.createRunner().withArguments(":foo:publish").build()
        project.createRunner().withArguments(
                ":assembleReleaseWasm32",
                ":assembleRelease${HostManager.hostName.capitalize()}"
        ).build()
    }

    @Test
    @Ignore
    fun `Plugin should allow creating components by creating source sets`() {
        val project = KonanProject.create(projectDirectory).apply {
            settingsFile.appendText("\nrootProject.name = 'test'")
            buildFile.writeText("""
                plugins { id 'kotlin-native' }

                sourceSets {
                    foo {
                        kotlin.srcDir 'src/foo/kotlin'
                        component {
                            outputKinds = [ EXECUTABLE ]
                        }
                    }
                }
            """.trimIndent())
            generateSrcFile(
                    Paths.get("src/foo/kotlin"),
                    "foo.kt",
                    "fun main(args: Array<String>) { println(\"Foo\") }"
            )
        }
        val assembleResult = project.createRunner().withArguments("assembleFooDebug", "assembleFooRelease").build()

        assertEquals(TaskOutcome.SUCCESS, assembleResult.task(":compileFooDebugKotlinNative")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, assembleResult.task(":compileFooReleaseKotlinNative")?.outcome)
        assertTrue(projectDirectory.resolve("build/exe/foo/debug/foo.$exeSuffix").exists())
        assertTrue(projectDirectory.resolve("build/exe/foo/release/foo.$exeSuffix").exists())
    }

}