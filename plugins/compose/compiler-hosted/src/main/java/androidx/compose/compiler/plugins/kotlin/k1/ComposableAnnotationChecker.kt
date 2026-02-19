/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.plugins.kotlin.k1

import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.builtins.isKFunctionType
import org.jetbrains.kotlin.builtins.isKSuspendFunctionType
import org.jetbrains.kotlin.builtins.isSuspendFunctionType
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.resolve.AdditionalAnnotationChecker
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.types.AbbreviatedType

class ComposableAnnotationChecker : AdditionalAnnotationChecker, StorageComponentContainerContributor {
    override fun checkEntries(
        entries: List<KtAnnotationEntry>,
        actualTargets: List<KotlinTarget>,
        trace: BindingTrace,
        annotated: KtAnnotated?,
        languageVersionSettings: LanguageVersionSettings,
    ) {
        if (annotated is KtTypeReference && actualTargets.any { it == KotlinTarget.TYPE }) {
            val composableAnnotation = entries.find { trace[BindingContext.ANNOTATION, it]?.isComposableAnnotation == true }
            if (composableAnnotation == null) {
                return
            }

            val type = trace[BindingContext.TYPE, annotated] ?: return
            if (type.isSuspendFunctionType || type.isKSuspendFunctionType) {
                // Handled separately in ComposableDeclarationChecker
                return
            }
            if (type !is AbbreviatedType && (type.isFunctionType || type.isKFunctionType)) {
                return
            }

            trace.report(
                ComposeErrors.COMPOSABLE_INAPPLICABLE_TYPE.on(composableAnnotation, type)
            )
        }
    }

    override fun registerModuleComponents(
        container: StorageComponentContainer,
        platform: TargetPlatform,
        moduleDescriptor: ModuleDescriptor
    ) {
        container.useInstance(this)
    }
}