/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.CompletionLocation
import com.intellij.codeInsight.completion.CompletionStatistician
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.PsiElement
import com.intellij.psi.statistics.StatisticsInfo
import com.intellij.psi.util.ProximityLocation
import com.intellij.psi.util.proximity.ProximityStatistician
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.completion.DeclarationLookupObject
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.ParameterNameRenderingPolicy

class KotlinCompletionStatistician : CompletionStatistician() {
    override fun serialize(element: LookupElement, location: CompletionLocation): StatisticsInfo? {
        val o = (element.`object` as? DeclarationLookupObject) ?: return null

        val context = element.getUserDataDeep(STATISTICS_INFO_CONTEXT_KEY) ?: ""

        if (o.descriptor != null) {
            return KotlinStatisticsInfo.forDescriptor(o.descriptor!!.original, context)
        } else {
            val fqName = o.importableFqName ?: return StatisticsInfo.EMPTY
            return StatisticsInfo(context, fqName.asString())
        }
    }
}

class KotlinProximityStatistician : ProximityStatistician() {
    override fun serialize(element: PsiElement, location: ProximityLocation): StatisticsInfo? {
        if (element !is KtDeclaration) return null
        val descriptor = element.resolveToDescriptorIfAny() ?: return null
        return KotlinStatisticsInfo.forDescriptor(descriptor)
    }
}

object KotlinStatisticsInfo {
    private val SIGNATURE_RENDERER = DescriptorRenderer.withOptions {
        withDefinedIn = false
        withoutReturnType = true
        startFromName = true
        receiverAfterName = true
        modifiers = emptySet()
        defaultParameterValueRenderer = null
        parameterNameRenderingPolicy = ParameterNameRenderingPolicy.NONE
    }

    fun forDescriptor(descriptor: DeclarationDescriptor, context: String = ""): StatisticsInfo {
        if (descriptor is ClassDescriptor) {
            return descriptor.importableFqName?.let { StatisticsInfo("", it.asString()) } ?: StatisticsInfo.EMPTY
        }

        val containerFqName = when (val container = descriptor.containingDeclaration) {
            is ClassDescriptor -> container.importableFqName?.asString()
            is PackageFragmentDescriptor -> container.fqName.asString()
            is ModuleDescriptor -> ""
            else -> null
        } ?: return StatisticsInfo.EMPTY
        val signature = SIGNATURE_RENDERER.render(descriptor)
        return StatisticsInfo(context, "$containerFqName###$signature")
    }
}
