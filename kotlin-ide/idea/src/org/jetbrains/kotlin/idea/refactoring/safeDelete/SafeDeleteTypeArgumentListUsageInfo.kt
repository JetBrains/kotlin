/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.safeDelete

import com.intellij.refactoring.safeDelete.usageInfo.SafeDeleteReferenceSimpleDeleteUsageInfo
import org.jetbrains.kotlin.idea.core.deleteElementAndCleanParent
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.KtTypeProjection

class SafeDeleteTypeArgumentListUsageInfo(
    typeProjection: KtTypeProjection, parameter: KtTypeParameter
) : SafeDeleteReferenceSimpleDeleteUsageInfo(typeProjection, parameter, true) {
    override fun deleteElement() {
        element?.deleteElementAndCleanParent()
    }
}
