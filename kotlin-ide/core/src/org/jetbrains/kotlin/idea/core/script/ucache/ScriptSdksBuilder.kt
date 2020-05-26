/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.ucache

import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdkType
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VfsUtil
import org.jetbrains.kotlin.idea.caches.project.getAllProjectSdks
import org.jetbrains.kotlin.idea.core.script.LOG
import org.jetbrains.kotlin.idea.core.script.configuration.utils.ScriptClassRootsStorage
import org.jetbrains.kotlin.idea.util.getProjectJdkTableSafe
import java.io.File

class ScriptSdksBuilder(
    val project: Project,
    private val sdks: MutableMap<SdkId, Sdk?> = mutableMapOf(),
    private val remove: Sdk? = null
) {
    private val defaultSdk by lazy { getScriptDefaultSdk() }

    fun build() = ScriptSdks(project, sdks)

    @Deprecated("Don't use, used only from DefaultScriptingSupport for saving to storage")
    fun addAll(other: ScriptSdksBuilder) {
        sdks.putAll(other.sdks)
    }

    // add sdk by home path with checking for removed sdk
    fun addSdk(sdkId: SdkId): Sdk? {
        val canonicalPath = sdkId.homeDirectory ?: return addDefaultSdk()
        return addSdk(File(canonicalPath))
    }

    fun addSdk(javaHome: File?): Sdk? {
        if (javaHome == null) return addDefaultSdk()

        return sdks.getOrPut(SdkId(javaHome)) {
            getScriptSdkByJavaHome(javaHome) ?: defaultSdk
        }
    }

    private fun getScriptSdkByJavaHome(javaHome: File): Sdk? {
        // workaround for mismatched gradle wrapper and plugin version
        val javaHomeVF = try {
            VfsUtil.findFileByIoFile(javaHome, true)
        } catch (e: Throwable) {
            null
        } ?: return null

        return getProjectJdkTableSafe().allJdks.find { it.homeDirectory == javaHomeVF }
            ?.takeIf { it.canBeUsedForScript() }
    }

    fun addDefaultSdk(): Sdk? =
        sdks.getOrPut(SdkId.default) { defaultSdk }

    fun addSdkByName(sdkName: String) {
        val sdk = getProjectJdkTableSafe().allJdks
            .find { it.name == sdkName }
            ?.takeIf { it.canBeUsedForScript() }
            ?: defaultSdk ?: return
        val homePath = sdk.homePath ?: return
        sdks[SdkId(homePath)] = sdk
    }

    private fun getScriptDefaultSdk(): Sdk? {
        val projectSdk = ProjectRootManager.getInstance(project).projectSdk?.takeIf { it.canBeUsedForScript() }
        if (projectSdk != null) return projectSdk

        val anyJavaSdk = getAllProjectSdks().find { it.canBeUsedForScript() }
        if (anyJavaSdk != null) {
            return anyJavaSdk
        }

        LOG.warn(
            "Default Script SDK is null: " +
                    "projectSdk = ${ProjectRootManager.getInstance(project).projectSdk}, " +
                    "all sdks = ${getAllProjectSdks().joinToString("\n")}"
        )

        return null
    }

    private fun Sdk.canBeUsedForScript() = this != remove && sdkType is JavaSdkType

    fun toStorage(storage: ScriptClassRootsStorage) {
        storage.sdks = sdks.values.mapNotNullTo(mutableSetOf()) { it?.name }
        storage.defaultSdkUsed = sdks.containsKey(SdkId.default)
    }

    fun fromStorage(storage: ScriptClassRootsStorage) {
        storage.sdks.forEach {
            addSdkByName(it)
        }
        if (storage.defaultSdkUsed) {
            addDefaultSdk()
        }
    }
}
