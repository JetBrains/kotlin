/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Test
import java.io.File

class BuildCacheIT : BaseGradleIT() {
    override fun defaultBuildOptions(): BuildOptions =
        super.defaultBuildOptions().copy(withBuildCache = true)

    companion object {
        private val GRADLE_VERSION = GradleVersionRequired.AtLeast("4.3")
    }

    @Test
    fun testNoCacheWithGradlePre43() = with(Project("simpleProject", GradleVersionRequired.Exact("4.2"))) {
        // Check that even with the build cache enabled, the Kotlin tasks are not cacheable with Gradle < 4.3:
        val optionsWithCache = defaultBuildOptions().copy(withBuildCache = true)

        build("assemble", options = optionsWithCache) {
            assertSuccessful()
            assertNotContains("Packing task ':compileKotlin'")
        }
        build("clean", "assemble", options = optionsWithCache) {
            assertSuccessful()
            assertNotContains(":compileKotlin FROM-CACHE")
            assertContains(":compileJava FROM-CACHE")
        }
    }

    @Test
    fun testKotlinCachingEnabledFlag() = with(Project("simpleProject", GRADLE_VERSION)) {
        prepareLocalBuildCache()

        build("assemble") {
            assertSuccessful()
            assertContains("Packing task ':compileKotlin'")
        }

        build("clean", "assemble", "-Dkotlin.caching.enabled=false") {
            assertSuccessful()
            assertNotContains(":compileKotlin FROM-CACHE")
        }
    }

    @Test
    fun testCacheHitAfterClean() = with(Project("simpleProject", GRADLE_VERSION)) {
        prepareLocalBuildCache()

        build("assemble") {
            assertSuccessful()
            assertContains("Packing task ':compileKotlin'")
        }
        build("clean", "assemble") {
            assertSuccessful()
            assertContains(":compileKotlin FROM-CACHE")
        }
    }

    @Test
    fun testCacheHitAfterCacheHit() = with(Project("simpleProject", GRADLE_VERSION)) {
        prepareLocalBuildCache()

        build("assemble") {
            assertSuccessful()
            // Should store the output into the cache:
            assertContains("Packing task ':compileKotlin'")
        }

        val sourceFile = File(projectDir, "src/main/kotlin/helloWorld.kt")
        val originalSource: String = sourceFile.readText()
        val modifiedSource: String = originalSource.replace(" and ", " + ")
        sourceFile.writeText(modifiedSource)

        build("assemble") {
            assertSuccessful()
            assertContains("Packing task ':compileKotlin'")
        }

        sourceFile.writeText(originalSource)

        build("assemble") {
            assertSuccessful()
            // Should load the output from cache:
            assertContains(":compileKotlin FROM-CACHE")
        }

        sourceFile.writeText(modifiedSource)

        build("assemble") {
            assertSuccessful()
            // And should load the output from cache again, without compilation:
            assertContains(":compileKotlin FROM-CACHE")
        }
    }

    @Test
    fun testCorrectBuildAfterCacheHit() = with(Project("buildCacheSimple", GRADLE_VERSION)) {
        prepareLocalBuildCache()

        // First build, should be stored into the build cache:
        build("assemble") {
            assertSuccessful()
            assertContains("Packing task ':compileKotlin'")
        }

        // A cache hit:
        build("clean", "assemble") {
            assertSuccessful()
            assertContains(":compileKotlin FROM-CACHE")
        }

        // Change the return type of foo() from Int to String in foo.kt, and check that fooUsage.kt is recompiled as well:
        File(projectDir, "src/main/kotlin/foo.kt").modify { it.replace("Int = 1", "String = \"abc\"") }
        build("assemble") {
            assertSuccessful()
            assertCompiledKotlinSources(relativize(allKotlinFiles))
        }
    }

    @Test
    fun testKaptCachingEnabledByDefault() = with(Project("simple", GRADLE_VERSION, directoryPrefix = "kapt2")) {
        prepareLocalBuildCache()

        build("clean", "build") {
            assertSuccessful()
            assertContains("Packing task ':kaptGenerateStubsKotlin'")
            assertContains("Packing task ':kaptKotlin'")
        }

        build("clean", "build") {
            assertSuccessful()
            assertContains(":kaptGenerateStubsKotlin FROM-CACHE")
            assertContains(":kaptKotlin FROM-CACHE")
        }

        File(projectDir, "build.gradle").appendText(
            "\n" + """
            afterEvaluate {
                kaptKotlin.useBuildCache = false
            }
            """.trimIndent()
        )

        build("clean", "build") {
            assertSuccessful()
            assertContains(":kaptGenerateStubsKotlin FROM-CACHE")
            assertNotContains(":kaptKotlin FROM-CACHE")
            assertContains("Caching disabled for task ':kaptKotlin': 'Caching is disabled for kapt")
        }
    }
}

fun BaseGradleIT.Project.prepareLocalBuildCache(directory: File = File(projectDir.parentFile, "buildCache").apply { mkdir() }): File {
    if (!projectDir.exists()) {
        setupWorkingDir()
    }
    File(projectDir, "settings.gradle").appendText("\nbuildCache.local.directory = '${directory.absolutePath.replace("\\", "/")}'")
    return directory
}