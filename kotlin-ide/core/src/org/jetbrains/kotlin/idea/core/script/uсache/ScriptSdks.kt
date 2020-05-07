/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.uсache

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.util.application.runReadAction
import java.io.File

class ScriptSdks(val project: Project, private val sdks: Map<String?, Sdk?>) {
    fun rebuild(remove: Sdk?): ScriptSdks {
        val builder = ScriptSdksBuilder(project, remove = remove)
        sdks.keys.forEach { home ->
            if (home != null) {
                builder.addSdk(File(home))
            }
        }
        return builder.build()
    }

    val nonIndexedClassRoots = mutableSetOf<VirtualFile>()
    val nonIndexedSourceRoots = mutableSetOf<VirtualFile>()

    val first: Sdk? = sdks.values.firstOrNull()
    operator fun get(sdkHome: String?) = sdks[sdkHome]

    init {
        val nonIndexedSdks = sdks.values.filterNotNullTo(mutableSetOf())

        runReadAction {
            ModuleManager.getInstance(project).modules.map {
                nonIndexedSdks.remove(ModuleRootManager.getInstance(it).sdk)
            }

            nonIndexedSdks.forEach {
                nonIndexedClassRoots.addAll(it.rootProvider.getFiles(OrderRootType.CLASSES))
                nonIndexedSourceRoots.addAll(it.rootProvider.getFiles(OrderRootType.SOURCES))
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ScriptSdks

        if (nonIndexedClassRoots != other.nonIndexedClassRoots) return false
        if (nonIndexedSourceRoots != other.nonIndexedSourceRoots) return false

        return true
    }

    override fun hashCode(): Int {
        var result = nonIndexedClassRoots.hashCode()
        result = 31 * result + nonIndexedSourceRoots.hashCode()
        return result
    }
}