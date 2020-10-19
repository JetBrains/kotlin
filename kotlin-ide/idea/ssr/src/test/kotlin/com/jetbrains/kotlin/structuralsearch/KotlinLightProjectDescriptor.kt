package com.jetbrains.kotlin.structuralsearch

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.ui.configuration.libraryEditor.NewLibraryEditor
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.testFramework.LightProjectDescriptor
import java.io.File

open class KotlinLightProjectDescriptor : LightProjectDescriptor() {
    override fun getSdk(): Sdk? {
        val javaHome = System.getProperty("java.home")
        assert(File(javaHome).isDirectory)
        val table = ProjectJdkTable.getInstance()
        val existing = table.findJdk("Full JDK")
        return existing ?: JavaSdk.getInstance().createJdk("Full JDK", javaHome, true)
    }

    protected fun addLibrary(clazz: Class<*>, orderRootType: OrderRootType, model: ModifiableRootModel) {
        val editor = NewLibraryEditor()
        editor.name = clazz.canonicalName

        val file = File(PathManager.getJarPathForClass(clazz) ?: "")
        assert(file.exists())
        editor.addRoot(
            VfsUtil.getUrlForLibraryRoot(file),
            orderRootType
        )

        val libraryTableModifiableModel = model.moduleLibraryTable.modifiableModel
        val library = libraryTableModifiableModel.createLibrary(editor.name)

        val libModel = library.modifiableModel
        editor.applyTo(libModel as LibraryEx.ModifiableModelEx)

        libModel.commit()
        libraryTableModifiableModel.commit()
    }

    override fun configureModule(module: Module, model: ModifiableRootModel, contentEntry: ContentEntry) {
        addLibrary(AccessDeniedException::class.java, OrderRootType.CLASSES, model)
    }
}