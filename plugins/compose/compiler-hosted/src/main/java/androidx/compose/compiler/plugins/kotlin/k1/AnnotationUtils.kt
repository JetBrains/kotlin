/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.compiler.plugins.kotlin.k1

import androidx.compose.compiler.plugins.kotlin.ComposeClassIds
import androidx.compose.compiler.plugins.kotlin.ComposeFqNames
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.replaceAnnotations

private fun makeComposableAnnotation(module: ModuleDescriptor): AnnotationDescriptor =
    object : AnnotationDescriptor {
        override val type: KotlinType
            get() = module.findClassAcrossModuleDependencies(
                ComposeClassIds.Composable
            )!!.defaultType
        override val allValueArguments: Map<Name, ConstantValue<*>> get() = emptyMap()
        override val source: SourceElement get() = SourceElement.NO_SOURCE
        override fun toString() = "[@Composable]"
    }

fun KotlinType.makeComposable(module: ModuleDescriptor): KotlinType {
    if (hasComposableAnnotation()) return this
    val annotation = makeComposableAnnotation(module)
    return replaceAnnotations(Annotations.create(annotations + annotation))
}

fun AnonymousFunctionDescriptor.annotateAsComposable(module: ModuleDescriptor) =
    AnonymousFunctionDescriptor(
        containingDeclaration,
        Annotations.create(annotations + makeComposableAnnotation(module)),
        kind,
        source,
        isSuspend
    )

fun KotlinType.hasComposableAnnotation(): Boolean =
    !isSpecialType && annotations.findAnnotation(ComposeFqNames.Composable) != null
fun Annotated.hasComposableAnnotation(): Boolean =
    annotations.findAnnotation(ComposeFqNames.Composable) != null
fun Annotated.hasReadonlyComposableAnnotation(): Boolean =
    annotations.findAnnotation(ComposeFqNames.ReadOnlyComposable) != null
fun Annotated.hasDisallowComposableCallsAnnotation(): Boolean =
    annotations.findAnnotation(ComposeFqNames.DisallowComposableCalls) != null
fun Annotated.compositionTarget(): String? =
    annotations.map { it.compositionTarget() }.firstOrNull { it != null }

fun Annotated.hasCompositionTargetMarker(): Boolean =
    annotations.findAnnotation(
        ComposeFqNames.ComposableTargetMarker
    ) != null

fun AnnotationDescriptor.compositionTarget(): String? =
    if (fqName == ComposeFqNames.ComposableTarget)
        allValueArguments[ComposeFqNames.ComposableTargetApplierArgument]?.value as? String
    else if (annotationClass?.hasCompositionTargetMarker() == true) this.fqName.toString() else null

fun Annotated.compositionScheme(): String? =
    annotations.findAnnotation(
        ComposeFqNames.ComposableInferredTarget
    )?.allValueArguments?.let {
        it[ComposeFqNames.ComposableInferredTargetSchemeArgument]?.value as? String
    }

fun Annotated.compositionOpenTarget(): Int? =
    annotations.findAnnotation(
        ComposeFqNames.ComposableOpenTarget
    )?.allValueArguments?.let {
        it[ComposeFqNames.ComposableOpenTargetIndexArgument]?.value as Int
    }

internal val KotlinType.isSpecialType: Boolean get() =
    this === TypeUtils.NO_EXPECTED_TYPE || this === TypeUtils.UNIT_EXPECTED_TYPE

val AnnotationDescriptor.isComposableAnnotation: Boolean get() = fqName == ComposeFqNames.Composable
