// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.stats.completion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupEvent
import com.intellij.codeInsight.lookup.LookupListener
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.completion.FeatureManagerImpl
import com.intellij.stats.personalization.UserFactorDescriptions
import com.intellij.stats.personalization.UserFactorStorage
import com.jetbrains.completion.feature.impl.FeatureUtils

/**
 * @author Vitaliy.Bibaev
 */
class LookupCompletedTracker : LookupListener {
    override fun lookupCanceled(event: LookupEvent) {
        val lookup = event.lookup as? LookupImpl ?: return
        val element = lookup.currentItem
        if (element != null && isSelectedByTyping(lookup, element)) {
            processTypedSelect(lookup, element)
        } else {
            UserFactorStorage.applyOnBoth(lookup.project, UserFactorDescriptions.COMPLETION_FINISH_TYPE) { updater ->
                updater.fireLookupCancelled()
            }
        }
    }

    override fun itemSelected(event: LookupEvent) {
        val lookup = event.lookup as? LookupImpl ?: return
        val element = event.item ?: return
        processExplicitSelect(lookup, element)
    }

    private fun isSelectedByTyping(lookup: LookupImpl, element: LookupElement): Boolean =
            element.lookupString == lookup.itemPattern(element)

    private fun processElementSelected(lookup: LookupImpl, element: LookupElement) {
        val relevanceObjects =
                lookup.getRelevanceObjects(listOf(element), false)
        val relevanceMap = relevanceObjects[element]?.map { it.first to it.second } ?: return
        val featuresValues = FeatureUtils.prepareRevelanceMap(relevanceMap, lookup.selectedIndex,
                lookup.prefixLength(), element.lookupString.length)
        val project = lookup.project
        val featureManager = FeatureManagerImpl.getInstance()
        featureManager.binaryFactors.filter { !featureManager.isUserFeature(it.name) }.forEach { feature ->
            UserFactorStorage.applyOnBoth(project, UserFactorDescriptions.binaryFeatureDescriptor(feature))
            { updater ->
                updater.update(featuresValues[feature.name])
            }
        }

        featureManager.doubleFactors.filter { !featureManager.isUserFeature(it.name) }.forEach { feature ->
            UserFactorStorage.applyOnBoth(project, UserFactorDescriptions.doubleFeatureDescriptor(feature))
            { updater ->
                updater.update(featuresValues[feature.name])
            }
        }

        featureManager.categoricalFactors.filter { !featureManager.isUserFeature(it.name) }.forEach { feature ->
            UserFactorStorage.applyOnBoth(project, UserFactorDescriptions.categoricalFeatureDescriptor(feature))
            { updater ->
                updater.update(featuresValues[feature.name])
            }
        }
    }

    private fun processExplicitSelect(lookup: LookupImpl, element: LookupElement) {
        processElementSelected(lookup, element)

        UserFactorStorage.applyOnBoth(lookup.project, UserFactorDescriptions.COMPLETION_FINISH_TYPE) { updater ->
            updater.fireExplicitCompletionPerformed()
        }

        val prefixLength = lookup.getPrefixLength(element)
        UserFactorStorage.applyOnBoth(lookup.project, UserFactorDescriptions.PREFIX_LENGTH_ON_COMPLETION) { updater ->
            updater.fireCompletionPerformed(prefixLength)
        }

        val itemPosition = lookup.selectedIndex
        if (itemPosition != -1) {
            UserFactorStorage.applyOnBoth(lookup.project, UserFactorDescriptions.SELECTED_ITEM_POSITION) { updater ->
                updater.fireCompletionPerformed(itemPosition)
            }
        }

        if (prefixLength > 1) {
            val pattern = lookup.itemPattern(element)
            val isMnemonicsUsed = !element.lookupString.startsWith(pattern)
            UserFactorStorage.applyOnBoth(lookup.project, UserFactorDescriptions.MNEMONICS_USAGE) { updater ->
                updater.fireCompletionFinished(isMnemonicsUsed)
            }
        }
    }

    private fun processTypedSelect(lookup: LookupImpl, element: LookupElement) {
        processElementSelected(lookup, element)

        UserFactorStorage.applyOnBoth(lookup.project, UserFactorDescriptions.COMPLETION_FINISH_TYPE) { updater ->
            updater.fireTypedSelectPerformed()
        }
    }
}
