/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.changeSignature.usages

import com.intellij.psi.PsiElement
import com.intellij.refactoring.changeSignature.JavaChangeInfo
import com.intellij.usageView.UsageInfo

class KotlinWrapperForJavaUsageInfos(
    val javaChangeInfo: JavaChangeInfo,
    val javaUsageInfos: Array<UsageInfo>,
    primaryMethod: PsiElement
) : UsageInfo(primaryMethod) {
    override fun hashCode() = javaChangeInfo.method.hashCode()

    override fun equals(other: Any?): Boolean {
        return other === this || (other is KotlinWrapperForJavaUsageInfos && javaChangeInfo.method == other.javaChangeInfo.method)
    }
}
