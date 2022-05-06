/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin

import androidx.compose.compiler.plugins.kotlin.ComposeErrors.ABSTRACT_COMPOSABLE_DEFAULT_PARAMETER_VALUE
import androidx.compose.compiler.plugins.kotlin.ComposeErrors.COMPOSABLE_FUN_MAIN
import androidx.compose.compiler.plugins.kotlin.ComposeErrors.COMPOSABLE_PROPERTY_BACKING_FIELD
import androidx.compose.compiler.plugins.kotlin.ComposeErrors.COMPOSABLE_SUSPEND_FUN
import androidx.compose.compiler.plugins.kotlin.ComposeErrors.COMPOSABLE_VAR
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.isSuspendFunctionType
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.types.KotlinType

class ComposableDeclarationChecker : DeclarationChecker, StorageComponentContainerContributor {
    override fun registerModuleComponents(
        container: StorageComponentContainer,
        platform: TargetPlatform,
        moduleDescriptor: ModuleDescriptor
    ) {
        container.useInstance(this)
    }

    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        when {
            declaration is KtProperty &&
                descriptor is PropertyDescriptor -> checkProperty(declaration, descriptor, context)
            declaration is KtPropertyAccessor &&
                descriptor is PropertyAccessorDescriptor -> checkPropertyAccessor(
                declaration,
                descriptor,
                context
            )
            declaration is KtFunction &&
                descriptor is FunctionDescriptor -> checkFunction(declaration, descriptor, context)
        }
    }

    private fun checkFunction(
        declaration: KtFunction,
        descriptor: FunctionDescriptor,
        context: DeclarationCheckerContext
    ) {
        val hasComposableAnnotation = descriptor.hasComposableAnnotation()
        if (descriptor.overriddenDescriptors.isNotEmpty()) {
            val override = descriptor.overriddenDescriptors.first()
            if (override.hasComposableAnnotation() != hasComposableAnnotation) {
                context.trace.report(
                    ComposeErrors.CONFLICTING_OVERLOADS.on(
                        declaration,
                        listOf(descriptor, override)
                    )
                )
            }

            descriptor.valueParameters.forEach { valueParameter ->
                valueParameter.overriddenDescriptors.firstOrNull()?.let { overriddenParam ->
                    val overrideIsComposable = overriddenParam.type.hasComposableAnnotation()
                    val paramIsComposable = valueParameter.type.hasComposableAnnotation()
                    if (paramIsComposable != overrideIsComposable) {
                        context.trace.report(
                            ComposeErrors.CONFLICTING_OVERLOADS.on(
                                declaration,
                                listOf(valueParameter, overriddenParam)
                            )
                        )
                    }
                }
            }
        }
        if (descriptor.isSuspend && hasComposableAnnotation) {
            context.trace.report(
                COMPOSABLE_SUSPEND_FUN.on(declaration.nameIdentifier ?: declaration)
            )
        }

        if (hasComposableAnnotation && descriptor.modality == Modality.ABSTRACT) {
            declaration.valueParameters.forEach {
                val defaultValue = it.defaultValue
                if (defaultValue != null) {
                    context.trace.report(
                        ABSTRACT_COMPOSABLE_DEFAULT_PARAMETER_VALUE.on(defaultValue)
                    )
                }
            }
        }
        val params = descriptor.valueParameters
        val ktparams = declaration.valueParameters
        if (params.size == ktparams.size) {
            for ((param, ktparam) in params.zip(ktparams)) {
                val typeRef = ktparam.typeReference
                if (typeRef != null) {
                    checkType(param.type, typeRef, context)
                }
            }
        }
        // NOTE: only use the MainFunctionDetector if the descriptor name is main, to avoid
        // unnecessarily allocating this class
        if (hasComposableAnnotation &&
            descriptor.name.asString() == "main" &&
            MainFunctionDetector(
                    context.trace.bindingContext,
                    context.languageVersionSettings
                ).isMain(descriptor)
        ) {
            context.trace.report(
                COMPOSABLE_FUN_MAIN.on(declaration.nameIdentifier ?: declaration)
            )
        }
    }

    private fun checkType(
        type: KotlinType,
        element: PsiElement,
        context: DeclarationCheckerContext
    ) {
        if (type.hasComposableAnnotation() && type.isSuspendFunctionType) {
            context.trace.report(
                COMPOSABLE_SUSPEND_FUN.on(element)
            )
        }
    }

    private fun checkProperty(
        declaration: KtProperty,
        descriptor: PropertyDescriptor,
        context: DeclarationCheckerContext
    ) {
        val hasComposableAnnotation = descriptor
            .getter
            ?.hasComposableAnnotation() == true
        if (descriptor.overriddenDescriptors.isNotEmpty()) {
            val override = descriptor.overriddenDescriptors.first()
            val overrideIsComposable = override.hasComposableAnnotation() ||
                override.getter?.hasComposableAnnotation() == true
            if (overrideIsComposable != hasComposableAnnotation) {
                context.trace.report(
                    ComposeErrors.CONFLICTING_OVERLOADS.on(
                        declaration,
                        listOf(descriptor, override)
                    )
                )
            }
        }
        if (!hasComposableAnnotation) return
        val initializer = declaration.initializer
        val name = declaration.nameIdentifier
        if (initializer != null && name != null) {
            context.trace.report(COMPOSABLE_PROPERTY_BACKING_FIELD.on(name))
        }
        if (descriptor.isVar && name != null) {
            context.trace.report(COMPOSABLE_VAR.on(name))
        }
    }

    private fun checkPropertyAccessor(
        declaration: KtPropertyAccessor,
        descriptor: PropertyAccessorDescriptor,
        context: DeclarationCheckerContext
    ) {
        val propertyDescriptor = descriptor.correspondingProperty
        val propertyPsi = declaration.parent as? KtProperty ?: return
        val name = propertyPsi.nameIdentifier
        val initializer = propertyPsi.initializer
        val hasComposableAnnotation = descriptor.hasComposableAnnotation()
        if (descriptor.overriddenDescriptors.isNotEmpty()) {
            val override = descriptor.overriddenDescriptors.first()
            val overrideComposable = override.hasComposableAnnotation()
            if (overrideComposable != hasComposableAnnotation) {
                context.trace.report(
                    ComposeErrors.CONFLICTING_OVERLOADS.on(
                        declaration,
                        listOf(descriptor, override)
                    )
                )
            }
        }
        if (!hasComposableAnnotation) return
        if (initializer != null && name != null) {
            context.trace.report(COMPOSABLE_PROPERTY_BACKING_FIELD.on(name))
        }
        if (propertyDescriptor.isVar && name != null) {
            context.trace.report(COMPOSABLE_VAR.on(name))
        }
    }
}
