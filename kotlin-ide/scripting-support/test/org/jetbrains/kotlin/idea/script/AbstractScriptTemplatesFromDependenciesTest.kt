/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.script

import com.intellij.ide.projectView.actions.MarkRootActionBase
import com.intellij.openapi.module.JavaModuleType
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.*
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.HeavyPlatformTestCase
import com.intellij.testFramework.PsiTestUtil
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.io.ZipUtil
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.kotlin.idea.core.script.ScriptDefinitionContributor
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.util.addDependency
import org.jetbrains.kotlin.test.util.jarRoot
import org.jetbrains.kotlin.test.util.projectLibrary
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipOutputStream

@RunWith(JUnit3RunnerWithInners::class)
abstract class AbstractScriptTemplatesFromDependenciesTest : HeavyPlatformTestCase() {

    companion object {
        private const val testFileName = "test.kts"
    }

    fun doTest(path: String) {
        val projectRoot = project.guessProjectDir() ?: return
        val testDataDir = File(path)

        copyDirContentsTo(
            LocalFileSystem.getInstance().findFileByIoFile(testDataDir)!!,
            projectRoot
        )

        testDataDir
            .listFiles { f -> f.name.startsWith("module") }
            ?.filter { it.exists() }
            ?.forEach {
                createTestModule(it)
            }

        ModuleManager.getInstance(project).modules.forEach { module ->
            testDataDir
                .listFiles { f -> f.name.startsWith("jar") }
                ?.map { folder ->
                    ModuleRootManager.getInstance(module).modifiableModel.apply {
                        val vFile = VfsUtil.findFileByIoFile(folder, true)!!
                        MarkRootActionBase.findContentEntry(this, vFile)?.addExcludeFolder(vFile)
                        runWriteAction { this@apply.commit() }
                    }

                    packJar(folder)
                }
                ?.forEach { jar ->
                    module.addDependency(
                        projectLibrary(
                            libraryName = "script-library",
                            classesRoot = jar.jarRoot
                        )
                    )
                }
        }

        val roots: Collection<VirtualFile> = FileBasedIndex.getInstance().getContainingFiles(
            ScriptTemplatesClassRootsIndex.NAME,
            Unit,
            GlobalSearchScope.allScope(project)
        )

        val testFile = File(path, testFileName)
        val fileText = testFile.readText()

        checkRoots(fileText, roots)

        val provider = ScriptDefinitionContributor.find<ScriptTemplatesFromDependenciesProvider>(project)
            ?: error("Cannot find ScriptTemplatesFromDependenciesProvider")

        val (templates, classpath) = provider.getTemplateClassPath(roots.toList())

        checkTemplateNames(fileText, templates)
        checkTemplateClasspath(fileText, classpath)
    }

    private fun String.removeTestDirPrefix(): String = this.substringAfterLast(getTestName(true))

    private fun checkRoots(fileText: String, roots: Collection<VirtualFile>) {
        val actual = roots.map { it.path.removeTestDirPrefix() }
        val expected = InTextDirectivesUtils.findListWithPrefixes(fileText, "// ROOT:")

        assertOrderedEquals("Roots are different", actual.sorted(), expected.sorted())
    }

    private fun checkTemplateNames(fileText: String, names: Collection<String>) {
        val expected = InTextDirectivesUtils.findListWithPrefixes(fileText, "// NAME:")

        assertOrderedEquals("Template names are different", names.sorted(), expected.sorted())
    }

    private fun checkTemplateClasspath(fileText: String, classpath: Collection<File>) {
        val actual = classpath.map {
            FileUtil.toSystemIndependentName(it.path).removeTestDirPrefix()
        }

        val expected = InTextDirectivesUtils.findListWithPrefixes(fileText, "// CLASSPATH:")

        assertOrderedEquals("Roots are different", actual.sorted(), expected.sorted())
    }

    private fun createTestModule(dir: File): Module {
        val newModule = createModuleAt(name, project, JavaModuleType.getModuleType(), dir.toPath())

        dir.listFiles()?.forEach {
            val root = VfsUtil.findFileByIoFile(it, true) ?: return@forEach
            when (it.name) {
                "src" -> PsiTestUtil.addSourceRoot(newModule, root)
                "test" -> PsiTestUtil.addSourceRoot(newModule, root, true)
                "resources" -> PsiTestUtil.addSourceRoot(newModule, root, JavaResourceRootType.RESOURCE)
            }
        }
        return newModule
    }

    private fun packJar(dir: File): File {
        val contentDir = KotlinTestUtils.tmpDirForReusableFolder("folderForLibrary-${getTestName(true)}")
        return createJarFile(contentDir, dir, "templates")
    }

    // Copied from `MockLibraryUtil.createJarFile` of old repo
    private fun createJarFile(contentDir: File, dirToAdd: File, jarName: String, sourcesPath: String? = null): File {
        val jarFile = File(contentDir, jarName + ".jar")

        ZipOutputStream(FileOutputStream(jarFile)).use { zip ->
            ZipUtil.addDirToZipRecursively(zip, jarFile, dirToAdd, "", null, null)
            if (sourcesPath != null) {
                ZipUtil.addDirToZipRecursively(zip, jarFile, File(sourcesPath), "src", null, null)
            }
        }

        return jarFile
    }
}