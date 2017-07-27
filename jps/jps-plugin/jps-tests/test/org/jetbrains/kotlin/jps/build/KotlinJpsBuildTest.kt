/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.jps.build

import com.google.common.collect.Lists
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtil.toSystemIndependentName
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.UsefulTestCase
import com.intellij.util.ArrayUtil
import com.intellij.util.io.URLUtil
import com.intellij.util.io.ZipUtil
import org.jetbrains.jps.ModuleChunk
import org.jetbrains.jps.api.CanceledStatus
import org.jetbrains.jps.builders.BuildResult
import org.jetbrains.jps.builders.CompileScopeTestBuilder
import org.jetbrains.jps.builders.JpsBuildTestCase
import org.jetbrains.jps.builders.TestProjectBuilderLogger
import org.jetbrains.jps.builders.impl.BuildDataPathsImpl
import org.jetbrains.jps.builders.logging.BuildLoggingManager
import org.jetbrains.jps.cmdline.ProjectDescriptor
import org.jetbrains.jps.incremental.BuilderRegistry
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.jps.incremental.IncProjectBuilder
import org.jetbrains.jps.incremental.ModuleLevelBuilder
import org.jetbrains.jps.incremental.messages.BuildMessage
import org.jetbrains.jps.incremental.messages.CompilerMessage
import org.jetbrains.jps.model.JpsModuleRootModificationUtil
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.java.JpsJavaDependencyScope
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.jps.model.java.JpsJavaSdkType
import org.jetbrains.jps.model.library.JpsOrderRootType
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.jps.util.JpsPathUtil
import org.jetbrains.kotlin.cli.common.Usage
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.JvmCodegenUtil
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.config.KotlinCompilerVersion.TEST_IS_PRE_RELEASE_SYSTEM_PROPERTY
import org.jetbrains.kotlin.incremental.CacheVersion
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.withIC
import org.jetbrains.kotlin.jps.build.KotlinJpsBuildTest.LibraryDependency.*
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.MockLibraryUtil
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.junit.Assert
import java.io.*
import java.net.URLClassLoader
import java.util.*
import java.util.regex.Pattern
import java.util.zip.ZipOutputStream

class KotlinJpsBuildTestIncremental : KotlinJpsBuildTest() {
    var isICEnabledBackup: Boolean = false

    override fun setUp() {
        super.setUp()
        isICEnabledBackup = IncrementalCompilation.isEnabled()
        IncrementalCompilation.setIsEnabled(true)
    }

    override fun tearDown() {
        IncrementalCompilation.setIsEnabled(isICEnabledBackup)
        super.tearDown()
    }

    fun testManyFiles() {
        doTest()

        val module = myProject.modules.get(0)
        assertFilesExistInOutput(module, "foo/MainKt.class", "boo/BooKt.class", "foo/Bar.class")

        checkWhen(touch("src/main.kt"), null, packageClasses("kotlinProject", "src/main.kt", "foo.MainKt"))
        checkWhen(touch("src/boo.kt"), null, packageClasses("kotlinProject", "src/boo.kt", "boo.BooKt"))
        checkWhen(touch("src/Bar.kt"), arrayOf("src/Bar.kt"), arrayOf(klass("kotlinProject", "foo.Bar")))

        checkWhen(del("src/main.kt"),
                  pathsToCompile = null,
                  pathsToDelete = packageClasses("kotlinProject", "src/main.kt", "foo.MainKt"))
        assertFilesExistInOutput(module, "boo/BooKt.class", "foo/Bar.class")
        assertFilesNotExistInOutput(module, "foo/MainKt.class")

        checkWhen(touch("src/boo.kt"), null, packageClasses("kotlinProject", "src/boo.kt", "boo.BooKt"))
        checkWhen(touch("src/Bar.kt"), null, arrayOf(klass("kotlinProject", "foo.Bar")))
    }

    fun testManyFilesForPackage() {
        doTest()

        val module = myProject.modules.get(0)
        assertFilesExistInOutput(module, "foo/MainKt.class", "boo/BooKt.class", "foo/Bar.class")

        checkWhen(touch("src/main.kt"), null, packageClasses("kotlinProject", "src/main.kt", "foo.MainKt"))
        checkWhen(touch("src/boo.kt"), null, packageClasses("kotlinProject", "src/boo.kt", "boo.BooKt"))
        checkWhen(touch("src/Bar.kt"),
                  arrayOf("src/Bar.kt"),
                  arrayOf(klass("kotlinProject", "foo.Bar"),
                          packagePartClass("kotlinProject", "src/Bar.kt", "foo.MainKt"),
                          module("kotlinProject")))

        checkWhen(del("src/main.kt"),
                  pathsToCompile = null,
                  pathsToDelete = packageClasses("kotlinProject", "src/main.kt", "foo.MainKt"))
        assertFilesExistInOutput(module, "boo/BooKt.class", "foo/Bar.class")

        checkWhen(touch("src/boo.kt"), null, packageClasses("kotlinProject", "src/boo.kt", "boo.BooKt"))
        checkWhen(touch("src/Bar.kt"), null,
                  arrayOf(klass("kotlinProject", "foo.Bar"),
                          packagePartClass("kotlinProject", "src/Bar.kt", "foo.MainKt"),
                          module("kotlinProject")))
    }
}

open class KotlinJpsBuildTest : AbstractKotlinJpsBuildTestCase() {
    companion object {
        private val PROJECT_NAME = "kotlinProject"
        private val ADDITIONAL_MODULE_NAME = "module2"
        private val JDK_NAME = "IDEA_JDK"

        private val EXCLUDE_FILES = arrayOf("Excluded.class", "YetAnotherExcluded.class")
        private val NOTHING = arrayOf<String>()
        private val KOTLIN_JS_LIBRARY = "jslib-example"
        private val PATH_TO_KOTLIN_JS_LIBRARY = AbstractKotlinJpsBuildTestCase.TEST_DATA_PATH + "general/KotlinJavaScriptProjectWithDirectoryAsLibrary/" + KOTLIN_JS_LIBRARY
        private val KOTLIN_JS_LIBRARY_JAR = "$KOTLIN_JS_LIBRARY.jar"
        private val EXPECTED_JS_FILES_IN_OUTPUT_FOR_STDLIB_ONLY = hashSetOf(
                "$PROJECT_NAME.js",
                "$PROJECT_NAME.meta.js",
                "lib/kotlin.js",
                "lib/kotlin.meta.js",
                "$PROJECT_NAME/root-package.kjsm"
        )
        private val EXPECTED_JS_FILES_IN_OUTPUT_FOR_MODULE_STDLIB_ONLY = hashSetOf(
                "$ADDITIONAL_MODULE_NAME.js",
                "$ADDITIONAL_MODULE_NAME.meta.js",
                "lib/kotlin.js",
                "lib/kotlin.meta.js",
                "$ADDITIONAL_MODULE_NAME/module2/module2.kjsm"
        )
        private val EXPECTED_JS_FILES_IN_OUTPUT_NO_COPY = hashSetOf(
                "$PROJECT_NAME.js",
                "$PROJECT_NAME.meta.js",
                "$PROJECT_NAME/root-package.kjsm"
        )
        private val EXPECTED_JS_FILES_IN_OUTPUT_WITH_ADDITIONAL_LIB_AND_DEFAULT_DIR = hashSetOf(
                "$PROJECT_NAME.js",
                "$PROJECT_NAME.meta.js",
                "lib/kotlin.js",
                "lib/kotlin.meta.js",
                "lib/jslib-example.js",
                "lib/jslib-example.meta.js",
                "lib/file0.js",
                "lib/dir/file1.js",
                "lib/META-INF-ex/file2.js",
                "lib/res0.js",
                "lib/resdir/res1.js",
                "$PROJECT_NAME/root-package.kjsm"
        )
        private val EXPECTED_JS_FILES_IN_OUTPUT_WITH_ADDITIONAL_LIB_AND_CUSTOM_DIR = hashSetOf(
                "$PROJECT_NAME.js",
                "$PROJECT_NAME.meta.js",
                "custom/kotlin.js",
                "custom/kotlin.meta.js",
                "custom/jslib-example.js",
                "custom/jslib-example.meta.js",
                "custom/file0.js",
                "custom/dir/file1.js",
                "custom/META-INF-ex/file2.js",
                "custom/res0.js",
                "custom/resdir/res1.js",
                "$PROJECT_NAME/root-package.kjsm"
        )

        private fun k2jsOutput(vararg moduleNames: String): Array<String> {
            val list = arrayListOf<String>()
            for (moduleName in moduleNames) {
                val outputDir = File("out/production/$moduleName")
                list.add(toSystemIndependentName(JpsJsModuleUtils.getOutputFile(outputDir, moduleName, false).path))
                list.add(toSystemIndependentName(JpsJsModuleUtils.getOutputMetaFile(outputDir, moduleName, false).path))
            }
            return list.toTypedArray()
        }

        private fun getMethodsOfClass(classFile: File): Set<String> {
            val result = TreeSet<String>()
            ClassReader(FileUtil.loadFileBytes(classFile)).accept(object : ClassVisitor(Opcodes.ASM5) {
                override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<String>?): MethodVisitor? {
                    result.add(name)
                    return null
                }
            }, 0)
            return result
        }

        @JvmStatic
        protected fun assertFilesExistInOutput(module: JpsModule, vararg relativePaths: String) {
            for (path in relativePaths) {
                val outputFile = findFileInOutputDir(module, path)
                assertTrue("Output not written: " + outputFile.absolutePath + "\n Directory contents: \n" + dirContents(outputFile.parentFile), outputFile.exists())
            }
        }

        private fun findFileInOutputDir(module: JpsModule, relativePath: String): File {
            val outputUrl = JpsJavaExtensionService.getInstance().getOutputUrl(module, false)
            assertNotNull(outputUrl)
            val outputDir = File(JpsPathUtil.urlToPath(outputUrl))
            return File(outputDir, relativePath)
        }


        @JvmStatic
        protected fun assertFilesNotExistInOutput(module: JpsModule, vararg relativePaths: String) {
            val outputUrl = JpsJavaExtensionService.getInstance().getOutputUrl(module, false)
            assertNotNull(outputUrl)
            val outputDir = File(JpsPathUtil.urlToPath(outputUrl))
            for (path in relativePaths) {
                val outputFile = File(outputDir, path)
                assertFalse("Output directory \"" + outputFile.absolutePath + "\" contains \"" + path + "\"", outputFile.exists())
            }
        }

        private fun dirContents(dir: File): String {
            val files = dir.listFiles() ?: return "<not found>"
            val builder = StringBuilder()
            for (file in files) {
                builder.append(" * ").append(file.name).append("\n")
            }
            return builder.toString()
        }

        @JvmStatic
        protected fun klass(moduleName: String, classFqName: String): String {
            val outputDirPrefix = "out/production/$moduleName/"
            return outputDirPrefix + classFqName.replace('.', '/') + ".class"
        }

        @JvmStatic
        protected fun module(moduleName: String): String {
            return "out/production/$moduleName/${JvmCodegenUtil.getMappingFileName(moduleName)}"
        }
    }

    annotation class WorkingDir(val name: String)

    enum class LibraryDependency {
        NONE,
        JVM_MOCK_RUNTIME,
        JVM_FULL_RUNTIME,
        JS_STDLIB,
    }

    override fun setUp() {
        super.setUp()
        val currentTestMethod = this::class.members.firstOrNull { it.name == "test" + getTestName(false) }
        val workingDirFromAnnotation = currentTestMethod?.annotations?.filterIsInstance<WorkingDir>()?.firstOrNull()?.name
        val sourceFilesRoot = File(AbstractKotlinJpsBuildTestCase.TEST_DATA_PATH + "general/" + (workingDirFromAnnotation ?: getTestName(false)))
        workDir = AbstractKotlinJpsBuildTestCase.copyTestDataToTmpDir(sourceFilesRoot)
        orCreateProjectDir

        JpsUtils.resetCaches()
    }

    override fun tearDown() {
        FileUtil.delete(workDir)
        super.tearDown()
    }

    override fun doGetProjectDir(): File = workDir

    private fun initProject(libraryDependency: LibraryDependency = NONE) {
        addJdk(JDK_NAME)
        loadProject(workDir.absolutePath + File.separator + PROJECT_NAME + ".ipr")

        when (libraryDependency) {
            NONE -> {}
            JVM_MOCK_RUNTIME -> addKotlinMockRuntimeDependency()
            JVM_FULL_RUNTIME -> addKotlinStdlibDependency()
            JS_STDLIB -> addKotlinJavaScriptStdlibDependency()
        }
    }

    fun doTest() {
        initProject(JVM_MOCK_RUNTIME)
        buildAllModules().assertSuccessful()
    }

    fun doTestWithRuntime() {
        initProject(JVM_FULL_RUNTIME)
        buildAllModules().assertSuccessful()
    }

    fun doTestWithKotlinJavaScriptLibrary() {
        initProject(JS_STDLIB)
        createKotlinJavaScriptLibraryArchive()
        addDependency(KOTLIN_JS_LIBRARY, File(workDir, KOTLIN_JS_LIBRARY_JAR))
        buildAllModules().assertSuccessful()
    }

    fun testKotlinProject() {
        doTest()

        checkWhen(touch("src/test1.kt"), null, packageClasses("kotlinProject", "src/test1.kt", "Test1Kt"))
    }

    fun testSourcePackagePrefix() {
        doTest()
    }

    fun testSourcePackageLongPrefix() {
        initProject()
        val buildResult = buildAllModules()
        buildResult.assertSuccessful()
        val warnings = buildResult.getMessages(BuildMessage.Kind.WARNING)
        assertEquals("Warning about invalid package prefix in module 2 is expected: $warnings", 2, warnings.size)
        assertEquals("Invalid package prefix name is ignored: invalid-prefix.test", warnings.first().messageText)
    }

    fun testSourcePackagePrefixWithInnerClasses() {
        initProject()
        buildAllModules().assertSuccessful()
    }

    fun testKotlinJavaScriptProject() {
        initProject(JS_STDLIB)
        buildAllModules().assertSuccessful()

        assertEquals(EXPECTED_JS_FILES_IN_OUTPUT_FOR_STDLIB_ONLY, contentOfOutputDir(PROJECT_NAME))
        checkWhen(touch("src/test1.kt"), null, k2jsOutput(PROJECT_NAME))
    }

    fun testKotlinJavaScriptProjectWithSourceMap() {
        initProject(JS_STDLIB)
        buildAllModules().assertSuccessful()

        val sourceMapContent = File(getOutputDir(PROJECT_NAME), "$PROJECT_NAME.js.map").readText()
        val expectedPath = "prefix-dir/pkg/test1.kt"
        assertTrue("Source map file should contain relative path ($expectedPath)", sourceMapContent.contains("\"$expectedPath\""))

        val librarySourceMapFile = File(getOutputDir(PROJECT_NAME), "lib/kotlin.js.map")
        assertTrue("Source map for stdlib should be copied to $librarySourceMapFile", librarySourceMapFile.exists())
    }

    fun testKotlinJavaScriptProjectWithTwoModules() {
        initProject(JS_STDLIB)
        buildAllModules().assertSuccessful()

        assertEquals(EXPECTED_JS_FILES_IN_OUTPUT_FOR_STDLIB_ONLY, contentOfOutputDir(PROJECT_NAME))
        assertEquals(EXPECTED_JS_FILES_IN_OUTPUT_FOR_MODULE_STDLIB_ONLY, contentOfOutputDir(ADDITIONAL_MODULE_NAME))

        checkWhen(touch("src/test1.kt"), null, k2jsOutput(PROJECT_NAME))
        checkWhen(touch("module2/src/module2.kt"), null, k2jsOutput(ADDITIONAL_MODULE_NAME))
        checkWhen(arrayOf(touch("src/test1.kt"), touch("module2/src/module2.kt")), null, k2jsOutput(PROJECT_NAME, ADDITIONAL_MODULE_NAME))
    }

    @WorkingDir("KotlinJavaScriptProjectWithTwoModules")
    fun testKotlinJavaScriptProjectWithTwoModulesAndWithLibrary() {
        initProject()
        createKotlinJavaScriptLibraryArchive()
        addDependency(KOTLIN_JS_LIBRARY, File(workDir, KOTLIN_JS_LIBRARY_JAR))
        addKotlinJavaScriptStdlibDependency()
        buildAllModules().assertSuccessful()
    }

    fun testKotlinJavaScriptProjectWithDirectoryAsStdlib() {
        initProject()
        val jslibJar = PathUtil.kotlinPathsForDistDirectory.jsStdLibJarPath
        val jslibDir = File(workDir, "KotlinJavaScript")
        try {
            ZipUtil.extract(jslibJar, jslibDir, null)
        }
        catch (ex: IOException) {
            throw IllegalStateException(ex.message)
        }

        addDependency("KotlinJavaScript", jslibDir)
        buildAllModules().assertSuccessful()

        assertEquals(EXPECTED_JS_FILES_IN_OUTPUT_FOR_STDLIB_ONLY, contentOfOutputDir(PROJECT_NAME))
        checkWhen(touch("src/test1.kt"), null, k2jsOutput(PROJECT_NAME))
    }

    fun testKotlinJavaScriptProjectWithDirectoryAsLibrary() {
        initProject(JS_STDLIB)
        addDependency(KOTLIN_JS_LIBRARY, File(workDir, KOTLIN_JS_LIBRARY))
        buildAllModules().assertSuccessful()

        assertEquals(EXPECTED_JS_FILES_IN_OUTPUT_WITH_ADDITIONAL_LIB_AND_DEFAULT_DIR, contentOfOutputDir(PROJECT_NAME))
        checkWhen(touch("src/test1.kt"), null, k2jsOutput(PROJECT_NAME))
    }

    fun testKotlinJavaScriptProjectWithLibrary() {
        doTestWithKotlinJavaScriptLibrary()

        assertEquals(EXPECTED_JS_FILES_IN_OUTPUT_WITH_ADDITIONAL_LIB_AND_DEFAULT_DIR, contentOfOutputDir(PROJECT_NAME))
        checkWhen(touch("src/test1.kt"), null, k2jsOutput(PROJECT_NAME))
    }

    fun testKotlinJavaScriptProjectWithLibraryCustomOutputDir() {
        doTestWithKotlinJavaScriptLibrary()

        assertEquals(EXPECTED_JS_FILES_IN_OUTPUT_WITH_ADDITIONAL_LIB_AND_CUSTOM_DIR, contentOfOutputDir(PROJECT_NAME))
        checkWhen(touch("src/test1.kt"), null, k2jsOutput(PROJECT_NAME))
    }

    fun testKotlinJavaScriptProjectWithLibraryNoCopy() {
        doTestWithKotlinJavaScriptLibrary()

        assertEquals(EXPECTED_JS_FILES_IN_OUTPUT_NO_COPY, contentOfOutputDir(PROJECT_NAME))
        checkWhen(touch("src/test1.kt"), null, k2jsOutput(PROJECT_NAME))
    }

    fun testKotlinJavaScriptProjectWithLibraryAndErrors() {
        initProject(JS_STDLIB)
        createKotlinJavaScriptLibraryArchive()
        addDependency(KOTLIN_JS_LIBRARY, File(workDir, KOTLIN_JS_LIBRARY_JAR))
        buildAllModules().assertFailed()

        assertEquals(Collections.EMPTY_SET, contentOfOutputDir(PROJECT_NAME))
    }

    fun testKotlinJavaScriptProjectWithEmptyDependencies() {
        initProject(JS_STDLIB)
        makeAll().assertSuccessful()
    }

    fun testKotlinJavaScriptInternalFromSpecialRelatedModule() {
        initProject(JS_STDLIB)
        makeAll().assertSuccessful()
    }

    fun testKotlinJavaScriptProjectWithTests() {
        initProject(JS_STDLIB)
        makeAll().assertSuccessful()
    }

    fun testKotlinJavaScriptProjectWithTestsAndSeparateTestAndSrcModuleDependencies() {
        initProject(JS_STDLIB)
        makeAll().assertSuccessful()
    }

    fun testKotlinJavaScriptProjectWithTestsAndTestAndSrcModuleDependency() {
        initProject(JS_STDLIB)
        val buildResult = makeAll()
        buildResult.assertSuccessful()

        val warnings = buildResult.getMessages(BuildMessage.Kind.WARNING)
        assertEquals("Warning about duplicate module definition: $warnings", 0, warnings.size)
    }

    fun testKotlinJavaScriptProjectWithTwoSrcModuleDependency() {
        initProject(JS_STDLIB)
        val buildResult = makeAll()
        buildResult.assertSuccessful()

        val warnings = buildResult.getMessages(BuildMessage.Kind.WARNING)
        assertEquals("Warning about duplicate module definition: $warnings", 0, warnings.size)
    }

    fun testExcludeFolderInSourceRoot() {
        doTest()

        val module = myProject.modules.get(0)
        assertFilesExistInOutput(module, "Foo.class")
        assertFilesNotExistInOutput(module, *EXCLUDE_FILES)

        checkWhen(touch("src/foo.kt"), null, arrayOf(klass("kotlinProject", "Foo")))
    }

    fun testExcludeModuleFolderInSourceRootOfAnotherModule() {
        doTest()

        for (module in myProject.modules) {
            assertFilesExistInOutput(module, "Foo.class")
        }

        checkWhen(touch("src/foo.kt"), null, arrayOf(klass("kotlinProject", "Foo")))
        checkWhen(touch("src/module2/src/foo.kt"), null, arrayOf(klass("module2", "Foo")))
    }

    fun testExcludeFileUsingCompilerSettings() {
        doTest()

        val module = myProject.modules.get(0)
        assertFilesExistInOutput(module, "Foo.class", "Bar.class")
        assertFilesNotExistInOutput(module, *EXCLUDE_FILES)

        checkWhen(touch("src/foo.kt"), null, arrayOf(klass("kotlinProject", "Foo")))
        checkWhen(touch("src/Excluded.kt"), null, NOTHING)
        checkWhen(touch("src/dir/YetAnotherExcluded.kt"), null, NOTHING)
    }

    fun testExcludeFolderNonRecursivelyUsingCompilerSettings() {
        doTest()

        val module = myProject.modules.get(0)
        assertFilesExistInOutput(module, "Foo.class", "Bar.class")
        assertFilesNotExistInOutput(module, *EXCLUDE_FILES)

        checkWhen(touch("src/foo.kt"), null, arrayOf(klass("kotlinProject", "Foo")))
        checkWhen(touch("src/dir/subdir/bar.kt"), null, arrayOf(klass("kotlinProject", "Bar")))

        checkWhen(touch("src/dir/Excluded.kt"), null, NOTHING)
        checkWhen(touch("src/dir/subdir/YetAnotherExcluded.kt"), null, NOTHING)
    }

    fun testExcludeFolderRecursivelyUsingCompilerSettings() {
        doTest()

        val module = myProject.modules.get(0)
        assertFilesExistInOutput(module, "Foo.class", "Bar.class")
        assertFilesNotExistInOutput(module, *EXCLUDE_FILES)

        checkWhen(touch("src/foo.kt"), null, arrayOf(klass("kotlinProject", "Foo")))

        checkWhen(touch("src/exclude/Excluded.kt"), null, NOTHING)
        checkWhen(touch("src/exclude/YetAnotherExcluded.kt"), null, NOTHING)
        checkWhen(touch("src/exclude/subdir/Excluded.kt"), null, NOTHING)
        checkWhen(touch("src/exclude/subdir/YetAnotherExcluded.kt"), null, NOTHING)
    }

    fun testKotlinProjectTwoFilesInOnePackage() {
        doTest()

        checkWhen(touch("src/test1.kt"), null, packageClasses("kotlinProject", "src/test1.kt", "_DefaultPackage"))
        checkWhen(touch("src/test2.kt"), null, packageClasses("kotlinProject", "src/test2.kt", "_DefaultPackage"))

        checkWhen(arrayOf(del("src/test1.kt"), del("src/test2.kt")), NOTHING,
                  arrayOf(packagePartClass("kotlinProject", "src/test1.kt", "_DefaultPackage"),
                          packagePartClass("kotlinProject", "src/test2.kt", "_DefaultPackage"),
                          module("kotlinProject")))

        assertFilesNotExistInOutput(myProject.modules.get(0), "_DefaultPackage.class")
    }

    fun testKotlinJavaProject() {
        doTestWithRuntime()
    }

    fun testJKJProject() {
        doTestWithRuntime()
    }

    fun testKJKProject() {
        doTestWithRuntime()
    }

    fun testKJCircularProject() {
        doTestWithRuntime()
    }

    fun testJKJInheritanceProject() {
        doTestWithRuntime()
    }

    fun testKJKInheritanceProject() {
        doTestWithRuntime()
    }

    fun testCircularDependenciesNoKotlinFiles() {
        doTest()
    }

    fun testCircularDependenciesDifferentPackages() {
        initProject(JVM_MOCK_RUNTIME)
        val result = buildAllModules()

        // Check that outputs are located properly
        assertFilesExistInOutput(findModule("module2"), "kt1/Kt1Kt.class")
        assertFilesExistInOutput(findModule("kotlinProject"), "kt2/Kt2Kt.class")

        result.assertSuccessful()

        checkWhen(touch("src/kt2.kt"), null, packageClasses("kotlinProject", "src/kt2.kt", "kt2.Kt2Kt"))
        checkWhen(touch("module2/src/kt1.kt"), null, packageClasses("module2", "module2/src/kt1.kt", "kt1.Kt1Kt"))
    }

    fun testCircularDependenciesSamePackage() {
        initProject(JVM_MOCK_RUNTIME)
        val result = buildAllModules()
        result.assertSuccessful()

        // Check that outputs are located properly
        val facadeWithA = findFileInOutputDir(findModule("module1"), "test/AKt.class")
        val facadeWithB = findFileInOutputDir(findModule("module2"), "test/BKt.class")
        UsefulTestCase.assertSameElements(getMethodsOfClass(facadeWithA), "<clinit>", "a", "getA")
        UsefulTestCase.assertSameElements(getMethodsOfClass(facadeWithB), "<clinit>", "b", "getB", "setB")

        checkWhen(touch("module1/src/a.kt"), null, packageClasses("module1", "module1/src/a.kt", "test.TestPackage"))
        checkWhen(touch("module2/src/b.kt"), null, packageClasses("module2", "module2/src/b.kt", "test.TestPackage"))
    }

    fun testCircularDependenciesSamePackageWithTests() {
        initProject(JVM_MOCK_RUNTIME)
        val result = buildAllModules()
        result.assertSuccessful()

        // Check that outputs are located properly
        val facadeWithA = findFileInOutputDir(findModule("module1"), "test/AKt.class")
        val facadeWithB = findFileInOutputDir(findModule("module2"), "test/BKt.class")
        UsefulTestCase.assertSameElements(getMethodsOfClass(facadeWithA), "<clinit>", "a", "funA", "getA")
        UsefulTestCase.assertSameElements(getMethodsOfClass(facadeWithB), "<clinit>", "b", "funB", "getB", "setB")

        checkWhen(touch("module1/src/a.kt"), null, packageClasses("module1", "module1/src/a.kt", "test.TestPackage"))
        checkWhen(touch("module2/src/b.kt"), null, packageClasses("module2", "module2/src/b.kt", "test.TestPackage"))
    }

    fun testInternalFromAnotherModule() {
        initProject(JVM_MOCK_RUNTIME)
        val result = buildAllModules()
        result.assertFailed()
        result.checkErrors()
    }

    fun testInternalFromSpecialRelatedModule() {
        initProject(JVM_MOCK_RUNTIME)
        buildAllModules().assertSuccessful()

        val classpath = listOf("out/production/module1", "out/test/module2").map { File(workDir, it).toURI().toURL() }.toTypedArray()
        val clazz = URLClassLoader(classpath).loadClass("test2.BarKt")
        clazz.getMethod("box").invoke(null)
    }

    fun testCircularDependenciesInternalFromAnotherModule() {
        initProject(JVM_MOCK_RUNTIME)
        val result = buildAllModules()
        result.assertFailed()
        result.checkErrors()
    }

    fun testCircularDependenciesWrongInternalFromTests() {
        initProject(JVM_MOCK_RUNTIME)
        val result = buildAllModules()
        result.assertFailed()
        result.checkErrors()
    }

    fun testCircularDependencyWithReferenceToOldVersionLib() {
        initProject(JVM_MOCK_RUNTIME)

        val libraryJar = MockLibraryUtil.compileJvmLibraryToJar(workDir.absolutePath + File.separator + "oldModuleLib/src", "module-lib")

        AbstractKotlinJpsBuildTestCase.addDependency(JpsJavaDependencyScope.COMPILE, Lists.newArrayList(findModule("module1"), findModule("module2")), false, "module-lib", libraryJar)

        val result = buildAllModules()
        result.assertSuccessful()
    }

    fun testDependencyToOldKotlinLib() {
        initProject()

        val libraryJar = MockLibraryUtil.compileJvmLibraryToJar(workDir.absolutePath + File.separator + "oldModuleLib/src", "module-lib")

        AbstractKotlinJpsBuildTestCase.addDependency(JpsJavaDependencyScope.COMPILE, Lists.newArrayList(findModule("module")), false, "module-lib", libraryJar)

        addKotlinStdlibDependency()

        val result = buildAllModules()
        result.assertSuccessful()
    }

    fun testAccessToInternalInProductionFromTests() {
        initProject(JVM_MOCK_RUNTIME)
        val result = buildAllModules()
        result.assertSuccessful()
    }

    private fun createKotlinJavaScriptLibraryArchive() {
        val jarFile = File(workDir, KOTLIN_JS_LIBRARY_JAR)
        try {
            val zip = ZipOutputStream(FileOutputStream(jarFile))
            ZipUtil.addDirToZipRecursively(zip, jarFile, File(PATH_TO_KOTLIN_JS_LIBRARY), "", null, null)
            zip.close()
        }
        catch (ex: FileNotFoundException) {
            throw IllegalStateException(ex.message)
        }
        catch (ex: IOException) {
            throw IllegalStateException(ex.message)
        }

    }

    private fun contentOfOutputDir(moduleName: String): Set<String> {
        val baseDir = getOutputDir(moduleName)
        val files = FileUtil.findFilesByMask(Pattern.compile(".*"), baseDir)
        val result = HashSet<String>()
        for (file in files) {
            val relativePath = FileUtil.getRelativePath(baseDir, file)
            assert(relativePath != null) { "relativePath should not be null" }
            result.add(toSystemIndependentName(relativePath!!))
        }
        return result
    }

    private fun getOutputDir(moduleName: String): File = File(workDir, "out/production/$moduleName")

    fun testReexportedDependency() {
        initProject()
        AbstractKotlinJpsBuildTestCase.addKotlinStdlibDependency(myProject.modules.filter { module -> module.name == "module2" }, true)
        buildAllModules().assertSuccessful()
    }

    fun testCancelLongKotlinCompilation() {
        generateLongKotlinFile("Foo.kt", "foo", "Foo")
        initProject(JVM_MOCK_RUNTIME)

        val INITIAL_DELAY = 2000

        val start = System.currentTimeMillis()
        val canceledStatus = CanceledStatus() { System.currentTimeMillis() - start > INITIAL_DELAY }

        val logger = TestProjectBuilderLogger()
        val buildResult = BuildResult()
        buildCustom(canceledStatus, logger, buildResult)
        val interval = System.currentTimeMillis() - start - INITIAL_DELAY

        assertCanceled(buildResult)
        buildResult.assertSuccessful()
        assert(interval < 8000) { "expected time for canceled compilation < 8000 ms, but $interval" }

        val module = myProject.modules.get(0)
        assertFilesNotExistInOutput(module, "foo/Foo.class")

        val expectedLog = workDir.absolutePath + File.separator + "expected.log"
        checkFullLog(logger, File(expectedLog))
    }

    fun testCancelKotlinCompilation() {
        initProject(JVM_MOCK_RUNTIME)
        buildAllModules().assertSuccessful()

        val module = myProject.modules.get(0)
        assertFilesExistInOutput(module, "foo/Bar.class")

        val buildResult = BuildResult()
        val canceledStatus = object : CanceledStatus {
            var checkFromIndex = 0

            override fun isCanceled(): Boolean {
                val messages = buildResult.getMessages(BuildMessage.Kind.INFO)
                for (i in checkFromIndex..messages.size - 1) {
                    if (messages[i].messageText.matches("kotlinc-jvm .+ \\(JRE .+\\)".toRegex())) {
                        return true
                    }
                }

                checkFromIndex = messages.size
                return false
            }
        }

        touch("src/Bar.kt").apply()
        buildCustom(canceledStatus, TestProjectBuilderLogger(), buildResult)
        assertCanceled(buildResult)
    }

    fun testFileDoesNotExistWarning() {
        initProject(JVM_MOCK_RUNTIME)

        AbstractKotlinJpsBuildTestCase.addDependency(
                JpsJavaDependencyScope.COMPILE, Lists.newArrayList(findModule("module")), false, "LibraryWithBadRoots",
                File("badroot.jar"),
                File("test/other/file.xml"),
                File("some/test.class"),
                File("some/other/baddir"))

        val result = buildAllModules()
        result.assertSuccessful()

        val warnings = result.getMessages(BuildMessage.Kind.WARNING)

        Assert.assertArrayEquals(
                arrayOf(
                        """Classpath entry points to a non-existent location: TEST_PATH/badroot.jar""",
                        """Classpath entry points to a non-existent location: TEST_PATH/some/test.class"""),
                warnings.map {
                    it.messageText.replace(File("").absolutePath, "TEST_PATH").replace("\\", "/")
                }.sorted().toTypedArray()
        )
    }

    fun testHelp() {
        initProject()

        val result = buildAllModules()
        result.assertSuccessful()
        val warning = result.getMessages(BuildMessage.Kind.WARNING).single()

        val expectedText = StringUtil.convertLineSeparators(Usage.render(K2JVMCompiler(), K2JVMCompilerArguments()))
        Assert.assertEquals(expectedText, warning.messageText)
    }

    fun testWrongArgument() {
        initProject()

        val result = buildAllModules()
        result.assertFailed()
        val errors = result.getMessages(BuildMessage.Kind.ERROR).joinToString("\n\n") { it.messageText }

        Assert.assertEquals("Invalid argument: -abcdefghij-invalid-argument", errors)
    }

    fun testCodeInKotlinPackage() {
        initProject(JVM_MOCK_RUNTIME)

        val result = buildAllModules()
        result.assertFailed()
        val errors = result.getMessages(BuildMessage.Kind.ERROR)

        Assert.assertEquals("Only the Kotlin standard library is allowed to use the 'kotlin' package", errors.single().messageText)
    }

    fun testDoNotCreateUselessKotlinIncrementalCaches() {
        initProject(JVM_MOCK_RUNTIME)
        buildAllModules().assertSuccessful()

        val storageRoot = BuildDataPathsImpl(myDataStorageRoot).dataStorageRoot
        assertTrue(File(storageRoot, "targets/java-test/kotlinProject/kotlin").exists())
        assertFalse(File(storageRoot, "targets/java-production/kotlinProject/kotlin").exists())
    }

    fun testDoNotCreateUselessKotlinIncrementalCachesForDependentTargets() {
        initProject(JVM_MOCK_RUNTIME)
        buildAllModules().assertSuccessful()

        checkWhen(touch("src/utils.kt"), null, packageClasses("kotlinProject", "src/utils.kt", "_DefaultPackage"))

        val storageRoot = BuildDataPathsImpl(myDataStorageRoot).dataStorageRoot
        assertTrue(File(storageRoot, "targets/java-production/kotlinProject/kotlin").exists())
        assertFalse(File(storageRoot, "targets/java-production/module2/kotlin").exists())
    }

    fun testKotlinProjectWithEmptyProductionOutputDir() {
        initProject(JVM_MOCK_RUNTIME)
        val result = buildAllModules()
        result.assertFailed()
        result.checkErrors()
    }

    fun testKotlinProjectWithEmptyTestOutputDir() {
        doTest()
    }

    fun testKotlinProjectWithEmptyProductionOutputDirWithoutSrcDir() {
        doTest()
    }

    fun testKotlinProjectWithEmptyOutputDirInSomeModules() {
        doTest()
    }

    fun testEAPToReleaseIC() {
        fun setPreRelease(value: Boolean) {
            System.setProperty(TEST_IS_PRE_RELEASE_SYSTEM_PROPERTY, value.toString())
        }

        try {
            withIC {
                initProject(JVM_MOCK_RUNTIME)

                setPreRelease(true)
                buildAllModules().assertSuccessful()
                assertCompiled(KotlinBuilder.KOTLIN_BUILDER_NAME, "src/Bar.kt", "src/Foo.kt")

                touch("src/Foo.kt").apply()
                buildAllModules()
                assertCompiled(KotlinBuilder.KOTLIN_BUILDER_NAME, "src/Foo.kt")

                setPreRelease(false)
                touch("src/Foo.kt").apply()
                buildAllModules().assertSuccessful()
                assertCompiled(KotlinBuilder.KOTLIN_BUILDER_NAME, "src/Bar.kt", "src/Foo.kt")
            }
        }
        finally {
            System.clearProperty(TEST_IS_PRE_RELEASE_SYSTEM_PROPERTY)
        }
    }

    fun testGetDependentTargets() {
        fun addModuleWithSourceAndTestRoot(name: String): JpsModule {
            return addModule(name, "src/").apply {
                contentRootsList.addUrl(JpsPathUtil.pathToUrl("test/"))
                addSourceRoot(JpsPathUtil.pathToUrl("test/"), JavaSourceRootType.TEST_SOURCE)
            }
        }

        val a = addModuleWithSourceAndTestRoot("a")
        val b = addModuleWithSourceAndTestRoot("b")
        val c = addModuleWithSourceAndTestRoot("c")
        val b2 = addModuleWithSourceAndTestRoot("b2")
        val c2 = addModuleWithSourceAndTestRoot("c2")

        JpsModuleRootModificationUtil.addDependency(b, a, JpsJavaDependencyScope.COMPILE, /*exported =*/ true)
        JpsModuleRootModificationUtil.addDependency(c, b, JpsJavaDependencyScope.COMPILE, /*exported =*/ false)
        JpsModuleRootModificationUtil.addDependency(b2, a, JpsJavaDependencyScope.COMPILE, /*exported =*/ false)
        JpsModuleRootModificationUtil.addDependency(c2, b2, JpsJavaDependencyScope.COMPILE, /*exported =*/ false)

        val actual = StringBuilder()
        buildCustom(CanceledStatus.NULL, TestProjectBuilderLogger(), BuildResult()) {
            project.setTestingContext(TestingContext(LookupTracker.DO_NOTHING, object: BuildLogger {
                override fun buildStarted(context: CompileContext, chunk: ModuleChunk) {
                    actual.append("Targets dependent on ${chunk.targets.joinToString() }:\n")
                    actual.append(getDependentTargets(chunk, context).map { it.toString() }.sorted().joinToString("\n"))
                    actual.append("\n---------\n")
                }

                override fun actionsOnCacheVersionChanged(actions: List<CacheVersion.Action>) {}
                override fun buildFinished(exitCode: ModuleLevelBuilder.ExitCode) {}
                override fun markedAsDirty(files: Iterable<File>) {}
            }))
        }

        val expectedFile = File(getCurrentTestDataRoot(), "expected.txt")

        KotlinTestUtils.assertEqualsToFile(expectedFile, actual.toString())
    }

    fun testJre9() {
        val path = KotlinTestUtils.getJdk9HomeIfPossible()?.absolutePath ?: return

        val jdk = myModel.global.addSdk(JDK_NAME, path, "9", JpsJavaSdkType.INSTANCE)
        jdk.addRoot(StandardFileSystems.JRT_PROTOCOL_PREFIX + path + URLUtil.JAR_SEPARATOR + "java.base", JpsOrderRootType.COMPILED)

        loadProject(workDir.absolutePath + File.separator + PROJECT_NAME + ".ipr")
        addKotlinStdlibDependency()

        buildAllModules().assertSuccessful()
    }

    fun testCustomDestination() {
        loadProject(workDir.absolutePath + File.separator + PROJECT_NAME + ".ipr")
        addKotlinStdlibDependency()
        buildAllModules().apply {
            assertSuccessful()

            val aClass = File(workDir, "customOut/A.class")
            assert(aClass.exists()) { "$aClass does not exist!" }

            val warnings = getMessages(BuildMessage.Kind.WARNING)
            assert(warnings.isEmpty()) { "Unexpected warnings: \n${warnings.joinToString("\n")}" }
        }
    }

    private fun BuildResult.checkErrors() {
        val actualErrors = getMessages(BuildMessage.Kind.ERROR)
                .map { it as CompilerMessage }
                .map { "${it.messageText} at line ${it.line}, column ${it.column}" }.sorted().joinToString("\n")
        val expectedFile = File(getCurrentTestDataRoot(), "errors.txt")
        KotlinTestUtils.assertEqualsToFile(expectedFile, actualErrors)
    }

    private fun getCurrentTestDataRoot() = File(AbstractKotlinJpsBuildTestCase.TEST_DATA_PATH + "general/" + getTestName(false))

    private fun buildCustom(
            canceledStatus: CanceledStatus,
            logger: TestProjectBuilderLogger,
            buildResult: BuildResult,
            setupProject: ProjectDescriptor.() -> Unit = {}
    ) {
        val scopeBuilder = CompileScopeTestBuilder.make().allModules()
        val descriptor = this.createProjectDescriptor(BuildLoggingManager(logger))

        descriptor.setupProject()

        try {
            val builder = IncProjectBuilder(descriptor, BuilderRegistry.getInstance(), this.myBuildParams, canceledStatus, null, true)
            builder.addMessageHandler(buildResult)
            builder.build(scopeBuilder.build(), false)
        }
        finally {
            descriptor.dataManager.flush(false)
            descriptor.release()
        }
    }

    private fun checkFullLog(logger: TestProjectBuilderLogger, expectedLogFile: File) {
        UsefulTestCase.assertSameLinesWithFile(expectedLogFile.absolutePath, logger.getFullLog(orCreateProjectDir, myDataStorageRoot))
    }

    private fun assertCanceled(buildResult: BuildResult) {
        val list = buildResult.getMessages(BuildMessage.Kind.INFO)
        assertTrue("The build has been canceled" == list.last().messageText)
    }

    private fun generateLongKotlinFile(filePath: String, packagename: String, className: String)  {
        val file = File(workDir.absolutePath + File.separator + "src" + File.separator + filePath)
        FileUtilRt.createIfNotExists(file)
        val writer = BufferedWriter(FileWriter(file))
        try {
            writer.write("package $packagename\n\n")
            writer.write("public class $className {\n")

            for (i in 0..10000) {
                writer.write("fun f$i():Int = $i\n\n")
            }

            writer.write("}\n")
        }
        finally {
            writer.close()
        }
    }

    private fun findModule(name: String): JpsModule {
        for (module in myProject.modules) {
            if (module.name == name) {
                return module
            }
        }
        throw IllegalStateException("Couldn't find module $name")
    }

    protected fun checkWhen(action: Action, pathsToCompile: Array<String>?, pathsToDelete: Array<String>?) {
        checkWhen(arrayOf(action), pathsToCompile, pathsToDelete)
    }

    protected fun checkWhen(actions: Array<Action>, pathsToCompile: Array<String>?, pathsToDelete: Array<String>?) {
        for (action in actions) {
            action.apply()
        }

        buildAllModules().assertSuccessful()

        if (pathsToCompile != null) {
            assertCompiled(KotlinBuilder.KOTLIN_BUILDER_NAME, *pathsToCompile)
        }

        if (pathsToDelete != null) {
            assertDeleted(*pathsToDelete)
        }
    }

    protected fun packageClasses(moduleName: String, fileName: String, packageClassFqName: String): Array<String> {
        return arrayOf(module(moduleName), packagePartClass(moduleName, fileName, packageClassFqName))
    }

    protected fun packagePartClass(moduleName: String, fileName: String, packageClassFqName: String): String {
        val path = FileUtilRt.toSystemIndependentName(File(workDir, fileName).absolutePath)
        val fakeVirtualFile = object : LightVirtualFile(path.substringAfterLast('/')) {
            override fun getPath(): String {
                // strip extra "/" from the beginning
                return path.substring(1)
            }
        }

        val packagePartFqName = PackagePartClassUtils.getDefaultPartFqName(FqName(packageClassFqName), fakeVirtualFile)
        return klass(moduleName, AsmUtil.internalNameByFqNameWithoutInnerClasses(packagePartFqName))
    }

    protected enum class Operation {
        CHANGE,
        DELETE
    }

    protected fun touch(path: String): Action = Action(Operation.CHANGE, path)

    protected fun del(path: String): Action = Action(Operation.DELETE, path)

    // TODO inline after KT-3974 will be fixed
    protected fun touch(file: File): Unit = JpsBuildTestCase.change(file.absolutePath)

    protected inner class Action constructor(private val operation: Operation, private val path: String) {
        fun apply() {
            val file = File(workDir, path)
            when (operation) {
                Operation.CHANGE ->
                    touch(file)
                Operation.DELETE ->
                    assertTrue("Can not delete file \"" + file.absolutePath + "\"", file.delete())
                else ->
                    fail("Unknown operation")
            }
        }
    }
}
