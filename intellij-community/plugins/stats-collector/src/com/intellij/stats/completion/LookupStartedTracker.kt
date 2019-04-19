// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.stats.completion

import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupEvent
import com.intellij.codeInsight.lookup.LookupListener
import com.intellij.stats.personalization.UserFactorDescriptions
import com.intellij.stats.personalization.UserFactorStorage

/**
 * @author Vitaliy.Bibaev
 */
class LookupStartedTracker : LookupListener {
    override fun currentItemChanged(event: LookupEvent) {
        val lookup = event.lookup ?: return
        if (processLookupStarted(lookup)) lookup.removeLookupListener(this)
    }

    private fun processLookupStarted(lookup: Lookup): Boolean {
        val completionType = CompletionUtil.getCurrentCompletionParameters()?.completionType ?: return false
        UserFactorStorage.applyOnBoth(lookup.project, UserFactorDescriptions.COMPLETION_TYPE) {
            it.fireCompletionPerformed(completionType)
        }

        return true
    }
}