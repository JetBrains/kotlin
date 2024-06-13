/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.build

import org.jetbrains.jps.model.JpsProject
import org.jetbrains.jps.model.java.JpsJavaDependencyScope
import org.jetbrains.jps.model.java.JpsJavaLibraryType
import org.jetbrains.jps.model.library.JpsLibrary
import org.jetbrains.jps.model.library.JpsOrderRootType
import org.jetbrains.jps.model.module.JpsModule
import java.io.File
import java.io.IOException

abstract class AbstractKotlinJpsBuildTestCase : BaseKotlinJpsBuildTestCase() {
    protected lateinit var workDir: File

    @Throws(IOException::class)
    override fun doGetProjectDir(): File? {
        return workDir
    }

    protected fun addKotlinMockRuntimeDependency(): JpsLibrary {
        return addDependency(KotlinJpsLibrary.MockRuntime)
    }

    protected fun addKotlinStdlibDependency(): JpsLibrary {
        return addDependency(KotlinJpsLibrary.JvmStdLib)
    }

    protected fun addKotlinJavaScriptStdlibDependency(): JpsLibrary {
        return addDependency(KotlinJpsLibrary.JsStdLib)
    }

    protected fun addKotlinLombokDependency(): JpsLibrary {
        return addDependency(KotlinJpsLibrary.Lombok)
    }

    private fun addDependency(library: KotlinJpsLibrary): JpsLibrary {
        return addDependency(myProject, library)
    }

    protected fun addDependency(libraryName: String, libraryFile: File): JpsLibrary {
        return addDependency(myProject, libraryName, libraryFile)
    }

    companion object {
        val TEST_DATA_PATH get() = System.getProperty("jps.testData.home") ?: "jps/jps-plugin/testData/"

        @JvmStatic
        protected fun addKotlinStdlibDependency(modules: Collection<JpsModule>, exported: Boolean = false): JpsLibrary {
            return addDependency(modules, KotlinJpsLibrary.JvmStdLib, exported)
        }

        @JvmStatic
        private fun addDependency(project: JpsProject, library: KotlinJpsLibrary, exported: Boolean = false): JpsLibrary {
            return addDependency(JpsJavaDependencyScope.COMPILE, project.modules, exported, library.id, *library.roots)
        }

        @JvmStatic
        private fun addDependency(modules: Collection<JpsModule>, library: KotlinJpsLibrary, exported: Boolean = false): JpsLibrary {
            return addDependency(JpsJavaDependencyScope.COMPILE, modules, exported, library.id, *library.roots)
        }

        @JvmStatic
        private fun addDependency(project: JpsProject, libraryName: String, libraryFile: File): JpsLibrary {
            return addDependency(JpsJavaDependencyScope.COMPILE, project.modules, false, libraryName, libraryFile)
        }

        @JvmStatic
        protected fun addDependency(
            type: JpsJavaDependencyScope,
            modules: Collection<JpsModule>,
            exported: Boolean,
            libraryName: String,
            vararg file: File
        ): JpsLibrary {
            val library = modules.iterator().next().project.addLibrary(libraryName, JpsJavaLibraryType.INSTANCE)

            for (fileRoot in file) {
                library.addRoot(fileRoot, JpsOrderRootType.COMPILED)
            }

            for (module in modules) {
                JpsModuleRootModificationUtil.addDependency(module, library, type, exported)
            }
            return library
        }
    }
}
