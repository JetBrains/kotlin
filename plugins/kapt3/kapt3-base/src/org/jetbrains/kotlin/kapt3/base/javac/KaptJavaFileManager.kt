/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.base.javac

import com.sun.tools.javac.file.BaseFileObject
import com.sun.tools.javac.file.JavacFileManager
import com.sun.tools.javac.main.Option
import com.sun.tools.javac.util.Context
import java.io.File
import java.util.*
import javax.tools.JavaFileManager
import javax.tools.JavaFileObject
import javax.tools.StandardLocation

class KaptJavaFileManager(context: Context) : JavacFileManager(context, true, null) {
    fun handleOptionJavac9(option: Option, value: String) {
        val handleOptionMethod = JavacFileManager::class.java
            .getMethod("handleOption", Option::class.java, String::class.java)

        handleOptionMethod.invoke(this, option, value)
    }

    var rootsToFilter = emptySet<File>()
    // TODO (gavra): store these more efficiently by package (requires some changes to type collection).
    var typeToIgnore = emptySet<String>()

    override fun list(
        location: JavaFileManager.Location?,
        packageName: String?,
        kinds: MutableSet<JavaFileObject.Kind>?,
        recurse: Boolean
    ): MutableIterable<JavaFileObject> {
        val originalList = super.list(location, packageName, kinds, recurse)

        if (location == null
            || location != StandardLocation.CLASS_PATH
            || rootsToFilter.isEmpty()
            || typeToIgnore.isEmpty()
            || !filterThisPath(packageName)
        ) {
            return originalList
        }

        val filteredList = LinkedList<JavaFileObject>()
        for (file in originalList)
            if (!shouldBeFiltered(packageName, file)) {
                filteredList.add(file)
            }

        return filteredList
    }


    private fun filterThisPath(packageName: String?): Boolean {
        packageName ?: return false

        val relativePath = packageName.replace('.', File.separatorChar)
        return rootsToFilter.any { it.resolve(relativePath).isDirectory }
    }

    private fun shouldBeFiltered(packageName: String?, fileObject: JavaFileObject): Boolean {
        if (fileObject.kind != JavaFileObject.Kind.CLASS) return false
        return when (fileObject.toUri().scheme) {
            "jar", "zip" -> false
            else -> {
                if (fileObject !is BaseFileObject) return false
                val typeName = packageName?.let { "$it." } + fileObject.shortName.dropLast(".class".length)

                return typeToIgnore.contains(typeName)
            }
        }
    }

    companion object {
        internal fun preRegister(context: Context) {
            context.put(JavaFileManager::class.java, Context.Factory<JavaFileManager> { KaptJavaFileManager(it) })
        }
    }
}