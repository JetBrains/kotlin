/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.framework

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.JarUtil
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.artifacts.KotlinArtifactNames
import org.jetbrains.kotlin.utils.LibraryUtils
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.util.jar.Attributes

object JsLibraryStdDetectionUtil {
    private val IS_JS_LIBRARY_STD_LIB = Key.create<Boolean>("IS_JS_LIBRARY_STD_LIB")

    fun hasJsStdlibJar(library: Library, project: Project, ignoreKind: Boolean = false): Boolean {
        if (library !is LibraryEx || library.isDisposed) return false
        if (!ignoreKind && library.effectiveKind(project) !is JSLibraryKind) return false

        val classes = listOf(*library.getFiles(OrderRootType.CLASSES))
        return getJsStdLibJar(classes) != null
    }

    fun getJsLibraryStdVersion(library: Library, project: Project): String? {
        if ((library as LibraryEx).effectiveKind(project) !is JSLibraryKind) return null
        val jar = getJsStdLibJar(library.getFiles(OrderRootType.CLASSES).toList()) ?: return null

        return JarUtil.getJarAttribute(VfsUtilCore.virtualToIoFile(jar), Attributes.Name.IMPLEMENTATION_VERSION)
    }

    fun getJsStdLibJar(classesRoots: List<VirtualFile>): VirtualFile? {
        for (root in classesRoots) {
            if (root.fileSystem.protocol !== StandardFileSystems.JAR_PROTOCOL) continue

            val name = root.url.substringBefore("!/").substringAfterLast('/')
            if (name == KotlinArtifactNames.KOTLIN_STDLIB_JS
                || name == "kotlin-jslib.jar" // Outdated JS stdlib name
                || PathUtil.KOTLIN_STDLIB_JS_JAR_PATTERN.matcher(name).matches()
                || PathUtil.KOTLIN_JS_LIBRARY_JAR_PATTERN.matcher(name).matches()
            ) {

                val jar = VfsUtilCore.getVirtualFileForJar(root) ?: continue
                var isJSStdLib = jar.getUserData(IS_JS_LIBRARY_STD_LIB)
                if (isJSStdLib == null) {
                    isJSStdLib = LibraryUtils.isKotlinJavascriptStdLibrary(File(jar.path))
                    jar.putUserData(IS_JS_LIBRARY_STD_LIB, isJSStdLib)
                }

                if (isJSStdLib) {
                    return jar
                }
            }
        }

        return null
    }
}
