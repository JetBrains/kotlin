// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.completion

import com.intellij.openapi.extensions.ExtensionPointName

interface CompletionTrackerDisabler {
    companion object {
        val EpName = ExtensionPointName<CompletionTrackerDisabler>("com.intellij.stats.completion.tracker.disabler")

        fun isDisabled(): Boolean = EpName.extensionList.any { it.isDisabled() }
    }

    fun isDisabled(): Boolean
}