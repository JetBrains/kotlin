/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.jps.android.model.base

import org.jetbrains.jps.model.JpsElement
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase
import org.jetbrains.jps.model.module.JpsModule
import java.io.File

interface AndroidExtensionsDataProvider : JpsElement {
    companion object {
        val KIND: JpsElementChildRoleBase<AndroidExtensionsDataProvider> =
            JpsElementChildRoleBase.create<AndroidExtensionsDataProvider>("android extensions data provider")

        fun getExtension(module: JpsModule): AndroidExtensionsDataProvider? {
            return module.container.getChild(KIND)
        }
    }

    fun isGradleProject(): Boolean
    fun getResourceDirForCompilationPath(): File?
    fun getManifestFileForCompilationPath(): File?
}

