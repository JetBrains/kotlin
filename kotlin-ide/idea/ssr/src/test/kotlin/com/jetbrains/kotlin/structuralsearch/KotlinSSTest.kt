package com.jetbrains.kotlin.structuralsearch

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.module.StdModuleTypes
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.ui.configuration.libraryEditor.NewLibraryEditor
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.structuralsearch.Matcher
import com.intellij.structuralsearch.inspection.highlightTemplate.SSBasedInspection
import com.intellij.structuralsearch.plugin.ui.SearchConfiguration
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.kotlin.idea.KotlinFileType
import java.io.File
import java.util.*

@Suppress("UnstableApiUsage")
abstract class KotlinSSTest : BasePlatformTestCase() {

    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinLightProjectDescriptor()

    protected fun doTest(pattern: String) {
        myFixture.configureByFile(getTestName(true) + ".kt")
        val configuration = SearchConfiguration()
        configuration.name = "SSR"
        val options = configuration.matchOptions.apply {
            fileType = KotlinFileType.INSTANCE
            fillSearchCriteria(pattern)
        }
        Matcher.validate(project, options)

        val inspection = SSBasedInspection()
        inspection.setConfigurations(Collections.singletonList(configuration))
        myFixture.enableInspections(inspection)

        myFixture.testHighlighting(true, false, false)
    }

    override fun getTestDataPath(): String = "src/test/resources/$basePath/"

    internal class KotlinLightProjectDescriptor : LightProjectDescriptor() {

        val moduleType: ModuleType<*>
            get() = StdModuleTypes.JAVA

        override fun getSdk(): Sdk? {
            val javaHome = System.getProperty("java.home")
            assert(File(javaHome).isDirectory)
            val table = ProjectJdkTable.getInstance()
            val existing = table.findJdk("Full JDK")
            return existing ?: JavaSdk.getInstance().createJdk("Full JDK", javaHome, true)
        }

        override fun configureModule(module: Module, model: ModifiableRootModel, contentEntry: ContentEntry) {
            val editor = NewLibraryEditor()
            editor.name = "LIBRARY"
            val file = File(PathManager.getJarPathForClass(AccessDeniedException::class.java) ?: "")
            assert(file.exists())
            editor.addRoot(
                VfsUtil.getUrlForLibraryRoot(file),
                OrderRootType.CLASSES
            )

            val libraryTableModifiableModel = model.moduleLibraryTable.modifiableModel
            val library = libraryTableModifiableModel.createLibrary(editor.name)

            val libModel = library.modifiableModel
            editor.applyTo(libModel as LibraryEx.ModifiableModelEx)

            libModel.commit()
            libraryTableModifiableModel.commit()
        }
    }
}