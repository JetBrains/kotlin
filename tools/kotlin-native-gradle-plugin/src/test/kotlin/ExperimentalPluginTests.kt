package org.jetbrains.kotlin.gradle.plugin.test

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.kotlin.gradle.plugin.experimental.internal.KotlinNativeMainComponent
import org.jetbrains.kotlin.gradle.plugin.experimental.internal.OutputKind
import org.jetbrains.kotlin.gradle.plugin.experimental.plugins.KotlinNativePlugin
import org.jetbrains.kotlin.gradle.plugin.experimental.plugins.kotlinNativeSourceSets
import org.jetbrains.kotlin.gradle.plugin.experimental.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.plugin.model.KonanToolingModelBuilder
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.Assume.assumeTrue
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

    private fun withProject(
        name: String = "testProject",
        plugins: Collection<Class<out Plugin<out Project>>> = listOf(KotlinNativePlugin::class.java),
        parent: Project? = null,
        block: ProjectInternal.() -> Unit
    ) {
        val builder = ProjectBuilder.builder().withProjectDir(projectDirectory).withName(name)
        parent?.let { builder.withParent(it) }
        val project = builder.build() as ProjectInternal
        plugins.forEach {
            project.pluginManager.apply(it)
        }
        project.block()
    }

    private fun assertFileExists(path: String, message: String = "No such file: $path")
        = assertTrue(projectDirectory.resolve(path).exists(), message)

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

        assertEquals(TaskOutcome.SUCCESS, assembleResult.task(":compileDebugExecutableKotlinNative")?.outcome)
        assertTrue(projectDirectory.resolve("build/exe/main/debug/executable/test.$exeSuffix").exists())
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
        assertEquals(TaskOutcome.UP_TO_DATE, compileReleaseResult.task(":library:compileDebugKotlinNative")?.outcome)
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
                    targets = ['host', 'wasm32']
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
                    targets = ['host', 'wasm32']
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
                    targets = ['host', 'wasm32']
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
                    targets = ['host', 'wasm32']
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
                    targets = ['host', 'wasm32']
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
                    targets = ['host', 'wasm32']
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
                    id 'signing'
                }

                group 'test'
                version '1.0'

                sourceSets.main.component {
                    targets = ['host', 'wasm32']
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
                    targets = ['host', 'wasm32']
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

    @Test
    fun `Plugin should not create compilation tasks for targets unsupported by the current host`() =
        withProject {
            val hosts = listOf("macos_x64", "linux_x64", "mingw_x64")
            components.withType(KotlinNativeMainComponent::class.java).getByName("main").targets = hosts
            evaluate()
            hosts.map { HostManager().targetByName(it) }.forEach {
                val task = tasks.findByName("compileDebug${it.name.capitalize()}KotlinNative")

                if (HostManager().enabled.contains(it)) {
                    assertNotNull(task)
                } else {
                    assertNull(task)
                }
            }
        }

    private fun assertCompileOutcome(result: BuildResult, compileTasks: Collection<String>, expectedOutcome: TaskOutcome) {
        compileTasks.forEach { taskName ->
            val task = result.task(taskName)
            assertNotNull(task, "Task '$taskName' was not executed") {
                assertEquals(
                    expectedOutcome,
                    it.outcome,
                    "Task '$taskName' has an incorrect outcome."
                )
            }
        }
    }

    @Test
    fun `Compilation should be up-to-date if there is no changes`() {
        val project = KonanProject.create(projectDirectory).apply {
            buildFile.writeText("""
                plugins { id 'kotlin-native' }

                sourceSets.main {
                    component {
                        outputKinds = [ EXECUTABLE, KLIBRARY, FRAMEWORK ]
                        targets = ['host', 'wasm32']
                    }
                }

            """.trimIndent())
        }

        val outputKinds = arrayOf(OutputKind.EXECUTABLE, OutputKind.FRAMEWORK)
        val buildTypes = arrayOf("Debug", "Release")
        val targets = arrayOf(HostManager.host, KonanTarget.WASM32)

        val compileTasks = targets.flatMap { target ->
            outputKinds.filter { it.availableFor(target) }.flatMap { kind ->
                buildTypes.map { type ->
                    ":compile${type}${kind.name.toLowerCase().capitalize()}${target.name.capitalize()}KotlinNative"
                }
            } + ":compileDebug${OutputKind.KLIBRARY.name.toLowerCase().capitalize()}${target.name.capitalize()}KotlinNative"
        }

        val result1 = project.createRunner().withArguments("assemble").build()
        assertCompileOutcome(result1, compileTasks, TaskOutcome.SUCCESS)

        val result2 = project.createRunner().withArguments("assemble").build()
        assertCompileOutcome(result2, compileTasks, TaskOutcome.UP_TO_DATE)
    }

    @Test
    fun `Framework name should not contain minus symbols`() = withProject("test-framework-project") {
        assumeTrue(HostManager.hostIsMac)
        components.withType(KotlinNativeMainComponent::class.java)
            .getByName("main")
            .outputKinds
            .set(listOf(OutputKind.FRAMEWORK, OutputKind.KLIBRARY))
        evaluate()

        val compileTasks = tasks.withType(KotlinNativeCompile::class.java)
        val frameworkTask = compileTasks.getByName("compileDebugFrameworkKotlinNative")
        val klibraryTask = compileTasks.getByName("compileDebugKlibraryKotlinNative")


        assertEquals("test_framework_project", frameworkTask.outputFile.nameWithoutExtension)
        assertEquals("test-framework-project", klibraryTask.outputFile.nameWithoutExtension)
    }

    @Test
    fun `Plugin should be able to build static and dynamic libraries`() {

        val project = KonanProject.create(projectDirectory).apply {
            buildFile.writeText("""
                plugins { id 'kotlin-native' }

                sourceSets.main {
                    component {
                        outputKinds = [ DYNAMIC, STATIC ]
                        targets = ['host']
                    }
                }

            """.trimIndent())
            settingsFile.appendText("\nrootProject.name = 'test-library'")
        }

        val baseName = "test_library"
        val sharedPrefix = CompilerOutputKind.DYNAMIC.prefix(HostManager.host)
        val sharedSuffix = CompilerOutputKind.DYNAMIC.suffix(HostManager.host)
        val staticPrefix = CompilerOutputKind.STATIC.prefix(HostManager.host)
        val staticSuffix = CompilerOutputKind.STATIC.suffix(HostManager.host)

        val libraryPaths = listOf(
            "build/lib/main/debug/dynamic/$sharedPrefix$baseName$sharedSuffix",
            "build/lib/main/release/dynamic/$sharedPrefix$baseName$sharedSuffix",
            "build/lib/main/debug/static/$staticPrefix$baseName$staticSuffix",
            "build/lib/main/release/static/$staticPrefix$baseName$staticSuffix"
        )

        val headerPaths = listOf(
            "build/lib/main/debug/dynamic/$sharedPrefix${baseName}_api.h",
            "build/lib/main/release/dynamic/$sharedPrefix${baseName}_api.h",
            "build/lib/main/debug/static/$staticPrefix${baseName}_api.h",
            "build/lib/main/release/static/$staticPrefix${baseName}_api.h"
        )

        val linkTasks = listOf(
            ":compileDebugDynamicKotlinNative",
            ":compileReleaseDynamicKotlinNative",
            ":compileDebugStaticKotlinNative",
            ":compileReleaseStaticKotlinNative"
        )

        project.createRunner().withArguments("assemble", "-i").build().let { result ->
            libraryPaths.forEach { assertFileExists(it) }
            headerPaths.forEach { assertFileExists(it) }
            linkTasks.forEach { assertEquals(TaskOutcome.SUCCESS, result.task(it)!!.outcome)  }
        }

        project.createRunner().withArguments("assemble").build().let { result ->
            linkTasks.forEach { assertEquals(TaskOutcome.UP_TO_DATE, result.task(it)!!.outcome)  }
        }

        assertTrue(projectDirectory.resolve(headerPaths[0]).delete())

        project.createRunner().withArguments("assemble").build().let { result ->
            assertEquals(TaskOutcome.SUCCESS, result.task(linkTasks[0])!!.outcome)
            linkTasks.drop(1).forEach {
                assertEquals(TaskOutcome.UP_TO_DATE, result.task(it)!!.outcome)
            }

            libraryPaths.forEach { assertFileExists(it) }
            headerPaths.forEach { assertFileExists(it) }
        }
    }

    @Test
    fun `Plugin should support cinterop dependencies`() {
        // region Project set up.
        val rootProject = KonanProject.createEmpty(projectDirectory).apply {
            buildFile.writeText("""
                plugins {
                    id 'kotlin-native'
                }

                group 'test'
                version '1.0'

                repositories {
                    maven { url = 'repo' }
                }

                sourceSets.main.component {
                    outputKinds = [ KLIBRARY ]
                    targets = ['host']
                }

                dependencies {
                    implementation project('libFoo')
                }
            """.trimIndent())

            settingsFile.writeText("""
                enableFeaturePreview('GRADLE_METADATA')

                rootProject.name = 'interop-test'

                include 'libFoo'
                include 'libBar'
            """.trimIndent())

            generateSrcFile("main.kt", """
                import kotlinx.cinterop.*
                import mystdio.*

                fun main(args: Array<String>) {
                    printf(foo())
                }
            """.trimIndent())

            generateSrcFile(listOf("src", "test", "kotlin"), "test.kt", """
                import kotlin.test.*

                @Test
                fun mainTest() {
                    main(emptyArray<String>())
                }
            """.trimIndent())
        }

        val libFooProject = KonanProject.createEmpty(rootProject.createSubDir("libFoo")).apply {
            buildFile.writeText("""
                plugins {
                    id 'kotlin-native'
                    id 'maven-publish'
                }

                group 'test'
                version '1.0'

                sourceSets.main {

                    component {
                        targets = ['host']
                        outputKinds = [ KLIBRARY ]
                    }

                    dependencies {
                        implementation project(':libBar')
                        cinterop('mystdio') {
                            extraOpts '-no-default-libs'
                            extraOpts '-no-endorsed-libs'
                        }
                    }
                }

                publishing {
                    repositories {
                        maven {
                            url = '../repo'
                        }
                    }
                }
            """.trimIndent())

            generateSrcFile("lib.kt", """
                fun foo() = "Interop is here!\n${'$'}{bar()}\n"
            """.trimIndent())

            generateDefFile("mystdio.def", """
                headers = stdio.h
                compilerOpts.osx = -D_ANSI_SOURCE
            """.trimIndent())
        }

        val libBarProject = KonanProject.createEmpty(rootProject.createSubDir("libBar")).apply {
            buildFile.writeText("""
                plugins {
                    id 'kotlin-native'
                    id 'maven-publish'
                }

                group 'test'
                version '1.0'

                sourceSets.main {
                    component {
                        targets = ['host']
                        outputKinds = [ KLIBRARY ]
                    }
                }

                publishing {
                    repositories {
                        maven {
                            url = '../repo'
                        }
                    }
                }
            """.trimIndent())

            generateSrcFile("lib.kt", """
                fun bar() = "Transitive call!"
            """.trimIndent())
        }
        // endregion.

        rootProject.createRunner().withArguments("build").build().apply {
            output.contains("Interop is here!")
            output.contains("Transitive call!")
        }

        assertFileExists("libFoo/build/cinterop/mystdio/${HostManager.hostName}/mystdio.klib")

        rootProject.createRunner().withArguments(":libFoo:publish", ":libBar:publish").build()
        assertFileExists("repo/test/libFoo_debug/1.0/libFoo_debug-1.0-interop-mystdio.klib")

        // A dependency on a published library
        rootProject.buildFile.writeText("""
            plugins {
                id 'kotlin-native'
            }

            group 'test'
            version '1.0'

            repositories {
                maven { url = 'repo' }
            }

            sourceSets.main.component {
                outputKinds = [ KLIBRARY ]
                targets = ['host']
            }

            dependencies {
                implementation 'test:libFoo:1.0'
            }
        """.trimIndent())

        assertTrue(projectDirectory.resolve("build").deleteRecursively())
        assertTrue(projectDirectory.resolve("libFoo/build").deleteRecursively())
        assertTrue(projectDirectory.resolve("libBar/build").deleteRecursively())

        rootProject.createRunner().withArguments("build").build().apply {
            output.contains("Interop is here!")
            output.contains("Transitive call!")
        }
    }

    @Test
    fun `Plugin should support custom entry points`() {
        val hostSuffix = CompilerOutputKind.PROGRAM.suffix(HostManager.host)
        val exePath = "build/exe/main/release/entry-point$hostSuffix"
        val project = KonanProject.createEmpty(projectDirectory).apply {
            buildFile.writeText("""
                plugins { id 'kotlin-native' }

                components.main {
                    outputKinds = [EXECUTABLE]
                    entryPoint = 'org.test.myMain'
                }

                task run(type: Exec) {
                    commandLine file('$exePath').absolutePath
                }
            """.trimIndent())
            settingsFile.appendText("rootProject.name = 'entry-point'")

            generateSrcFile("main.kt", """
                package org.test

                fun myMain(args: Array<String>) {
                    println("myMain called!")
                }
            """.trimIndent())
        }
        project.createRunner().withArguments(":assemble").build()
        assertFileExists(exePath)
        val result = project.createRunner().withArguments(":run").build()
        assertTrue(result.output.contains("myMain called!"))
    }

    @Test
    fun `Plugin should support the konan tooling model`() {
        withProject {
            val hostName = HostManager.hostName
            val mainSrcDirs = listOf("src/main/kotlin", "src/other/kotlin").map {
                file(it).apply { mkdirs() }
            }
            val testDir = file("src/test/kotlin").apply { mkdirs() }

            val mainSrcFiles = listOf(
                "src/main/kotlin/main.kt" to "fun main(args: Array<String>) { foo() }",
                "src/other/kotlin/foo.kt" to "fun foo() { println(42) }"
            ).map { (name, content) ->
                project.file(name).apply { createNewFile(); writeText(content) }
            }
            val testSrcFile = file("src/test/kotlin/test.kt").apply {
                createNewFile()
                writeText("""
                    import kotlin.test.*
                    @Test fun test() { foo() }
                """.trimIndent())
            }

            components.withType(KotlinNativeMainComponent::class.java).getByName("main").apply {
                outputKinds.set(listOf(OutputKind.KLIBRARY, OutputKind.EXECUTABLE))
                targets = listOf(hostName, "wasm32")
            }
            kotlinNativeSourceSets.getByName("main").kotlin.srcDirs(*mainSrcDirs.toTypedArray())

            evaluate()

            val model = KonanToolingModelBuilder.buildAll("konan", this)
            val mainArtifacts = model.artifacts.filterNot { it.name.toLowerCase().contains("test") }
            val testArtifacts = model.artifacts.filter { it.name.toLowerCase().contains("test") }

            mainArtifacts.forEach {
                assertEquals(mainSrcDirs.toSet(), it.srcDirs.toSet())
                assertEquals(mainSrcFiles.toSet(), it.srcFiles.toSet())
            }
            testArtifacts.forEach {
                assertEquals(setOf(testDir), it.srcDirs.toSet())
                assertEquals(setOf(testSrcFile) + mainSrcFiles.toSet(), it.srcFiles.toSet())
            }

            val nameToArtifact = model.artifacts.map { it.name to it }.toMap()

            val kinds = listOf(OutputKind.EXECUTABLE, OutputKind.KLIBRARY)
            val targets = listOf(HostManager.host, KonanTarget.WASM32)

            // Production binaries
            kinds.forEach { kind ->
                val buildTypes = if (kind == OutputKind.KLIBRARY) listOf("debug") else listOf("debug", "release")
                buildTypes.forEach { buildType ->
                    targets.forEach { target ->
                        val suffix = "${buildType.capitalize()}${kind.name.toLowerCase().capitalize()}${target.name.capitalize()}"
                        val artifact = nameToArtifact.getValue("main$suffix")
                        assertEquals(target.name, artifact.targetPlatform)
                        assertEquals(kind.compilerOutputKind, artifact.type)
                        assertEquals("compile${suffix}KotlinNative", artifact.buildTaskName)

                        val outputRoot = if (kind == OutputKind.EXECUTABLE) "exe" else "lib"
                        val outputFile = file(
                            "build/$outputRoot/main/$buildType/${kind.name.toLowerCase()}/${target.name}/" +
                                    "testProject${kind.compilerOutputKind.suffix(target)}"
                        )
                        assertEquals(outputFile, artifact.file)
                    }
                }
            }

            // Test binaries
            targets.forEach { target ->
                val suffix = "Debug${target.name.capitalize()}"
                val artifact = nameToArtifact.getValue("test$suffix")
                assertEquals(target.name, artifact.targetPlatform)
                assertEquals(CompilerOutputKind.PROGRAM, artifact.type)
                assertEquals("compileTest${suffix}KotlinNative", artifact.buildTaskName)
                assertEquals(
                    file("build/test-exe/test/debug/${target.name}/test${CompilerOutputKind.PROGRAM.suffix(target)}"),
                    artifact.file
                )
            }
        }
    }

    @Test
    fun `Plugin should provide a correct serialization compiler plugin`() {
        val project = KonanProject.createEmpty(projectDirectory).apply {
            buildFile.writeText("""
                import org.jetbrains.kotlin.gradle.plugin.experimental.tasks.KotlinNativeCompile

                plugins {
                    id 'kotlin-native'
                    id 'kotlinx-serialization-native'
                }

                repositories {
                    maven { url "https://kotlin.bintray.com/kotlin-eap" }
                    maven { url "https://kotlin.bintray.com/kotlin-dev" }
                    maven { url "${MultiplatformSpecification.KOTLIN_REPO}" }
                }

                def assertTrue(boolean cond, String message) {
                    if (!cond) {
                        throw AssertionError(message)
                    }
                }

                task assertClassPath {
                    doLast {
                        tasks.withType(KotlinNativeCompile.class).all {
                            def compileClassPath = it.compilerPluginClasspath
                            assertTrue(compileClassPath != null, "compileClassPath should not be null")
                            assertTrue(!compileClassPath.isEmpty(), "No compiler plugins in the classpath")
                            assertTrue(compileClassPath.singleFile.absolutePath.contains("kotlin-serialization-unshaded"),
                                        "No unshaded version of serialization plugin in the classpath")
                        }
                    }
                }
            """.trimIndent())
        }
        project.createRunner().withArguments("assertClassPath").build()
    }

    @Test
    fun `Plugin should be compatible with the MPP one`() {
        val repo = tmpFolder.newFolder("repo")
        val repoPath = KonanProject.escapeBackSlashes(repo.absolutePath)
        val nativeProducer = KonanProject.createEmpty(tmpFolder.newFolder("native-producer")).apply {
            buildFile.writeText("""
                plugins {
                    id 'kotlin-native'
                    id 'maven-publish'
                }

                group 'test'
                version '1.0'

                sourceSets.main.component {
                    targets = ['wasm32', 'host']
                }

                publishing {
                    repositories {
                        maven { url = '$repoPath' }
                    }
                }
            """.trimIndent())

            settingsFile.appendText("enableFeaturePreview('GRADLE_METADATA')")
            generateSrcFile("native.kt", "fun native() = 42")
        }

        val mpp = KonanProject.createEmpty(tmpFolder.newFolder("mpp")).apply {
            buildFile.writeText("""
                plugins {
                    id 'org.jetbrains.kotlin.multiplatform' version '${MultiplatformSpecification.KOTLIN_VERSION}'
                    id 'maven-publish'
                }

                group 'test'
                version '1.0'

                repositories {
                    maven { url '$repoPath' }
                    maven { url "${MultiplatformSpecification.KOTLIN_REPO}" }
                    jcenter()
                }

                kotlin {
                    sourceSets {
                        commonMain {
                            dependencies {
                                implementation 'org.jetbrains.kotlin:kotlin-stdlib-common'
                                implementation 'test:native-producer:1.0'
                            }
                        }
                    }

                    targets {
                        fromPreset(presets.wasm32, 'wasm')
                    }
                }

                publishing {
                    repositories {
                        maven { url = '$repoPath' }
                    }
                }
            """.trimIndent())

            settingsFile.writeText("""
                pluginManagement {
                    repositories {
                        maven { url "${MultiplatformSpecification.KOTLIN_REPO}" }
                        jcenter()
                    }
                }
                enableFeaturePreview('GRADLE_METADATA')
            """.trimIndent())

            propertiesFile.writeText("org.jetbrains.kotlin.native.home=$konanHome")
            generateSrcFile(listOf("src/wasmMain/kotlin"), "mpp.kt", "fun mpp() = native()")
        }

        val nativeConsumer = KonanProject.createEmpty(tmpFolder.newFolder("native-consumer")).apply {
            buildFile.writeText("""
                plugins {
                    id 'kotlin-native'
                    id 'maven-publish'
                }

                group 'test'
                version '1.0'

                repositories {
                    maven { url '$repoPath' }
                    maven { url "${MultiplatformSpecification.KOTLIN_REPO}" }
                    jcenter()
                }

                sourceSets.main.component {
                    targets = ['wasm32']
                }

                dependencies {
                    implementation 'test:mpp:1.0'
                }

                publishing {
                    repositories {
                        maven { url = '$repoPath' }
                    }
                }
            """.trimIndent())

            settingsFile.appendText("enableFeaturePreview('GRADLE_METADATA')")
            generateSrcFile("consumer.kt", "fun consumer() = mpp()")
        }

        val mppConsumer = KonanProject.createEmpty(tmpFolder.newFolder("mpp-consumer")).apply {
            buildFile.writeText("""
                plugins {
                    id 'org.jetbrains.kotlin.multiplatform' version '${MultiplatformSpecification.KOTLIN_VERSION}'
                }

                group 'test'
                version '1.0'

                repositories {
                    maven { url '$repoPath' }
                    maven { url "${MultiplatformSpecification.KOTLIN_REPO}" }
                    jcenter()
                }

                kotlin {
                    sourceSets {
                        commonMain.dependencies {
                            implementation 'org.jetbrains.kotlin:kotlin-stdlib-common'
                            implementation 'test:native-consumer:1.0'
                        }
                    }

                    targets {
                        fromPreset(presets.wasm32, 'wasm')
                    }
                }
            """.trimIndent())

            settingsFile.writeText("""
                pluginManagement {
                    repositories {
                        maven { url "${MultiplatformSpecification.KOTLIN_REPO}" }
                        jcenter()
                    }
                }
                enableFeaturePreview('GRADLE_METADATA')
            """.trimIndent())
        }

        nativeProducer.createRunner().withArguments("publish").build()
        mpp.createRunner().withArguments("publish").build()
        mpp.createRunner().withArguments("dependencies").build().also {
            assertFalse(it.output.contains("FAILED"))
        }
        nativeConsumer.createRunner().withArguments("publish").build()
        mppConsumer.createRunner().withArguments("dependencies").build().also {
            assertFalse(it.output.contains("FAILED"))
        }

        val publishedFiles = listOf(
            "repo/test/native-producer/1.0/native-producer-1.0.module",
            "repo/test/native-producer_debug_wasm32/1.0/native-producer_debug_wasm32-1.0.module",
            "repo/test/native-producer_debug_wasm32/1.0/native-producer_debug_wasm32-1.0.klib",
            "repo/test/mpp/1.0/mpp-1.0.module",
            "repo/test/mpp-wasm/1.0/mpp-wasm-1.0.module",
            "repo/test/mpp-wasm/1.0/mpp-wasm-1.0.klib"
        )

        publishedFiles.forEach {
            assertFileExists(it)
        }
    }

    @Test
    fun `Plugin should allow overriding Kotlin-Native version`() {
        val project = KonanProject.createEmpty(projectDirectory).apply {
            buildFile.writeText("plugins { id 'kotlin-native' }")
            propertiesFile.writeText("")
        }

        val result = project.createRunner().withArguments(
            "checkKonanCompiler",
            "-Porg.jetbrains.kotlin.native.version=0.9.3",
            "-i"
        ).build()
        assertTrue(result.output.contains(
            "Downloading Kotlin/Native compiler from.*kotlin-native-${HostManager.simpleOsName()}-0.9.3".toRegex()
        ))
    }

    @Test
    fun `Plugin should show warning if deprecated properties are used`() {
        val project = KonanProject.createEmpty(projectDirectory).apply {
            buildFile.writeText("plugins { id 'kotlin-native' }")
            val properties = propertiesFile.readText().replace("org.jetbrains.kotlin.native.home", "konan.home")
            propertiesFile.writeText(properties)
        }

        val result = project.createRunner().withArguments("tasks").build()
        assertTrue(result.output.contains(
            "Project property 'konan.home' is deprecated. Use 'org.jetbrains.kotlin.native.home' instead."
        ))
    }

    @Test
    fun `Plugin should support OptionalExpectation`() {
        val project = KonanProject.createEmpty(projectDirectory).apply {
            buildFile.writeText("""
                plugins {
                    id 'kotlin-native'
                }

                components.main.targets = ['host']
                components.test.targets = ['host']
            """.trimIndent())
            generateSrcFile("main.kt", """
                import kotlin.jvm.*

                class A {
                    @JvmField
                    val a = "A.a"
                }

                fun foo() {
                    println(A().a)
                }
            """.trimIndent())
            generateSrcFile(listOf("src", "test", "kotlin"), "test.kt", """
                import kotlin.test.*

                @Test
                fun test() {
                    foo()
                }
            """.trimIndent())
        }
        val result = project.createRunner().withArguments("build").build()
        assertTrue(result.output.contains("A.a"))
    }

    private fun BuildResult.checkFrameworkCompilationCommandLine(check: (String) -> Unit) {
        output.lineSequence().filter {
            it.contains("Run tool: konanc") && it.contains("-p framework")
        }.toList().also {
            assertTrue(it.isNotEmpty())
        }.forEach(check)
    }

    @Test
    fun `Plugin should support symbol exporting for frameworks`() {
        assumeTrue(HostManager.hostIsMac)
        val transitiveDir = tmpFolder.newFolder("transitive")
        val transitiveProject = KonanProject.createEmpty(transitiveDir).apply {
            buildFile.writeText("""
                plugins { id 'kotlin-native' }
                components.main.targets = [ 'ios_arm64' ]
            """)
            generateSrcFile("transitive.kt", "fun transitive() = 42")
        }

        val libraryDir = tmpFolder.newFolder("library")
        val libraryProject = KonanProject.createEmpty(libraryDir).apply {
            buildFile.writeText("""
                plugins { id 'kotlin-native' }
                components.main.targets = [ 'ios_arm64' ]

                dependencies {
                    implementation project(':transitive')
                }
            """.trimIndent())
            generateSrcFile("library.kt", "fun foo() = transitive()")
        }

        val project = KonanProject.createEmpty(projectDirectory).apply {
            settingsFile.writeText("""
                include ':library'
                include ':transitive'
                rootProject.name = 'test'
            """.trimIndent())
            buildFile.writeText("""
                plugins { id 'kotlin-native' }

                dependencies {
                    export project(':library')
                }

                sourceSets.main.component {
                    targets = [ 'ios_arm64' ]
                    outputKinds = [ FRAMEWORK ]
                }
            """.trimIndent())
            generateSrcFile("main.kt", "fun main(args: Array<String>) { println(foo()) }")
        }

        with(project.createRunner().withArguments("compileDebugKotlinNative", "--info").build()) {
            assertEquals(TaskOutcome.SUCCESS, task(":compileDebugKotlinNative")?.outcome)
            assertEquals(TaskOutcome.SUCCESS, task(":library:compileDebugKotlinNative")?.outcome)
            assertTrue(projectDirectory.resolve("build/lib/main/debug/test.framework").exists())

            val header = projectDirectory.resolve("build/lib/main/debug/test.framework/Headers/test.h")
            assertTrue(header.exists())
            assertTrue(header.readText().contains("+ (int32_t)foo "))
            assertFalse(header.readText().contains("+ (int32_t)transitive")) // By default export is non-transitive.

            checkFrameworkCompilationCommandLine {
                assertTrue(it.contains("-Xembed-bitcode-marker"))
                assertTrue(it.contains("-g"))
            }
        }

        with(project.createRunner().withArguments("compileReleaseKotlinNative", "--info").build()) {
            assertEquals(TaskOutcome.SUCCESS, task(":compileReleaseKotlinNative")?.outcome)
            checkFrameworkCompilationCommandLine {
                assertTrue(it.contains("-Xembed-bitcode"))
                assertTrue(it.contains("-opt"))
            }
        }

        // Check that transitive export works too.
        project.buildFile.appendText("\ncomponents.main.dependencies.transitiveExport = true")
        with(project.createRunner().withArguments("compileDebugKotlinNative").build()) {
            val header = projectDirectory.resolve("build/lib/main/debug/test.framework/Headers/test.h")
            assertTrue(header.exists())
            assertTrue(header.readText().contains("+ (int32_t)foo "))
            assertTrue(header.readText().contains("+ (int32_t)transitive"))
        }
    }
}
