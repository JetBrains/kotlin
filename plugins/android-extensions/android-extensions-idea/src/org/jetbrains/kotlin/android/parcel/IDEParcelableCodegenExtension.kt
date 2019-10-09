/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.parcel

import org.jetbrains.kotlin.android.synthetic.idea.androidExtensionsIsExperimental
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.psi.KtElement

class IDEParcelableCodegenExtension : ParcelableCodegenExtension() {
    override fun isExperimental(element: KtElement): Boolean {
        val moduleInfo = element.getModuleInfo()
        return moduleInfo.androidExtensionsIsExperimental
    }
}