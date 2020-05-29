/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.ide.ui.UISettings
import com.intellij.ui.breadcrumbs.BreadcrumbsProvider

// FIX ME WHEN BUNCH 201 REMOVED
abstract class BreadcrumbsProviderCompatBase : BreadcrumbsProvider {
    override fun isShownByDefault(): Boolean =
        !UISettings.instance.showMembersInNavigationBar
}
