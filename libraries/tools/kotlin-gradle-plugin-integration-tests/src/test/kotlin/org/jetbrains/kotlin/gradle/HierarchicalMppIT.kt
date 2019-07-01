/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.internals.MULTIPLATFORM_PROJECT_METADATA_FILE_NAME
import org.jetbrains.kotlin.gradle.internals.parseKotlinSourceSetMetadataFromXml
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinProjectStructureMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.ModuleDependencyIdentifier
import org.jetbrains.kotlin.gradle.util.modify
import java.io.File
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HierarchicalMppIT : BaseGradleIT() {
    companion object {
        private val gradleVersion = GradleVersionRequired.AtLeast("5.0")
    }

    @Test
    fun testPublishedModules() {
        Project("third-party-lib", gradleVersion, "hierarchical-mpp-published-modules").run {
            setupWorkingDir()
            gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)
            build("publish") {
                assertSuccessful()
            }
        }

        Project("my-lib-foo", gradleVersion, "hierarchical-mpp-published-modules").run {
            setupWorkingDir()
            gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)
            build("publish") {
                checkMyLibFoo(this, subprojectPrefix = null)
            }
        }

        Project("my-lib-bar", gradleVersion, "hierarchical-mpp-published-modules").run {
            setupWorkingDir()
            gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)
            build("publish") {
                checkMyLibBar(this, subprojectPrefix = null)
            }
        }

        Project("my-app", gradleVersion, "hierarchical-mpp-published-modules").run {
            setupWorkingDir()
            gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)
            build("assemble") {
                checkMyApp(this, subprojectPrefix = null)
            }
        }
    }

    @Test
    fun testProjectDependencies() {
        Project("third-party-lib", gradleVersion, "hierarchical-mpp-project-dependency").run {
            setupWorkingDir()
            gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)
            build("publish") {
                assertSuccessful()
            }
        }

        with(Project("hierarchical-mpp-project-dependency", gradleVersion)) {
            setupWorkingDir()
            gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)

            build("publish", "assemble") {
                checkMyLibFoo(this, subprojectPrefix = "my-lib-foo")
                checkMyLibBar(this, subprojectPrefix = "my-lib-bar")
                checkMyApp(this, subprojectPrefix = "my-app")
            }
        }
    }

    private fun checkMyLibFoo(compiledProject: CompiledProject, subprojectPrefix: String? = null) = with(compiledProject) {
        assertSuccessful()
        assertTasksExecuted(expectedTasks(subprojectPrefix))

        ZipFile(
            project.projectDir.parentFile.resolve(
                "repo/com/example/foo/my-lib-foo-metadata/1.0/my-lib-foo-metadata-1.0-all.jar"
            )
        ).use { publishedMetadataJar ->
            publishedMetadataJar.checkAllEntryNamesArePresent(
                "META-INF/$MULTIPLATFORM_PROJECT_METADATA_FILE_NAME",

                "commonMain/META-INF/my-lib-foo.kotlin_module",
                "commonMain/com/example/foo/FooKt.kotlin_metadata",

                "jvmAndJsMain/META-INF/my-lib-foo_jvmAndJsMain.kotlin_module",
                "jvmAndJsMain/com/example/foo/FooJvmAndJsKt.kotlin_metadata",

                "linuxAndJsMain/META-INF/my-lib-foo_linuxAndJsMain.kotlin_module",
                "linuxAndJsMain/com/example/foo/FooLinuxAndJsKt.kotlin_metadata"
            )

            val parsedProjectStructureMetadata: KotlinProjectStructureMetadata = publishedMetadataJar.getProjectStructureMetadata()

            val expectedProjectStructureMetadata = expectedProjectStructureMetadata(
                sourceSetModuleDependencies = mapOf(
                    "jvmAndJsMain" to setOf("com.example.thirdparty" to "third-party-lib"),
                    "linuxAndJsMain" to emptySet(),
                    "commonMain" to emptySet()
                )
            )

            assertEquals(expectedProjectStructureMetadata, parsedProjectStructureMetadata)
        }
    }

    private fun checkMyLibBar(compiledProject: CompiledProject, subprojectPrefix: String?) = with(compiledProject) {
        val taskPrefix = subprojectPrefix?.let { ":$it" }.orEmpty()

        assertSuccessful()
        assertTasksExecuted(expectedTasks(subprojectPrefix))

        ZipFile(
            project.projectDir.parentFile.resolve(
                "repo/com/example/bar/my-lib-bar-metadata/1.0/my-lib-bar-metadata-1.0-all.jar"
            )
        ).use { publishedMetadataJar ->
            publishedMetadataJar.checkAllEntryNamesArePresent(
                "META-INF/$MULTIPLATFORM_PROJECT_METADATA_FILE_NAME",

                "commonMain/META-INF/my-lib-bar.kotlin_module",
                "commonMain/com/example/bar/BarKt.kotlin_metadata",

                "jvmAndJsMain/META-INF/my-lib-bar_jvmAndJsMain.kotlin_module",
                "jvmAndJsMain/com/example/bar/BarJvmAndJsKt.kotlin_metadata",

                "linuxAndJsMain/META-INF/my-lib-bar_linuxAndJsMain.kotlin_module",
                "linuxAndJsMain/com/example/bar/BarLinuxAndJsKt.kotlin_metadata"
            )

            val parsedProjectStructureMetadata: KotlinProjectStructureMetadata = publishedMetadataJar.getProjectStructureMetadata()

            val expectedProjectStructureMetadata = expectedProjectStructureMetadata(
                sourceSetModuleDependencies = mapOf(
                    "jvmAndJsMain" to setOf(),
                    "linuxAndJsMain" to emptySet(),
                    "commonMain" to setOf("com.example.foo" to "my-lib-foo")
                )
            )

            assertEquals(expectedProjectStructureMetadata, parsedProjectStructureMetadata)
        }

        checkNamesOnCompileClasspath(
            "$taskPrefix:compileKotlinMetadata",
            shouldInclude = listOf(
                "my-lib-foo" to "main"
            ),
            shouldNotInclude = listOf(
                "my-lib-foo" to "jvmAndJsMain",
                "my-lib-foo" to "linuxAndJsMain",
                "third-party-lib-metadata-1.0" to ""
            )
        )

        checkNamesOnCompileClasspath(
            "$taskPrefix:compileJvmAndJsMainKotlinMetadata",
            shouldInclude = listOf(
                "my-lib-foo" to "main",
                "my-lib-foo" to "jvmAndJsMain",
                "third-party-lib-metadata-1.0" to ""
            ),
            shouldNotInclude = listOf(
                "my-lib-foo" to "linuxAndJsMain"
            )
        )

        checkNamesOnCompileClasspath(
            "$taskPrefix:compileLinuxAndJsMainKotlinMetadata",
            shouldInclude = listOf(
                "my-lib-foo" to "linuxAndJsMain",
                "my-lib-foo" to "main"
            ),
            shouldNotInclude = listOf(
                "my-lib-foo" to "jvmAndJsMain",
                "third-party-lib-metadata-1.0" to ""
            )
        )
    }

    private fun checkMyApp(compiledProject: CompiledProject, subprojectPrefix: String?) = with(compiledProject) {
        val taskPrefix = subprojectPrefix?.let { ":$it" }.orEmpty()

        assertSuccessful()
        assertTasksExecuted(expectedTasks(subprojectPrefix))

        checkNamesOnCompileClasspath(
            "$taskPrefix:compileKotlinMetadata",
            shouldInclude = listOf(
                "my-lib-bar" to "main",
                "my-lib-foo" to "main"
            ),
            shouldNotInclude = listOf(
                "my-lib-bar" to "jvmAndJsMain",
                "my-lib-bar" to "linuxAndJsMain",
                "my-lib-foo" to "jvmAndJsMain",
                "my-lib-foo" to "linuxAndJsMain",
                "third-party-lib-metadata-1.0" to ""
            )
        )

        checkNamesOnCompileClasspath(
            "$taskPrefix:compileJvmAndJsMainKotlinMetadata",
            shouldInclude = listOf(
                "my-lib-bar" to "main",
                "my-lib-bar" to "jvmAndJsMain",
                "my-lib-foo" to "main",
                "my-lib-foo" to "jvmAndJsMain",
                "third-party-lib-metadata-1.0" to ""
            ),
            shouldNotInclude = listOf(
                "my-lib-bar" to "linuxAndJsMain",
                "my-lib-foo" to "linuxAndJsMain"
            )
        )

        checkNamesOnCompileClasspath(
            "$taskPrefix:compileLinuxAndJsMainKotlinMetadata",
            shouldInclude = listOf(
                "my-lib-bar" to "main",
                "my-lib-bar" to "linuxAndJsMain",
                "my-lib-foo" to "main",
                "my-lib-foo" to "linuxAndJsMain"
            ),
            shouldNotInclude = listOf(
                "my-lib-bar" to "jvmAndJsMain",
                "my-lib-foo" to "jvmAndJsMain",
                "third-party-lib-metadata-1.0" to ""
            )
        )

        checkNamesOnCompileClasspath("$taskPrefix:compileLinuxAndJsMainKotlinMetadata")
    }

    private fun CompiledProject.checkNamesOnCompileClasspath(
        taskPath: String,
        shouldInclude: Iterable<Pair<String, String>> = emptyList(),
        shouldNotInclude: Iterable<Pair<String, String>> = emptyList()
    ) {
        val taskOutput = getOutputForTask(taskPath.removePrefix(":"))
        val compilerArgsLine = taskOutput.lines().single { "Kotlin compiler args:" in it }
        val classpathItems = compilerArgsLine.substringAfter("-classpath").substringBefore(" -").split(File.pathSeparator)

        shouldInclude.forEach { (module, sourceSet) ->
            assertTrue("expected module '$module' source set '$sourceSet' on the classpath of task $taskPath") {
                classpathItems.any { module in it && it.contains(sourceSet, ignoreCase = true) }
            }
        }

        shouldNotInclude.forEach { (module, sourceSet) ->
            assertTrue("not expected module '$module' source set '$sourceSet' on the compile classpath of task $taskPath") {
                classpathItems.none { module in it && it.contains(sourceSet, ignoreCase = true) }
            }
        }
    }

    private fun expectedTasks(subprojectPrefix: String?) = listOf(
        "generateProjectStructureMetadata",
        "transformCommonMainDependenciesMetadata",
        "transformJvmAndJsMainDependenciesMetadata",
        "transformLinuxAndJsMainDependenciesMetadata",
        "compileKotlinMetadata",
        "compileJvmAndJsMainKotlinMetadata",
        "compileLinuxAndJsMainKotlinMetadata"
    ).map { subprojectPrefix?.let { ":$it" }.orEmpty() + ":" + it }

    // the projects used in these tests are similar and only the dependencies differ:
    private fun expectedProjectStructureMetadata(
        sourceSetModuleDependencies: Map<String, Set<Pair<String, String>>>
    ): KotlinProjectStructureMetadata {

        val jvmSourceSets = setOf("commonMain", "jvmAndJsMain")
        val jsSourceSets = setOf("commonMain", "jvmAndJsMain", "linuxAndJsMain")
        return KotlinProjectStructureMetadata(
            sourceSetNamesByVariantName = mapOf(
                "js-api" to jsSourceSets,
                "js-runtime" to jsSourceSets,
                "jvm-api" to jvmSourceSets,
                "jvm-runtime" to jvmSourceSets,
                "linuxX64-api" to setOf("commonMain", "linuxAndJsMain")
            ),
            sourceSetsDependsOnRelation = mapOf(
                "jvmAndJsMain" to setOf("commonMain"),
                "linuxAndJsMain" to setOf("commonMain"),
                "commonMain" to emptySet()
            ),
            sourceSetModuleDependencies = sourceSetModuleDependencies.mapValues { (_, pairs) ->
                pairs.map {
                    ModuleDependencyIdentifier(it.first, it.second)
                }.toSet()
            }
        )
    }

    private fun ZipFile.checkAllEntryNamesArePresent(vararg expectedEntryNames: String) {
        val entryNames = entries().asSequence().map { it.name }.toSet()
        val entryNamesString = entryNames.joinToString()
        expectedEntryNames.forEach {
            assertTrue("expecting entry $it in entry names $entryNamesString") { it in entryNames }
        }
    }

    private fun ZipFile.getProjectStructureMetadata(): KotlinProjectStructureMetadata {
        val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val document = getInputStream(getEntry("META-INF/$MULTIPLATFORM_PROJECT_METADATA_FILE_NAME"))
            .use { inputStream -> documentBuilder.parse(inputStream) }
        return checkNotNull(parseKotlinSourceSetMetadataFromXml(document))
    }
}