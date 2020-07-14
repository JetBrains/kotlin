/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.test

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.PersistentLibraryKind
import com.intellij.openapi.roots.ui.configuration.libraryEditor.NewLibraryEditor
import com.intellij.openapi.vfs.VfsUtil
import org.jetbrains.kotlin.idea.artifacts.KotlinArtifacts
import org.jetbrains.kotlin.idea.artifacts.KotlinTestArtifacts
import org.jetbrains.kotlin.idea.framework.CommonLibraryKind
import org.jetbrains.kotlin.idea.framework.JSLibraryKind
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.util.getProjectJdkTableSafe
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.io.File
import kotlin.test.assertNotNull

/**
 * Helper for configuring kotlin runtime in tested project.
 */
object ConfigLibraryUtil {
    private const val LIB_NAME_JAVA_RUNTIME = "JAVA_RUNTIME_LIB_NAME"
    private const val LIB_NAME_KOTLIN_TEST = "KOTLIN_TEST_LIB_NAME"
    private const val LIB_NAME_KOTLIN_STDLIB_JS = "KOTLIN_JS_STDLIB_NAME"
    private const val LIB_NAME_KOTLIN_STDLIB_COMMON = "KOTLIN_COMMON_STDLIB_NAME"

    private val LIBRARY_NAME_TO_JAR_PATH = mapOf(
            "JUnit" to com.intellij.util.PathUtil.getJarPathForClass(junit.framework.TestCase::class.java),
            "TestNG" to com.intellij.util.PathUtil.getJarPathForClass(org.testng.annotations.Test::class.java)
    )

    private fun getKotlinRuntimeLibEditor(libName: String, library: File): NewLibraryEditor {
        val editor = NewLibraryEditor()
        editor.name = libName
        editor.addRoot(VfsUtil.getUrlForLibraryRoot(library), OrderRootType.CLASSES)

        return editor
    }

    fun configureKotlinRuntimeAndSdk(module: Module, sdk: Sdk) {
        configureSdk(module, sdk)
        configureKotlinRuntime(module)
    }

    fun configureKotlinJsRuntimeAndSdk(module: Module, sdk: Sdk) {
        configureSdk(module, sdk)
        addLibrary(getKotlinRuntimeLibEditor(LIB_NAME_KOTLIN_STDLIB_JS, KotlinArtifacts.kotlinStdlibJs), module, JSLibraryKind)
    }

    fun configureKotlinCommonRuntime(module: Module) {
        addLibrary(getKotlinRuntimeLibEditor(LIB_NAME_KOTLIN_STDLIB_COMMON, KotlinTestArtifacts.kotlinStdlibCommon), module, CommonLibraryKind)
    }

    fun configureKotlinRuntime(module: Module) {
        addLibrary(getKotlinRuntimeLibEditor(LIB_NAME_JAVA_RUNTIME, KotlinArtifacts.kotlinStdlib), module)
        addLibrary(getKotlinRuntimeLibEditor(LIB_NAME_KOTLIN_TEST, KotlinArtifacts.kotlinTest), module)
    }

    fun unConfigureKotlinRuntime(module: Module) {
        removeLibrary(module, LIB_NAME_JAVA_RUNTIME)
        removeLibrary(module, LIB_NAME_KOTLIN_TEST)
    }

    fun unConfigureKotlinRuntimeAndSdk(module: Module, sdk: Sdk) {
        configureSdk(module, sdk)
        unConfigureKotlinRuntime(module)
    }

    fun unConfigureKotlinJsRuntimeAndSdk(module: Module, sdk: Sdk) {
        configureSdk(module, sdk)
        removeLibrary(module, LIB_NAME_KOTLIN_STDLIB_JS)
    }

    fun unConfigureKotlinCommonRuntime(module: Module) {
        removeLibrary(module, LIB_NAME_KOTLIN_STDLIB_COMMON)
    }

    fun configureSdk(module: Module, sdk: Sdk) {
        ApplicationManager.getApplication().runWriteAction {
            val rootManager = ModuleRootManager.getInstance(module)
            val rootModel = rootManager.modifiableModel

            assertNotNull(
                getProjectJdkTableSafe().findJdk(sdk.name),
                "Cannot find sdk in ProjectJdkTable. This may cause sdk leak.\n" +
                        "You can use ProjectPluginTestBase.addJdk(Disposable ...) to register sdk in ProjectJdkTable.\n" +
                        "Then sdk will be removed in tearDown"
            )

            rootModel.sdk = sdk
            rootModel.commit()
        }
    }

    fun addLibrary(editor: NewLibraryEditor, module: Module, kind: PersistentLibraryKind<*>? = null): Library =
        runWriteAction {
            val rootManager = ModuleRootManager.getInstance(module)
            val model = rootManager.modifiableModel

            val library = try {
                addLibrary(editor, model, kind)
            } finally {
                model.commit()
            }

            library
        }

    fun addLibrary(editor: NewLibraryEditor, model: ModifiableRootModel, kind: PersistentLibraryKind<*>? = null): Library {
        val libraryTableModifiableModel = model.moduleLibraryTable.modifiableModel
        val library = libraryTableModifiableModel.createLibrary(editor.name, kind)

        val libModel = library.modifiableModel
        try {
            editor.applyTo(libModel as LibraryEx.ModifiableModelEx)
        } finally {
            libModel.commit()
            libraryTableModifiableModel.commit()
        }

        return library
    }


    fun removeLibrary(module: Module, libraryName: String): Boolean {
        return runWriteAction {
            var removed = false

            val rootManager = ModuleRootManager.getInstance(module)
            val model = rootManager.modifiableModel

            for (orderEntry in model.orderEntries) {
                if (orderEntry is LibraryOrderEntry) {

                    val library = orderEntry.library
                    if (library != null) {
                        val name = library.name
                        if (name != null && name == libraryName) {

                            // Dispose attached roots
                            val modifiableModel = library.modifiableModel
                            for (rootUrl in library.rootProvider.getUrls(OrderRootType.CLASSES)) {
                                modifiableModel.removeRoot(rootUrl, OrderRootType.CLASSES)
                            }
                            for (rootUrl in library.rootProvider.getUrls(OrderRootType.SOURCES)) {
                                modifiableModel.removeRoot(rootUrl, OrderRootType.SOURCES)
                            }
                            modifiableModel.commit()

                            model.moduleLibraryTable.removeLibrary(library)

                            removed = true
                            break
                        }
                    }
                }
            }

            model.commit()

            removed
        }
    }

    private fun addLibrary(module: Module, libraryName: String, jarPaths: List<String>) {
        val editor = NewLibraryEditor()
        editor.name = libraryName
        for (jarPath in jarPaths) {
            val jarFile = File(jarPath)

            require(jarFile.exists()) {
                "Cannot configure library with given path, file doesn't exists $jarPath"
            }
            editor.addRoot(VfsUtil.getUrlForLibraryRoot(jarFile), OrderRootType.CLASSES)
        }

        addLibrary(editor, module)
    }

    private fun libraryNameToJar(libraryName: String): String =
            LIBRARY_NAME_TO_JAR_PATH[libraryName] ?: error("$libraryName isn't registered")

    private fun configureLibraries(module: Module, rootPath: String, libraryNames: List<String>) {
        for (libraryName in libraryNames) {
            val jarPaths = libraryName.split(";".toRegex()).dropLastWhile { it.isEmpty() }.map { libraryNameToJar(it) }
            addLibrary(module, libraryName, jarPaths)
        }
    }

    private fun unconfigureLibrariesByName(module: Module, libraryNames: MutableList<String>) {
        val iterator = libraryNames.iterator()
        while (iterator.hasNext()) {
            val libraryName = iterator.next()
            if (removeLibrary(module, libraryName)) {
                iterator.remove()
            }
        }

        if (libraryNames.isNotEmpty()) throw AssertionError("Couldn't find the following libraries: " + libraryNames)
    }

    fun configureLibrariesByDirective(module: Module, rootPath: String, fileText: String) {
        configureLibraries(module, rootPath, InTextDirectivesUtils.findListWithPrefixes(fileText, "// CONFIGURE_LIBRARY: "))
    }

    fun unconfigureLibrariesByDirective(module: Module, fileText: String) {
        val libraryNames =
                InTextDirectivesUtils.findListWithPrefixes(fileText, "// CONFIGURE_LIBRARY: ") +
                InTextDirectivesUtils.findListWithPrefixes(fileText, "// UNCONFIGURE_LIBRARY: ")

        unconfigureLibrariesByName(module, libraryNames.toMutableList())
    }
}
