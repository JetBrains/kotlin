/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.diagnostic

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlinx.serialization.compiler.resolve.*

internal fun checkCompanionSerializerDependency(descriptor: ClassDescriptor, declaration: KtDeclaration, trace: BindingTrace) {
    val companionObjectDescriptor = descriptor.companionObjectDescriptor ?: return
    val serializerForInCompanion = companionObjectDescriptor.serializerForClass ?: return
    val serializerAnnotationSource =
        companionObjectDescriptor.findAnnotationDeclaration(SerializationAnnotations.serializerAnnotationFqName)
    val serializableWith = descriptor.serializableWith
    if (descriptor.hasSerializableOrMetaAnnotationWithoutArgs) {
        if (serializerForInCompanion == descriptor.defaultType) {
            // @Serializable class Foo / @Serializer(Foo::class) companion object — prohibited due to problems with recursive resolve
            descriptor.onSerializableOrMetaAnnotation {
                trace.report(SerializationErrors.COMPANION_OBJECT_AS_CUSTOM_SERIALIZER_DEPRECATED.on(it, descriptor))
            }
        } else {
            // @Serializable class Foo / @Serializer(Bar::class) companion object — prohibited as vague and confusing
            trace.report(
                SerializationErrors.COMPANION_OBJECT_SERIALIZER_INSIDE_OTHER_SERIALIZABLE_CLASS.on(
                    serializerAnnotationSource ?: declaration,
                    descriptor.defaultType,
                    serializerForInCompanion
                )
            )
        }
    } else if (serializableWith != null) {
        if (serializableWith == companionObjectDescriptor.defaultType && serializerForInCompanion == descriptor.defaultType) {
            // @Serializable(Foo.Companion) class Foo / @Serializer(Foo::class) companion object — the only case that is allowed
        } else {
            // @Serializable(anySer) class Foo / @Serializer(anyOtherClass) companion object — prohibited as vague and confusing
            trace.report(
                SerializationErrors.COMPANION_OBJECT_SERIALIZER_INSIDE_OTHER_SERIALIZABLE_CLASS.on(
                    serializerAnnotationSource ?: declaration,
                    descriptor.defaultType,
                    serializerForInCompanion
                )
            )
        }
    } else {
        // (regular) class Foo / @Serializer(something) companion object - not recommended
        trace.report(
            SerializationErrors.COMPANION_OBJECT_SERIALIZER_INSIDE_NON_SERIALIZABLE_CLASS.on(
                serializerAnnotationSource ?: declaration,
                descriptor.defaultType,
                serializerForInCompanion
            )
        )
    }
}

internal fun checkCompanionOfSerializableClass(descriptor: ClassDescriptor, trace: BindingTrace) {
    val companionObjectDescriptor = descriptor.companionObjectDescriptor ?: return
    if (!descriptor.hasSerializableOrMetaAnnotation) return
    if (!companionObjectDescriptor.hasSerializableOrMetaAnnotation) return

    val serializableArg = descriptor.serializableWith
    val companionArg = companionObjectDescriptor.serializableWith
    if (serializableArg != null && companionArg != null && serializableArg == companionArg) {
        // allowed
        return
    }
    // other versions are not allowed
    companionObjectDescriptor.onSerializableOrMetaAnnotation {
        trace.report(
            SerializationErrors.COMPANION_OBJECT_IS_SERIALIZABLE_INSIDE_SERIALIZABLE_CLASS.on(
                it,
                descriptor
            )
        )
    }
}
