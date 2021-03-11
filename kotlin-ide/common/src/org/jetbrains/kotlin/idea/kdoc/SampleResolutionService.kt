/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.kdoc

import com.intellij.openapi.components.ServiceManager
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.resolve.BindingContext

interface SampleResolutionService {
    fun resolveSample(
        context: BindingContext,
        fromDescriptor: DeclarationDescriptor,
        resolutionFacade: ResolutionFacade,
        qualifiedName: List<String>
    ): Collection<DeclarationDescriptor>

    companion object {

        /**
         * It's internal implementation, please use [resolveKDocSampleLink], or [resolveKDocLink]
         */
        internal fun resolveSample(
            context: BindingContext,
            fromDescriptor: DeclarationDescriptor,
            resolutionFacade: ResolutionFacade,
            qualifiedName: List<String>
        ): Collection<DeclarationDescriptor> {
            val instance = ServiceManager.getService(resolutionFacade.project, SampleResolutionService::class.java)
            return instance?.resolveSample(context, fromDescriptor, resolutionFacade, qualifiedName) ?: emptyList()
        }
    }
}
