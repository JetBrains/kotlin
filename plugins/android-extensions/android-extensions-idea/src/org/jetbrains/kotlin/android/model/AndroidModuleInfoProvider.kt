/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.model

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import java.io.File

val Module.isAndroidModule

    get() = AndroidModuleInfoProvider.getInstance(this)?.isAndroidModule() ?: false

interface AndroidModuleInfoProvider {
    companion object {
        val EP_NAME = ExtensionPointName.create<AndroidModuleInfoProvider>("org.jetbrains.kotlin.android.model.androidModuleInfoProvider")

        fun getInstance(module: Module): AndroidModuleInfoProvider? {
            val extensionArea = Extensions.getArea(module)
            if (!extensionArea.hasExtensionPoint(EP_NAME.name)) {
                return null
            }
            return extensionArea.getExtensionPoint(EP_NAME).extension
        }

        fun getInstance(element: PsiElement): AndroidModuleInfoProvider? {
            val module = ApplicationManager.getApplication().runReadAction<Module> {
                ModuleUtilCore.findModuleForPsiElement(element)
            }
            return getInstance(module)
        }
    }

    val module: Module

    fun isAndroidModule(): Boolean
    fun isGradleModule(): Boolean

    fun getApplicationPackage(): String?

    // For old Android Extensions
    fun getMainSourceProvider(): SourceProviderMirror?
    fun getFlavorSourceProviders(): List<SourceProviderMirror>
    fun getAllResourceDirectories(): List<VirtualFile>

    // For experimental Android Extensions
    fun getApplicationResourceDirectories(createIfNecessary: Boolean): Collection<VirtualFile>
    fun getAllSourceProviders(): List<SourceProviderMirror>
    fun getActiveSourceProviders(): List<SourceProviderMirror>


    interface SourceProviderMirror {
        val name: String
        val resDirectories: Collection<File>
    }
}

