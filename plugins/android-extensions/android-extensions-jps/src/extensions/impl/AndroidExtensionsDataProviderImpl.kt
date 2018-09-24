/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.jps.android.model.base.impl

import org.jetbrains.jps.android.AndroidJpsUtil
import org.jetbrains.jps.android.model.base.AndroidExtensionsDataProvider
import org.jetbrains.jps.model.ex.JpsElementBase
import org.jetbrains.jps.model.module.JpsModule
import java.io.File

class AndroidExtensionsDataProviderImpl : JpsElementBase<AndroidExtensionsDataProviderImpl>(),
    AndroidExtensionsDataProvider {
    private val module
        get() = super.getParent() as JpsModule

    override fun applyChanges(modified: AndroidExtensionsDataProviderImpl) {
        fireElementChanged()
    }

    override fun createCopy(): AndroidExtensionsDataProviderImpl {
        return AndroidExtensionsDataProviderImpl()
    }

    private val androidExtension
        get() = AndroidJpsUtil.getExtension(module)

    override fun isGradleProject(): Boolean {
        return androidExtension?.isGradleProject ?: false
    }

    override fun getResourceDirForCompilationPath(): File? {
        val androidExtension = this.androidExtension ?: return null
        return AndroidJpsUtil.getResourceDirForCompilationPath(androidExtension)
    }

    override fun getManifestFileForCompilationPath(): File? {
        val androidExtension = this.androidExtension ?: return null
        return AndroidJpsUtil.getManifestFileForCompilationPath(androidExtension)
    }
}