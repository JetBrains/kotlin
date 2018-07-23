/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.model.impl

import com.android.builder.model.SourceProvider
import com.android.tools.idea.gradle.project.GradleProjectInfo
import com.android.tools.idea.gradle.project.model.AndroidModuleModel
import com.android.tools.idea.res.ResourceRepositoryManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.kotlin.android.model.AndroidModuleInfoProvider
import java.io.File

class AndroidModuleInfoProviderImpl(override val module: Module) : AndroidModuleInfoProvider {
    private val androidFacet: AndroidFacet?
        get() = AndroidFacet.getInstance(module)

    private val androidModuleModel: AndroidModuleModel?
        get() = AndroidModuleModel.get(module)

    override fun isAndroidModule() = androidFacet != null
    override fun isGradleModule() = GradleProjectInfo.getInstance(module.project).isBuildWithGradle

    override fun getAllResourceDirectories(): List<VirtualFile> {
        return androidFacet?.allResourceDirectories ?: emptyList()
    }

    override fun getApplicationPackage() = androidFacet?.manifest?.`package`?.toString()

    override fun getMainSourceProvider(): AndroidModuleInfoProvider.SourceProviderMirror? {
        return androidFacet?.mainSourceProvider?.let(::SourceProviderMirrorImpl)
    }

    override fun getApplicationResourceDirectories(createIfNecessary: Boolean): Collection<VirtualFile> {
        return ResourceRepositoryManager.getOrCreateInstance(module)?.getAppResources(createIfNecessary)?.resourceDirs ?: emptyList()
    }

    override fun getAllSourceProviders(): List<AndroidModuleInfoProvider.SourceProviderMirror> {
        val androidModuleModel = this.androidModuleModel ?: return emptyList()
        return androidModuleModel.allSourceProviders.map(::SourceProviderMirrorImpl)
    }

    override fun getActiveSourceProviders(): List<AndroidModuleInfoProvider.SourceProviderMirror> {
        val androidModuleModel = this.androidModuleModel ?: return emptyList()
        return androidModuleModel.activeSourceProviders.map(::SourceProviderMirrorImpl)
    }

    override fun getFlavorSourceProviders(): List<AndroidModuleInfoProvider.SourceProviderMirror> {
        val androidModuleModel = this.androidModuleModel ?: return emptyList()

        val getFlavorSourceProvidersMethod = try {
            AndroidFacet::class.java.getMethod("getFlavorSourceProviders")
        } catch (e: NoSuchMethodException) {
            null
        }

        return if (getFlavorSourceProvidersMethod != null) {
            @Suppress("UNCHECKED_CAST")
            val sourceProviders = getFlavorSourceProvidersMethod.invoke(androidFacet) as? List<SourceProvider>
            sourceProviders?.map(::SourceProviderMirrorImpl) ?: emptyList()
        } else {
            androidModuleModel.flavorSourceProviders.map(::SourceProviderMirrorImpl)
        }
    }

    private class SourceProviderMirrorImpl(val sourceProvider: SourceProvider) :
        AndroidModuleInfoProvider.SourceProviderMirror {
        override val name: String
            get() = sourceProvider.name

        override val resDirectories: Collection<File>
            get() = sourceProvider.resDirectories
    }
}