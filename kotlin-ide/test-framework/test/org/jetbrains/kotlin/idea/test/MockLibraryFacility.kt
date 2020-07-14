package org.jetbrains.kotlin.idea.test

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.ui.configuration.libraryEditor.NewLibraryEditor
import com.intellij.openapi.util.io.FileUtilRt
import org.jetbrains.kotlin.idea.framework.JSLibraryKind
import org.jetbrains.kotlin.platform.js.JsPlatform
import org.jetbrains.kotlin.test.KotlinCompilerStandalone
import java.io.File

data class MockLibraryFacility(
    val source: File,
    val attachSources: Boolean = true,
    val platform: KotlinCompilerStandalone.Platform = KotlinCompilerStandalone.Platform.Jvm(),
    val options: List<String> = emptyList(),
    val classpath: List<File> = emptyList()
) {
    companion object {
        const val MOCK_LIBRARY_NAME = "kotlinMockLibrary"
    }

    fun setUp(module: Module) {
        val libraryJar = KotlinCompilerStandalone(
            listOf(source),
            platform = platform,
            options = options,
            classpath = classpath
        ).compile()

        val libraryEditor = NewLibraryEditor().apply {
            name = MOCK_LIBRARY_NAME

            addRoot("jar://" + FileUtilRt.toSystemIndependentName(libraryJar.absolutePath) + "!/", OrderRootType.CLASSES)

            if (attachSources) {
                addRoot("file://" + FileUtilRt.toSystemIndependentName(source.absolutePath), OrderRootType.SOURCES)
            }
        }

        val libraryKind = if (platform is JsPlatform) JSLibraryKind else null
        ConfigLibraryUtil.addLibrary(libraryEditor, module, libraryKind)
    }

    fun tearDown(module: Module) {
        ConfigLibraryUtil.removeLibrary(module, MOCK_LIBRARY_NAME)
    }
}