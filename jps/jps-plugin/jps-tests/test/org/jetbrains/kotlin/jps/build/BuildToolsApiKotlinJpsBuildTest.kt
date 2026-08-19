/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.build

import com.intellij.util.PathUtilRt
import org.jetbrains.jps.incremental.messages.BuildMessage
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.jps.util.JpsPathUtil
import org.jetbrains.kotlin.compilerRunner.btapi.JpsBuildToolsApiCompilerRunner
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.config.KotlinFacetSettings
import org.jetbrains.kotlin.jps.model.JpsKotlinFacetModuleExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.io.File

/**
 * Covers the Build Tools API path of [org.jetbrains.kotlin.jps.targets.KotlinJvmModuleBuildTarget.compileModuleChunk].
 *
 * The path supports Kotlin-only, non-multiplatform modules; each unsupported case is covered as a failure.
 *
 * Incremental compilation is run by the compiler rather than by JPS, so the tests that cover it assert on which class
 * files were rewritten rather than on what JPS handed to the seam; see [markOutputAsNotRewritten].
 */
class BuildToolsApiKotlinJpsBuildTest : AbstractKotlinJpsBuildTestCase() {
    @BeforeEach
    override fun setUp(testInfo: TestInfo) {
        super.setUp(testInfo)
        workDir = KotlinTestUtils.tmpDirForTest(testInfo)
    }

    private fun withBuildToolsApi(incremental: Boolean = false, verbose: Boolean = false, fn: () -> Unit) {
        withSystemProperty(JpsBuildToolsApiCompilerRunner.USE_BUILD_TOOLS_API_PROPERTY, "true") {
            withSystemProperty(JpsBuildToolsApiCompilerRunner.VERBOSE_PROPERTY, verbose.toString()) {
                withSystemProperty(IncrementalCompilation.INCREMENTAL_COMPILATION_JVM_PROPERTY, incremental.toString(), fn)
            }
        }
    }

    @Test
    fun testKotlinOnlyModuleIsCompiled() = withBuildToolsApi {
        val file = createFile(
            "m1/K.kt",
            """
                package m1

                class K {
                    fun greet() = "hello"
                }
            """
        )
        addModule("m1", PathUtilRt.getParentPath(file))
        addKotlinStdlibDependency()

        rebuildAllModules()

        val outputClass = File(workDir, "out/production/m1/m1/K.class")
        assertTrue(outputClass.exists(), "Expected $outputClass to be generated")
    }

    /**
     * Deliberately references a JDK class rather than only Kotlin builtins. Builtins are served from the stdlib's own
     * metadata, so a module that uses nothing else compiles even when no JDK reached the compiler at all — which is
     * exactly the failure `-jdk-home` versus `-no-jdk` produces, and exactly what every other test here would miss.
     * On a JDK 9+ SDK the modular JDK root is the only channel available, see `JpsBtaCompilationUnit.modularJdkRoot`.
     */
    @Test
    fun testModuleUsingJdkClassesIsCompiled() = withBuildToolsApi {
        val file = createFile(
            "m1/K.kt",
            """
                package m1

                fun ids(): java.util.ArrayList<java.lang.Integer> = java.util.ArrayList()
            """
        )
        addModule("m1", PathUtilRt.getParentPath(file))
        addKotlinStdlibDependency()

        buildAllModules().assertSuccessful()

        val outputClass = File(workDir, "out/production/m1/m1/KKt.class")
        assertTrue(outputClass.exists(), "Expected $outputClass to be generated")
    }

    /**
     * The real test of the `Severity.OUTPUT` bridge: JPS can only delete the stale class file if it received correct
     * source to output mappings through [org.jetbrains.kotlin.compilerRunner.btapi.JpsCompilerMessageRendererBridge].
     */
    @Test
    fun testOutputOfARemovedSourceIsCleanedUp() = withBuildToolsApi {
        val kept = createFile("m1/Kept.kt", "package m1\n\nclass Kept")
        val removed = createFile("m1/Removed.kt", "package m1\n\nclass Removed")
        addModule("m1", PathUtilRt.getParentPath(kept))
        addKotlinStdlibDependency()

        rebuildAllModules()
        val removedOutput = File(workDir, "out/production/m1/m1/Removed.class")
        assertTrue(removedOutput.exists(), "Expected $removedOutput to be generated")

        assertTrue(File(removed).delete(), "Could not delete $removed")
        buildAllModules().assertSuccessful()

        assertTrue(File(workDir, "out/production/m1/m1/Kept.class").exists(), "The kept class should survive")
        assertTrue(!removedOutput.exists(), "Expected $removedOutput to be deleted along with its source")
    }

    /**
     * The progress lines have to be `INFO`, which JPS turns into a `BuildMessage.Kind.INFO` shown in the *Build* tool
     * window. Reporting them through the logger instead would leave the build output silent about which path ran.
     */
    @Test
    fun testProgressIsReportedToTheBuildOutput() = withBuildToolsApi {
        val file = createFile("m1/K.kt", "package m1\n\nclass K")
        addModule("m1", PathUtilRt.getParentPath(file))
        addKotlinStdlibDependency()

        val result = buildAllModules()
        result.assertSuccessful()

        val info = result.getMessages(BuildMessage.Kind.INFO).map { it.messageText }
        assertTrue(info.any { it.contains("[Build Tools API] Build session started") }, "Actual messages: $info")
        assertTrue(info.any { it.contains("[Build Tools API] Compiling 'm1'") }, "Actual messages: $info")
        assertTrue(info.any { it.contains("[Build Tools API] Compiled 'm1'") }, "Actual messages: $info")
    }

    /**
     * The equivalent of Gradle's `--info`/`--debug`: with [JpsBuildToolsApiCompilerRunner.VERBOSE_PROPERTY] set, the
     * detail that normally only reaches the build process log is reported to the *Build* tool window as well.
     */
    @Test
    fun testVerboseModeReportsCompilationDetailToTheBuildOutput() = withBuildToolsApi(verbose = true) {
        val file = createFile("m1/K.kt", "package m1\n\nclass K")
        addModule("m1", PathUtilRt.getParentPath(file))
        addKotlinStdlibDependency()

        val result = buildAllModules()
        result.assertSuccessful()

        val info = result.getMessages(BuildMessage.Kind.INFO).map { it.messageText }
        assertTrue(info.any { it.contains("[m1] sources:") }, "Actual messages: $info")
        assertTrue(info.any { it.contains("[m1] classpath:") }, "Actual messages: $info")
        assertTrue(info.any { it.contains("[m1] arguments:") }, "Actual messages: $info")
    }

    /**
     * The counterpart of [testVerboseModeReportsCompilationDetailToTheBuildOutput]: without the switch the argument
     * strings and the compile set stay out of the build output, which is what keeps an ordinary build readable.
     */
    @Test
    fun testCompilationDetailIsNotReportedByDefault() = withBuildToolsApi {
        val file = createFile("m1/K.kt", "package m1\n\nclass K")
        addModule("m1", PathUtilRt.getParentPath(file))
        addKotlinStdlibDependency()

        val result = buildAllModules()
        result.assertSuccessful()

        val info = result.getMessages(BuildMessage.Kind.INFO).map { it.messageText }
        assertTrue(info.none { it.contains("[m1] arguments:") }, "Actual messages: $info")
        assertTrue(info.any { it.contains("[Build Tools API] Compiled 'm1'") }, "Actual messages: $info")
    }

    @Test
    fun testCompilationErrorFailsTheBuild() = withBuildToolsApi {
        val file = createFile("m1/K.kt", "package m1\n\nval broken: Int = \"not an int\"")
        addModule("m1", PathUtilRt.getParentPath(file))
        addKotlinStdlibDependency()

        val result = buildAllModules()
        result.assertFailed()

        val errors = result.getMessages(BuildMessage.Kind.ERROR)
        assertTrue(errors.any { it.messageText.contains("Initializer type mismatch") }, "Actual errors: $errors")
    }

    @Test
    fun testCircularDependencyIsRejected() = withBuildToolsApi {
        val aFile = createFile("m1/A.kt", "package m1\n\nclass A")
        val bFile = createFile("m2/B.kt", "package m2\n\nclass B")
        val a = addModule("m1", PathUtilRt.getParentPath(aFile))
        val b = addModule("m2", PathUtilRt.getParentPath(bFile))
        JpsJavaExtensionService.getInstance().getOrCreateDependencyExtension(b.dependenciesList.addModuleDependency(a))
        JpsJavaExtensionService.getInstance().getOrCreateDependencyExtension(a.dependenciesList.addModuleDependency(b))
        addKotlinStdlibDependency()

        val result = buildAllModules()
        result.assertFailed()

        val errors = result.getMessages(BuildMessage.Kind.ERROR)
        assertTrue(
            errors.any { it.messageText.contains("does not support circular module dependencies") },
            "Actual errors: $errors"
        )
    }

    @Test
    fun testMultiplatformModuleIsRejected() = withBuildToolsApi {
        // Plain declarations rather than `expect`/`actual`: the guard fires on sources being included from another
        // module, and the common module is built as a target of its own before `m1` is reached.
        val commonFile = createFile("common/Common.kt", "package common\n\nclass Shared")
        val platformFile = createFile("m1/Platform.kt", "package common\n\nfun use() = Shared()")
        val common = addModule("common", PathUtilRt.getParentPath(commonFile))
        val m1 = addModule("m1", PathUtilRt.getParentPath(platformFile))
        JpsJavaExtensionService.getInstance().getOrCreateDependencyExtension(m1.dependenciesList.addModuleDependency(common))
        // Makes JPS include the sources of `common` into the `m1` target, which is what marks them cross-compiled.
        m1.container.setChild(
            JpsKotlinFacetModuleExtension.KIND,
            JpsKotlinFacetModuleExtension(KotlinFacetSettings().apply { implementedModuleNames = listOf("common") })
        )
        addKotlinStdlibDependency()

        val result = buildAllModules()
        result.assertFailed()

        val errors = result.getMessages(BuildMessage.Kind.ERROR)
        assertTrue(
            errors.any { it.messageText.contains("does not support multiplatform modules") },
            "Actual errors: $errors"
        )
    }

    @Test
    fun testModuleDependencyIsRejectedWhenIncremental() = withBuildToolsApi(incremental = true) {
        val libFile = createFile("lib/L.kt", "package lib\n\nclass L")
        val appFile = createFile("m1/K.kt", "package m1\n\nclass K")
        val lib = addModule("lib", PathUtilRt.getParentPath(libFile))
        val m1 = addModule("m1", PathUtilRt.getParentPath(appFile))
        JpsJavaExtensionService.getInstance().getOrCreateDependencyExtension(m1.dependenciesList.addModuleDependency(lib))
        addKotlinStdlibDependency()

        val result = buildAllModules()
        result.assertFailed()

        val errors = result.getMessages(BuildMessage.Kind.ERROR)
        assertTrue(
            errors.any { it.messageText.contains("does not support incremental compilation of a module that depends") },
            "Actual errors: $errors"
        )
    }

    /**
     * Module dependencies are only rejected when incremental compilation is on, so this is the shape the spike is
     * actually aimed at. It is also the only test that puts another target's output directory on the classpath, which
     * is what `findClassPathRoots() - chunk.targets.map { it.outputDir }` has to leave alone.
     */
    @Test
    fun testModuleIsCompiledAgainstTheOutputOfItsDependency() = withBuildToolsApi {
        val libFile = createFile("lib/L.kt", "package lib\n\nclass L {\n    fun answer() = 42\n}")
        val appFile = createFile("m1/K.kt", "package m1\n\nimport lib.L\n\nfun use() = L().answer()")
        val lib = addModule("lib", PathUtilRt.getParentPath(libFile))
        val m1 = addModule("m1", PathUtilRt.getParentPath(appFile))
        JpsJavaExtensionService.getInstance().getOrCreateDependencyExtension(m1.dependenciesList.addModuleDependency(lib))
        addKotlinStdlibDependency()

        buildAllModules().assertSuccessful()

        assertTrue(File(workDir, "out/production/lib/lib/L.class").exists(), "Expected 'lib' to be compiled")
        assertTrue(File(workDir, "out/production/m1/m1/KKt.class").exists(), "Expected 'm1' to be compiled")
    }

    /**
     * The one thing `friendDirs` buys: a test target may see `internal` declarations of the production target it
     * belongs to. Without `-Xfriend-paths` this fails to resolve rather than producing different output, so the
     * assertion is that the build succeeds at all.
     */
    @Test
    fun testTestSourcesSeeInternalDeclarationsOfProduction() = withBuildToolsApi {
        val productionFile = createFile("m1/src/Internal.kt", "package m1\n\ninternal fun secret() = 42")
        val testFile = createFile("m1/test/UsesInternal.kt", "package m1\n\nfun useSecret() = secret()")
        val m1 = addModule("m1", PathUtilRt.getParentPath(productionFile))

        val testRoot = PathUtilRt.getParentPath(testFile)
        m1.contentRootsList.addUrl(JpsPathUtil.pathToUrl(testRoot))
        m1.addSourceRoot(JpsPathUtil.pathToUrl(testRoot), JavaSourceRootType.TEST_SOURCE)
        // Without an output of its own the test target would share the production one, and the classpath subtraction
        // would then drop the very directory the friend paths point at.
        JpsJavaExtensionService.getInstance().getOrCreateModuleExtension(m1).testOutputUrl =
            JpsPathUtil.pathToUrl(File(workDir, "out/test/m1").absolutePath)
        addKotlinStdlibDependency()

        buildAllModules().assertSuccessful()

        assertTrue(File(workDir, "out/production/m1/m1/InternalKt.class").exists(), "Expected production to be compiled")
        assertTrue(File(workDir, "out/test/m1/m1/UsesInternalKt.class").exists(), "Expected tests to be compiled")
    }

    /**
     * The heart of it: the compiler, not JPS, decides the compile set, and it leaves alone what did not change.
     */
    @Test
    fun testUnrelatedFileIsNotRecompiled() = withBuildToolsApi(incremental = true) {
        val changed = createFile("m1/Changed.kt", "package m1\n\nclass Changed")
        createFile("m1/Untouched.kt", "package m1\n\nclass Untouched")
        addModule("m1", PathUtilRt.getParentPath(changed))
        addKotlinStdlibDependency()

        buildAllModules().assertSuccessful()
        val changedOutput = markOutputAsNotRewritten("Changed.class")
        val untouchedOutput = markOutputAsNotRewritten("Untouched.class")

        change(changed, "package m1\n\nclass Changed { fun added() = 1 }")
        buildAllModules().assertSuccessful()

        assertRewritten(changedOutput)
        assertNotRewritten(untouchedOutput)
    }

    /**
     * The other half: the compiler expands the set JPS gave it. JPS only knows `A.kt` changed; recompiling `B.kt` too
     * is a decision nothing on the JPS side made.
     */
    @Test
    fun testDependentFileIsRecompiled() = withBuildToolsApi(incremental = true) {
        val a = createFile("m1/A.kt", "package m1\n\nfun a(): Int = 1")
        createFile("m1/B.kt", "package m1\n\nfun b() = a()")
        addModule("m1", PathUtilRt.getParentPath(a))
        addKotlinStdlibDependency()

        buildAllModules().assertSuccessful()
        val aOutput = markOutputAsNotRewritten("AKt.class")
        val bOutput = markOutputAsNotRewritten("BKt.class")

        change(a, "package m1\n\nfun a(): String = \"1\"")
        buildAllModules().assertSuccessful()

        assertRewritten(aOutput)
        assertRewritten(bOutput)
    }

    @Test
    fun testOutputOfARemovedSourceIsCleanedUpIncrementally() = withBuildToolsApi(incremental = true) {
        val kept = createFile("m1/Kept.kt", "package m1\n\nclass Kept")
        val removed = createFile("m1/Removed.kt", "package m1\n\nclass Removed")
        addModule("m1", PathUtilRt.getParentPath(kept))
        addKotlinStdlibDependency()

        buildAllModules().assertSuccessful()
        val removedOutput = File(workDir, "out/production/m1/m1/Removed.class")
        assertTrue(removedOutput.exists(), "Expected $removedOutput to be generated")

        assertTrue(File(removed).delete(), "Could not delete $removed")
        buildAllModules().assertSuccessful()

        assertTrue(File(workDir, "out/production/m1/m1/Kept.class").exists(), "The kept class should survive")
        assertTrue(!removedOutput.exists(), "Expected $removedOutput to be deleted along with its source")
    }

    /**
     * A JPS rebuild has to reach the compiler as well, or its caches would survive the very action meant to discard
     * everything.
     */
    @Test
    fun testRebuildRecompilesEverything() = withBuildToolsApi(incremental = true) {
        val changed = createFile("m1/Changed.kt", "package m1\n\nclass Changed")
        createFile("m1/Untouched.kt", "package m1\n\nclass Untouched")
        addModule("m1", PathUtilRt.getParentPath(changed))
        addKotlinStdlibDependency()

        buildAllModules().assertSuccessful()
        change(changed, "package m1\n\nclass Changed { fun added() = 1 }")
        buildAllModules().assertSuccessful()

        val changedOutput = markOutputAsNotRewritten("Changed.class")
        val untouchedOutput = markOutputAsNotRewritten("Untouched.class")

        rebuildAllModules()

        assertRewritten(changedOutput)
        assertRewritten(untouchedOutput)
    }

    /**
     * Class file timestamps are what incrementality is asserted on, rather than the compiler's own progress line: one
     * build can run several rounds, because JPS\'s Java dependency graph may mark more files dirty after a round, and
     * each round is a separate compilation with its own line. `assertCompiled` is no help either — it observes what
     * JPS marked dirty, not what the compiler chose to recompile.
     *
     * Zero is used as the "not rewritten" marker instead of remembering the previous timestamp, because a filesystem
     * may only keep timestamps to the nearest second.
     */
    private fun markOutputAsNotRewritten(className: String): File {
        val output = File(workDir, "out/production/m1/m1/$className")
        assertTrue(output.exists(), "Expected $output to be generated")
        assertTrue(output.setLastModified(0L), "Could not reset the timestamp of $output")
        return output
    }

    private fun assertRewritten(output: File) =
        assertTrue(output.lastModified() > 0L, "Expected $output to have been recompiled")

    private fun assertNotRewritten(output: File) =
        assertEquals(0L, output.lastModified(), "Expected $output not to have been recompiled")
}
