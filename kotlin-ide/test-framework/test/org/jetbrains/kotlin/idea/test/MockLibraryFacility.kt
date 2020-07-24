package org.jetbrains.kotlin.idea.test

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.OrderRootType
import org.jetbrains.kotlin.idea.framework.JSLibraryKind
import org.jetbrains.kotlin.platform.js.JsPlatform
import org.jetbrains.kotlin.test.KotlinCompilerStandalone
import java.io.File

data class MockLibraryFacility(
    val sources: List<File>,
    val attachSources: Boolean = true,
    val platform: KotlinCompilerStandalone.Platform = KotlinCompilerStandalone.Platform.Jvm(),
    val options: List<String> = emptyList(),
    val classpath: List<File> = emptyList()
) {
    constructor(
        source: File,
        attachSources: Boolean = true,
        platform: KotlinCompilerStandalone.Platform = KotlinCompilerStandalone.Platform.Jvm(),
        options: List<String> = emptyList(),
        classpath: List<File> = emptyList()
    ) : this(listOf(source), attachSources, platform, options, classpath)

    companion object {
        const val MOCK_LIBRARY_NAME = "kotlinMockLibrary"
    }

    fun setUp(module: Module) {
        val libraryJar = KotlinCompilerStandalone(
            sources,
            platform = platform,
            options = options,
            classpath = classpath
        ).compile()

        val kind = if (platform is JsPlatform) JSLibraryKind else null
        ConfigLibraryUtil.addLibrary(module, MOCK_LIBRARY_NAME, kind) {
            addRoot(libraryJar, OrderRootType.CLASSES)

            if (attachSources) {
                for (source in sources) {
                    addRoot(source, OrderRootType.SOURCES)
                }
            }
        }
    }

    fun tearDown(module: Module) {
        ConfigLibraryUtil.removeLibrary(module, MOCK_LIBRARY_NAME)
    }
}