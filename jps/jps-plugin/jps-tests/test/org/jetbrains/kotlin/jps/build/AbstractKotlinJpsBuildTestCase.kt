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

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.jps.model.JpsDummyElement
import org.jetbrains.jps.model.JpsModuleRootModificationUtil
import org.jetbrains.jps.model.JpsProject
import org.jetbrains.jps.model.java.JpsJavaDependencyScope
import org.jetbrains.jps.model.java.JpsJavaLibraryType
import org.jetbrains.jps.model.java.JpsJavaSdkType
import org.jetbrains.jps.model.library.JpsLibrary
import org.jetbrains.jps.model.library.JpsOrderRootType
import org.jetbrains.jps.model.library.sdk.JpsSdk
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.jps.util.JpsPathUtil
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.utils.PathUtil

import java.io.File
import java.io.IOException

abstract class AbstractKotlinJpsBuildTestCase : BaseKotlinJpsBuildTestCase() {
    protected lateinit var workDir: File

    @Throws(IOException::class)
    override fun doGetProjectDir(): File? {
        return workDir
    }

    override fun addJdk(name: String, path: String?): JpsSdk<JpsDummyElement> {
        val homePath = System.getProperty("java.home")
        val versionString = System.getProperty("java.version")
        val jdk = myModel.global.addSdk(name, homePath, versionString, JpsJavaSdkType.INSTANCE)
        jdk.addRoot(JpsPathUtil.pathToUrl(path), JpsOrderRootType.COMPILED)
        return jdk.properties
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

    private fun addDependency(library: KotlinJpsLibrary): JpsLibrary {
        return addDependency(myProject, library)
    }

    protected fun addDependency(libraryName: String, libraryFile: File): JpsLibrary {
        return addDependency(myProject, libraryName, libraryFile)
    }

    companion object {
        val TEST_DATA_PATH = "jps-plugin/testData/"

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
