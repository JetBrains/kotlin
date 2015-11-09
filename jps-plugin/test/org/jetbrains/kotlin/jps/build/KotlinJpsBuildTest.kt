/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtil.toSystemIndependentName
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.UsefulTestCase
import com.intellij.util.ArrayUtil
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.io.ZipUtil
import org.jetbrains.jps.api.CanceledStatus
import org.jetbrains.jps.builders.BuildResult
import org.jetbrains.jps.builders.CompileScopeTestBuilder
import org.jetbrains.jps.builders.JpsBuildTestCase
import org.jetbrains.jps.builders.TestProjectBuilderLogger
import org.jetbrains.jps.builders.impl.BuildDataPathsImpl
import org.jetbrains.jps.builders.logging.BuildLoggingManager
import org.jetbrains.jps.incremental.BuilderRegistry
import org.jetbrains.jps.incremental.IncProjectBuilder
import org.jetbrains.jps.incremental.messages.BuildMessage
import org.jetbrains.jps.incremental.messages.CompilerMessage
import org.jetbrains.jps.model.java.JpsJavaDependencyScope
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.jps.util.JpsPathUtil
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.JvmCodegenUtil
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
import java.util.*
import java.util.regex.Pattern
import java.util.zip.ZipOutputStream
import kotlin.test.*

public class KotlinJpsBuildTest : AbstractKotlinJpsBuildTestCase() {
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
                "lib/stdlib.meta.js"
        )
        private val EXPECTED_JS_FILES_IN_OUTPUT_FOR_MODULE_STDLIB_ONLY = hashSetOf(
                "$ADDITIONAL_MODULE_NAME.js",
                "$ADDITIONAL_MODULE_NAME.meta.js",
                "lib/kotlin.js",
                "lib/stdlib.meta.js"
        )
        private val EXPECTED_JS_FILES_IN_OUTPUT_NO_COPY = hashSetOf("$PROJECT_NAME.js", "$PROJECT_NAME.meta.js")
        private val EXPECTED_JS_FILES_IN_OUTPUT_WITH_ADDITIONAL_LIB_AND_DEFAULT_DIR = hashSetOf(
                "$PROJECT_NAME.js",
                "$PROJECT_NAME.meta.js",
                "lib/kotlin.js",
                "lib/stdlib.meta.js",
                "lib/jslib-example.js",
                "lib/file0.js",
                "lib/dir/file1.js",
                "lib/META-INF-ex/file2.js",
                "lib/res0.js",
                "lib/resdir/res1.js"
        )
        private val EXPECTED_JS_FILES_IN_OUTPUT_WITH_ADDITIONAL_LIB_AND_CUSTOM_DIR = hashSetOf(
                "$PROJECT_NAME.js",
                "$PROJECT_NAME.meta.js",
                "custom/kotlin.js",
                "custom/stdlib.meta.js",
                "custom/jslib-example.js",
                "custom/file0.js",
                "custom/dir/file1.js",
                "custom/META-INF-ex/file2.js",
                "custom/res0.js",
                "custom/resdir/res1.js"
        )

        private fun k2jsOutput(vararg moduleNames: String): Array<String> {
            val list = arrayListOf<String>()
            for (moduleName in moduleNames) {
                val outputDir = File("out/production/$moduleName")
                list.add(toSystemIndependentName(JpsJsModuleUtils.getOutputFile(outputDir, moduleName).getPath()))
                list.add(toSystemIndependentName(JpsJsModuleUtils.getOutputMetaFile(outputDir, moduleName).getPath()))
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

        private fun assertFilesExistInOutput(module: JpsModule, vararg relativePaths: String) {
            for (path in relativePaths) {
                val outputFile = findFileInOutputDir(module, path)
                assertTrue(outputFile.exists(), "Output not written: " + outputFile.getAbsolutePath() + "\n Directory contents: \n" + dirContents(outputFile.getParentFile()))
            }
        }

        private fun findFileInOutputDir(module: JpsModule, relativePath: String): File {
            val outputUrl = JpsJavaExtensionService.getInstance().getOutputUrl(module, false)
            assertNotNull(outputUrl)
            val outputDir = File(JpsPathUtil.urlToPath(outputUrl))
            return File(outputDir, relativePath)
        }


        private fun assertFilesNotExistInOutput(module: JpsModule, vararg relativePaths: String) {
            val outputUrl = JpsJavaExtensionService.getInstance().getOutputUrl(module, false)
            assertNotNull(outputUrl)
            val outputDir = File(JpsPathUtil.urlToPath(outputUrl))
            for (path in relativePaths) {
                val outputFile = File(outputDir, path)
                assertFalse(outputFile.exists(), "Output directory \"" + outputFile.getAbsolutePath() + "\" contains \"" + path + "\"")
            }
        }

        private fun dirContents(dir: File): String {
            val files = dir.listFiles() ?: return "<not found>"
            val builder = StringBuilder()
            for (file in files) {
                builder.append(" * ").append(file.getName()).append("\n")
            }
            return builder.toString()
        }

        private fun klass(moduleName: String, classFqName: String): String {
            val outputDirPrefix = "out/production/$moduleName/"
            return outputDirPrefix + classFqName.replace('.', '/') + ".class"
        }

        private fun module(moduleName: String): String {
            return "out/production/$moduleName/${JvmCodegenUtil.getMappingFileName(moduleName)}"
        }

        public fun mergeArrays(vararg stringArrays: Array<String>): Array<String> {
            val result = HashSet<String>()
            for (array in stringArrays) {
                result.addAll(Arrays.asList(*array))
            }
            return ArrayUtil.toStringArray(result)
        }
    }

    override fun setUp() {
        super.setUp()
        val sourceFilesRoot = File(AbstractKotlinJpsBuildTestCase.TEST_DATA_PATH + "general/" + getTestName(false))
        workDir = AbstractKotlinJpsBuildTestCase.copyTestDataToTmpDir(sourceFilesRoot)
        getOrCreateProjectDir()
    }

    override fun tearDown() {
        FileUtil.delete(workDir)
        super.tearDown()
    }

    override fun doGetProjectDir(): File = workDir

    private fun initProject() {
        addJdk(JDK_NAME)
        loadProject(workDir.getAbsolutePath() + File.separator + PROJECT_NAME + ".ipr")
    }

    public fun doTest() {
        initProject()
        makeAll().assertSuccessful()
    }

    public fun doTestWithRuntime() {
        initProject()
        addKotlinRuntimeDependency()
        makeAll().assertSuccessful()
    }

    public fun doTestWithKotlinJavaScriptLibrary() {
        initProject()
        addKotlinJavaScriptStdlibDependency()
        createKotlinJavaScriptLibraryArchive()
        addKotlinJavaScriptDependency(KOTLIN_JS_LIBRARY, File(workDir, KOTLIN_JS_LIBRARY_JAR))
        makeAll().assertSuccessful()
    }

    public fun testKotlinProject() {
        doTest()

        checkWhen(touch("src/test1.kt"), null, packageClasses("kotlinProject", "src/test1.kt", "Test1Kt"))
    }

    public fun testSourcePackagePrefix() {
        doTest()
    }
    
    public fun testSourcePackageLongPrefix() {
        initProject()
        val buildResult = makeAll()
        buildResult.assertSuccessful()
        val warnings = buildResult.getMessages(BuildMessage.Kind.WARNING)
        assertEquals("Warning about invalid package prefix in module 2 is expected: $warnings", 2, warnings.size)
        assertEquals("Invalid package prefix name is ignored: invalid-prefix.test", warnings.first().messageText)
    }

    public fun testKotlinJavaScriptProject() {
        initProject()
        addKotlinJavaScriptStdlibDependency()
        makeAll().assertSuccessful()

        assertEquals(EXPECTED_JS_FILES_IN_OUTPUT_FOR_STDLIB_ONLY, contentOfOutputDir(PROJECT_NAME))
        checkWhen(touch("src/test1.kt"), null, k2jsOutput(PROJECT_NAME))
    }

    public fun testKotlinJavaScriptProjectWithTwoModules() {
        initProject()
        addKotlinJavaScriptStdlibDependency()
        makeAll().assertSuccessful()

        assertEquals(EXPECTED_JS_FILES_IN_OUTPUT_FOR_STDLIB_ONLY, contentOfOutputDir(PROJECT_NAME))
        assertEquals(EXPECTED_JS_FILES_IN_OUTPUT_FOR_MODULE_STDLIB_ONLY, contentOfOutputDir(ADDITIONAL_MODULE_NAME))

        checkWhen(touch("src/test1.kt"), null, k2jsOutput(PROJECT_NAME))
        checkWhen(touch("module2/src/module2.kt"), null, k2jsOutput(ADDITIONAL_MODULE_NAME))
        checkWhen(arrayOf(touch("src/test1.kt"), touch("module2/src/module2.kt")), null, k2jsOutput(PROJECT_NAME, ADDITIONAL_MODULE_NAME))
    }

    public fun testKotlinJavaScriptProjectWithDirectoryAsStdlib() {
        initProject()
        val jslibJar = PathUtil.getKotlinPathsForDistDirectory().getJsStdLibJarPath()
        val jslibDir = File(workDir, "KotlinJavaScript")
        try {
            ZipUtil.extract(jslibJar, jslibDir, null)
        }
        catch (ex: IOException) {
            throw IllegalStateException(ex.getMessage())
        }

        addKotlinJavaScriptDependency("KotlinJavaScript", jslibDir)
        makeAll().assertSuccessful()

        assertEquals(EXPECTED_JS_FILES_IN_OUTPUT_FOR_STDLIB_ONLY, contentOfOutputDir(PROJECT_NAME))
        checkWhen(touch("src/test1.kt"), null, k2jsOutput(PROJECT_NAME))
    }

    public fun testKotlinJavaScriptProjectWithDirectoryAsLibrary() {
        initProject()
        addKotlinJavaScriptStdlibDependency()
        addKotlinJavaScriptDependency(KOTLIN_JS_LIBRARY, File(workDir, KOTLIN_JS_LIBRARY))
        makeAll().assertSuccessful()

        assertEquals(EXPECTED_JS_FILES_IN_OUTPUT_WITH_ADDITIONAL_LIB_AND_DEFAULT_DIR, contentOfOutputDir(PROJECT_NAME))
        checkWhen(touch("src/test1.kt"), null, k2jsOutput(PROJECT_NAME))
    }

    public fun testKotlinJavaScriptProjectWithLibrary() {
        doTestWithKotlinJavaScriptLibrary()

        assertEquals(EXPECTED_JS_FILES_IN_OUTPUT_WITH_ADDITIONAL_LIB_AND_DEFAULT_DIR, contentOfOutputDir(PROJECT_NAME))
        checkWhen(touch("src/test1.kt"), null, k2jsOutput(PROJECT_NAME))
    }

    public fun testKotlinJavaScriptProjectWithLibraryCustomOutputDir() {
        doTestWithKotlinJavaScriptLibrary()

        assertEquals(EXPECTED_JS_FILES_IN_OUTPUT_WITH_ADDITIONAL_LIB_AND_CUSTOM_DIR, contentOfOutputDir(PROJECT_NAME))
        checkWhen(touch("src/test1.kt"), null, k2jsOutput(PROJECT_NAME))
    }

    public fun testKotlinJavaScriptProjectWithLibraryNoCopy() {
        doTestWithKotlinJavaScriptLibrary()

        assertEquals(EXPECTED_JS_FILES_IN_OUTPUT_NO_COPY, contentOfOutputDir(PROJECT_NAME))
        checkWhen(touch("src/test1.kt"), null, k2jsOutput(PROJECT_NAME))
    }

    public fun testKotlinJavaScriptProjectWithLibraryAndErrors() {
        initProject()
        addKotlinJavaScriptStdlibDependency()
        createKotlinJavaScriptLibraryArchive()
        addKotlinJavaScriptDependency(KOTLIN_JS_LIBRARY, File(workDir, KOTLIN_JS_LIBRARY_JAR))
        makeAll().assertFailed()

        assertEquals(Collections.EMPTY_SET, contentOfOutputDir(PROJECT_NAME))
    }

    public fun testExcludeFolderInSourceRoot() {
        doTest()

        val module = myProject.getModules().get(0)
        assertFilesExistInOutput(module, "Foo.class")
        assertFilesNotExistInOutput(module, *EXCLUDE_FILES)

        checkWhen(touch("src/foo.kt"), null, arrayOf(klass("kotlinProject", "Foo")))
    }

    public fun testExcludeModuleFolderInSourceRootOfAnotherModule() {
        doTest()

        for (module in myProject.getModules()) {
            assertFilesExistInOutput(module, "Foo.class")
        }

        checkWhen(touch("src/foo.kt"), null, arrayOf(klass("kotlinProject", "Foo")))
        checkWhen(touch("src/module2/src/foo.kt"), null, arrayOf(klass("module2", "Foo")))
    }

    public fun testExcludeFileUsingCompilerSettings() {
        doTest()

        val module = myProject.getModules().get(0)
        assertFilesExistInOutput(module, "Foo.class", "Bar.class")
        assertFilesNotExistInOutput(module, *EXCLUDE_FILES)

        checkWhen(touch("src/foo.kt"), null, arrayOf(klass("kotlinProject", "Foo")))
        checkWhen(touch("src/Excluded.kt"), null, NOTHING)
        checkWhen(touch("src/dir/YetAnotherExcluded.kt"), null, NOTHING)
    }

    public fun testExcludeFolderNonRecursivelyUsingCompilerSettings() {
        doTest()

        val module = myProject.getModules().get(0)
        assertFilesExistInOutput(module, "Foo.class", "Bar.class")
        assertFilesNotExistInOutput(module, *EXCLUDE_FILES)

        checkWhen(touch("src/foo.kt"), null, arrayOf(klass("kotlinProject", "Foo")))
        checkWhen(touch("src/dir/subdir/bar.kt"), null, arrayOf(klass("kotlinProject", "Bar")))

        checkWhen(touch("src/dir/Excluded.kt"), null, NOTHING)
        checkWhen(touch("src/dir/subdir/YetAnotherExcluded.kt"), null, NOTHING)
    }

    public fun testExcludeFolderRecursivelyUsingCompilerSettings() {
        doTest()

        val module = myProject.getModules().get(0)
        assertFilesExistInOutput(module, "Foo.class", "Bar.class")
        assertFilesNotExistInOutput(module, *EXCLUDE_FILES)

        checkWhen(touch("src/foo.kt"), null, arrayOf(klass("kotlinProject", "Foo")))

        checkWhen(touch("src/exclude/Excluded.kt"), null, NOTHING)
        checkWhen(touch("src/exclude/YetAnotherExcluded.kt"), null, NOTHING)
        checkWhen(touch("src/exclude/subdir/Excluded.kt"), null, NOTHING)
        checkWhen(touch("src/exclude/subdir/YetAnotherExcluded.kt"), null, NOTHING)
    }

    public fun testManyFiles() {
        doTest()

        val module = myProject.getModules().get(0)
        assertFilesExistInOutput(module, "foo/MainKt.class", "boo/BooKt.class", "foo/Bar.class")

        checkWhen(touch("src/main.kt"), null, packageClasses("kotlinProject", "src/main.kt", "foo.MainKt"))
        checkWhen(touch("src/boo.kt"), null, packageClasses("kotlinProject", "src/boo.kt", "boo.BooKt"))
        checkWhen(touch("src/Bar.kt"), arrayOf("src/Bar.kt"), arrayOf(klass("kotlinProject", "foo.Bar")))

        checkWhen(del("src/main.kt"),
                arrayOf("src/Bar.kt", "src/boo.kt"),
                mergeArrays(packageClasses("kotlinProject", "src/main.kt", "foo.MainKt"),
                            packageClasses("kotlinProject", "src/boo.kt", "boo.BooKt"),
                            arrayOf(klass("kotlinProject", "foo.Bar"))))
        assertFilesExistInOutput(module, "boo/BooKt.class", "foo/Bar.class")
        assertFilesNotExistInOutput(module, "foo/MainKt.class")

        checkWhen(touch("src/boo.kt"), null, packageClasses("kotlinProject", "src/boo.kt", "boo.BooKt"))
        checkWhen(touch("src/Bar.kt"), null, arrayOf(klass("kotlinProject", "foo.Bar")))
    }

    public fun testManyFilesForPackage() {
        doTest()

        val module = myProject.getModules().get(0)
        assertFilesExistInOutput(module, "foo/MainKt.class", "boo/BooKt.class", "foo/Bar.class")

        checkWhen(touch("src/main.kt"), null, packageClasses("kotlinProject", "src/main.kt", "foo.MainKt"))
        checkWhen(touch("src/boo.kt"), null, packageClasses("kotlinProject", "src/boo.kt", "boo.BooKt"))
        checkWhen(touch("src/Bar.kt"),
                arrayOf("src/Bar.kt"),
                arrayOf(klass("kotlinProject", "foo.Bar"),
                        packagePartClass("kotlinProject", "src/Bar.kt", "foo.MainKt"),
                        module("kotlinProject")))

        checkWhen(del("src/main.kt"),
                arrayOf("src/Bar.kt", "src/boo.kt"),
                mergeArrays(packageClasses("kotlinProject", "src/main.kt", "foo.MainKt"),
                            packageClasses("kotlinProject", "src/Bar.kt", "foo.MainKt"),
                            packageClasses("kotlinProject", "src/boo.kt", "boo.BooKt"),
                            arrayOf(klass("kotlinProject", "foo.Bar"))))
        assertFilesExistInOutput(module, "boo/BooKt.class", "foo/Bar.class")

        checkWhen(touch("src/boo.kt"), null, packageClasses("kotlinProject", "src/boo.kt", "boo.BooKt"))
        checkWhen(touch("src/Bar.kt"), null,
                arrayOf(klass("kotlinProject", "foo.Bar"),
                        packagePartClass("kotlinProject", "src/Bar.kt", "foo.MainKt"),
                        module("kotlinProject")))
    }

    public fun testKotlinProjectTwoFilesInOnePackage() {
        doTest()

        checkWhen(touch("src/test1.kt"), null, packageClasses("kotlinProject", "src/test1.kt", "_DefaultPackage"))
        checkWhen(touch("src/test2.kt"), null, packageClasses("kotlinProject", "src/test2.kt", "_DefaultPackage"))

        checkWhen(arrayOf(del("src/test1.kt"), del("src/test2.kt")), NOTHING,
                  arrayOf(packagePartClass("kotlinProject", "src/test1.kt", "_DefaultPackage"),
                          packagePartClass("kotlinProject", "src/test2.kt", "_DefaultPackage"),
                          module("kotlinProject")))

        assertFilesNotExistInOutput(myProject.getModules().get(0), "_DefaultPackage.class")
    }

    public fun testKotlinJavaProject() {
        doTestWithRuntime()
    }

    public fun testJKJProject() {
        doTestWithRuntime()
    }

    public fun testKJKProject() {
        doTestWithRuntime()
    }

    public fun testKJCircularProject() {
        doTestWithRuntime()
    }

    public fun testJKJInheritanceProject() {
        doTestWithRuntime()
    }

    public fun testKJKInheritanceProject() {
        doTestWithRuntime()
    }

    public fun testCircularDependenciesNoKotlinFiles() {
        doTest()
    }

    public fun testCircularDependenciesDifferentPackages() {
        initProject()
        val result = makeAll()

        // Check that outputs are located properly
        assertFilesExistInOutput(findModule("module2"), "kt1/Kt1Kt.class")
        assertFilesExistInOutput(findModule("kotlinProject"), "kt2/Kt2Kt.class")

        result.assertSuccessful()

        checkWhen(touch("src/kt2.kt"), null, packageClasses("kotlinProject", "src/kt2.kt", "kt2.Kt2Kt"))
        checkWhen(touch("module2/src/kt1.kt"), null, packageClasses("module2", "module2/src/kt1.kt", "kt1.Kt1Kt"))
    }

    public fun testCircularDependenciesSamePackage() {
        initProject()
        val result = makeAll()
        result.assertSuccessful()

        // Check that outputs are located properly
        val facadeWithA = findFileInOutputDir(findModule("module1"), "test/AKt.class")
        val facadeWithB = findFileInOutputDir(findModule("module2"), "test/BKt.class")
        UsefulTestCase.assertSameElements(getMethodsOfClass(facadeWithA), "<clinit>", "a", "getA")
        UsefulTestCase.assertSameElements(getMethodsOfClass(facadeWithB), "<clinit>", "b", "getB", "setB")

        checkWhen(touch("module1/src/a.kt"), null, packageClasses("module1", "module1/src/a.kt", "test.TestPackage"))
        checkWhen(touch("module2/src/b.kt"), null, packageClasses("module2", "module2/src/b.kt", "test.TestPackage"))
    }

    public fun testCircularDependenciesSamePackageWithTests() {
        initProject()
        val result = makeAll()
        result.assertSuccessful()

        // Check that outputs are located properly
        val facadeWithA = findFileInOutputDir(findModule("module1"), "test/AKt.class")
        val facadeWithB = findFileInOutputDir(findModule("module2"), "test/BKt.class")
        UsefulTestCase.assertSameElements(getMethodsOfClass(facadeWithA), "<clinit>", "a", "funA", "getA")
        UsefulTestCase.assertSameElements(getMethodsOfClass(facadeWithB), "<clinit>", "b", "funB", "getB", "setB")

        checkWhen(touch("module1/src/a.kt"), null, packageClasses("module1", "module1/src/a.kt", "test.TestPackage"))
        checkWhen(touch("module2/src/b.kt"), null, packageClasses("module2", "module2/src/b.kt", "test.TestPackage"))
    }

    public fun testInternalFromAnotherModule() {
        initProject()
        val result = makeAll()
        result.assertFailed()
        result.checkErrors()
    }

    public fun testCircularDependenciesInternalFromAnotherModule() {
        initProject()
        val result = makeAll()
        result.assertFailed()
        result.checkErrors()
    }

    public fun testCircularDependenciesWrongInternalFromTests() {
        initProject()
        val result = makeAll()
        result.assertFailed()
        result.checkErrors()
    }

    public fun testCircularDependencyWithReferenceToOldVersionLib() {
        initProject()

        val libraryJar = MockLibraryUtil.compileLibraryToJar(workDir.getAbsolutePath() + File.separator + "oldModuleLib/src", "module-lib", false)

        AbstractKotlinJpsBuildTestCase.addDependency(JpsJavaDependencyScope.COMPILE, Lists.newArrayList(findModule("module1"), findModule("module2")), false, "module-lib", libraryJar)

        val result = makeAll()
        result.assertSuccessful()
    }

    public fun testDependencyToOldKotlinLib() {
        initProject()

        val libraryJar = MockLibraryUtil.compileLibraryToJar(workDir.getAbsolutePath() + File.separator + "oldModuleLib/src", "module-lib", false)

        AbstractKotlinJpsBuildTestCase.addDependency(JpsJavaDependencyScope.COMPILE, Lists.newArrayList(findModule("module")), false, "module-lib", libraryJar)

        addKotlinRuntimeDependency()

        val result = makeAll()
        result.assertSuccessful()
    }

    public fun testAccessToInternalInProductionFromTests() {
        initProject()

        val result = makeAll()
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
            throw IllegalStateException(ex.getMessage())
        }
        catch (ex: IOException) {
            throw IllegalStateException(ex.getMessage())
        }

    }

    private fun contentOfOutputDir(moduleName: String): Set<String> {
        val outputDir = "out/production/$moduleName"
        val baseDir = File(workDir, outputDir)
        val files = FileUtil.findFilesByMask(Pattern.compile(".*"), baseDir)
        val result = HashSet<String>()
        for (file in files) {
            val relativePath = FileUtil.getRelativePath(baseDir, file)
            assert(relativePath != null) { "relativePath should not be null" }
            result.add(toSystemIndependentName(relativePath!!))
        }
        return result
    }

    public fun testReexportedDependency() {
        initProject()
        AbstractKotlinJpsBuildTestCase.addKotlinRuntimeDependency(JpsJavaDependencyScope.COMPILE, ContainerUtil.filter(myProject.getModules(), object : Condition<JpsModule> {
            override fun value(module: JpsModule): Boolean {
                return module.getName() == "module2"
            }
        }), true)
        makeAll().assertSuccessful()
    }

    public fun testCancelLongKotlinCompilation() {
        generateLongKotlinFile("Foo.kt", "foo", "Foo")
        initProject()

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

        val module = myProject.getModules().get(0)
        assertFilesNotExistInOutput(module, "foo/Foo.class")

        val expectedLog = workDir.getAbsolutePath() + File.separator + "expected.log"
        checkFullLog(logger, File(expectedLog))
    }

    public fun testCancelKotlinCompilation() {
        initProject()
        makeAll().assertSuccessful()

        val module = myProject.getModules().get(0)
        assertFilesExistInOutput(module, "foo/Bar.class")

        val buildResult = BuildResult()
        val canceledStatus = object: CanceledStatus {
            var checkFromIndex = 0;

            public override fun isCanceled(): Boolean {
                val messages = buildResult.getMessages(BuildMessage.Kind.INFO)
                for (i in checkFromIndex..messages.size()-1) {
                    if (messages.get(i).getMessageText().startsWith("Kotlin JPS plugin version")) return true;
                }

                checkFromIndex = messages.size();
                return false;
            }
        }

        touch("src/Bar.kt").apply()
        buildCustom(canceledStatus, TestProjectBuilderLogger(), buildResult)
        assertCanceled(buildResult)

        assertFilesNotExistInOutput(module, "foo/Bar.class")
    }

    public fun testFileDoesNotExistWarning() {
        initProject()

        AbstractKotlinJpsBuildTestCase.addDependency(
                JpsJavaDependencyScope.COMPILE, Lists.newArrayList(findModule("module")), false, "LibraryWithBadRoots",
                File("badroot.jar"),
                File("test/other/file.xml"),
                File("some/test.class"),
                File("some/other/baddir"))

        val result = makeAll()
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

    public fun testDoNotCreateUselessKotlinIncrementalCaches() {
        initProject()
        makeAll().assertSuccessful()

        val storageRoot = BuildDataPathsImpl(myDataStorageRoot).dataStorageRoot
        assertTrue(File(storageRoot, "targets/java-test/kotlinProject/kotlin").exists())
        assertFalse(File(storageRoot, "targets/java-production/kotlinProject/kotlin").exists())
    }

    public fun testDoNotCreateUselessKotlinIncrementalCachesForDependentTargets() {
        initProject()
        makeAll().assertSuccessful()

        checkWhen(touch("src/utils.kt"), null, packageClasses("kotlinProject", "src/utils.kt", "_DefaultPackage"))

        val storageRoot = BuildDataPathsImpl(myDataStorageRoot).dataStorageRoot
        assertTrue(File(storageRoot, "targets/java-production/kotlinProject/kotlin").exists())
        assertFalse(File(storageRoot, "targets/java-production/module2/kotlin").exists())
    }

    private fun BuildResult.checkErrors() {
        val actualErrors = getMessages(BuildMessage.Kind.ERROR)
                .map { it as CompilerMessage }
                .map { "${it.messageText} at line ${it.line}, column ${it.column}" }.sorted().joinToString("\n")
        val projectRoot = File(AbstractKotlinJpsBuildTestCase.TEST_DATA_PATH + "general/" + getTestName(false))
        val expectedFile = File(projectRoot, "errors.txt")
        KotlinTestUtils.assertEqualsToFile(expectedFile, actualErrors)
    }

    private fun buildCustom(canceledStatus: CanceledStatus, logger: TestProjectBuilderLogger,buildResult: BuildResult) {
        val scopeBuilder = CompileScopeTestBuilder.make().all()
        val descriptor = this.createProjectDescriptor(BuildLoggingManager(logger))
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
        UsefulTestCase.assertSameLinesWithFile(expectedLogFile.getAbsolutePath(), logger.getFullLog(getOrCreateProjectDir(), myDataStorageRoot))
    }

    private fun assertCanceled(buildResult: BuildResult) {
        val list = buildResult.getMessages(BuildMessage.Kind.INFO)
        assertTrue("The build has been canceled".equals(list.last().getMessageText()))
    }

    private fun generateLongKotlinFile(filePath: String, packagename: String, className: String)  {
        val file = File(workDir.getAbsolutePath() + File.separator + "src" + File.separator + filePath)
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
        for (module in myProject.getModules()) {
            if (module.getName() == name) {
                return module
            }
        }
        throw IllegalStateException("Couldn't find module $name")
    }

    private fun checkWhen(action: Action, pathsToCompile: Array<String>?, pathsToDelete: Array<String>?) {
        checkWhen(arrayOf(action), pathsToCompile, pathsToDelete)
    }

    private fun checkWhen(actions: Array<Action>, pathsToCompile: Array<String>?, pathsToDelete: Array<String>?) {
        for (action in actions) {
            action.apply()
        }

        makeAll().assertSuccessful()

        if (pathsToCompile != null) {
            assertCompiled(KotlinBuilder.KOTLIN_BUILDER_NAME, *pathsToCompile)
        }

        if (pathsToDelete != null) {
            assertDeleted(*pathsToDelete)
        }
    }

    private fun packageClasses(moduleName: String, fileName: String, packageClassFqName: String): Array<String> {
        return arrayOf(module(moduleName), packagePartClass(moduleName, fileName, packageClassFqName))
    }

    private fun packagePartClass(moduleName: String, fileName: String, packageClassFqName: String): String {
        val path = FileUtilRt.toSystemIndependentName(File(workDir, fileName).getAbsolutePath())
        val fakeVirtualFile = object : LightVirtualFile(path.substringAfterLast('/')) {
            override fun getPath(): String {
                // strip extra "/" from the beginning
                return path.substring(1)
            }
        }

        val packagePartFqName = PackagePartClassUtils.getDefaultPartFqName(FqName(packageClassFqName), fakeVirtualFile)
        return klass(moduleName, AsmUtil.internalNameByFqNameWithoutInnerClasses(packagePartFqName))
    }

    private enum class Operation {
        CHANGE,
        DELETE
    }

    private fun touch(path: String): Action = Action(Operation.CHANGE, path)

    private fun del(path: String): Action = Action(Operation.DELETE, path)

    // TODO inline after KT-3974 will be fixed
    private fun touch(file: File): Unit = JpsBuildTestCase.change(file.absolutePath)

    private inner class Action constructor(private val operation: Operation, private val path: String) {
        fun apply() {
            val file = File(workDir, path)
            when (operation) {
                Operation.CHANGE ->
                    touch(file)
                Operation.DELETE ->
                    assertTrue(file.delete(), "Can not delete file \"" + file.getAbsolutePath() + "\"")
                else ->
                    fail("Unknown operation")
            }
        }
    }
}
