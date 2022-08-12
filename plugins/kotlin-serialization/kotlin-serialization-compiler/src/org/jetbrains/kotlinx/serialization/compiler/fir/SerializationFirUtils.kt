/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.fir

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingDeclarationSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.outerClassSymbol
import org.jetbrains.kotlin.fir.declarations.findArgumentByName
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.expressions.classId
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationAnnotations
import org.jetbrains.kotlinx.serialization.compiler.resolve.isSerializableObject
import org.jetbrains.kotlinx.serialization.compiler.resolve.shouldHaveGeneratedMethodsInCompanion

internal val FirClassSymbol<*>.hasSerializableAnnotation
    get() = this.annotations.any {
        it.classId?.asSingleFqName()?.asString() == SerializationAnnotations.serializableAnnotationFqName.asString()
    }

internal val FirClassSymbol<*>.hasSerializableAnnotationWithoutArgs: Boolean
    get() {
        if (!hasSerializableAnnotation) return false
        val target = this.resolvedAnnotationsWithArguments.find {
            it.classId?.asSingleFqName()?.asString() == SerializationAnnotations.serializableAnnotationFqName.asString()
        }!!
        return target.findArgumentByName(Name.identifier("with")) == null
    }

internal val FirClassSymbol<*>.shouldHaveGeneratedMethodsInCompanion: Boolean
    get() = this.isSerializableObject || this.isSerializableEnum() || this.classKind == ClassKind.CLASS && hasSerializableAnnotation || this.isSealedSerializableInterface

internal val FirClassSymbol<*>.isSerializableObject: Boolean
    get() = classKind == ClassKind.OBJECT && hasSerializableAnnotation

internal val FirClassSymbol<*>.isInternallySerializableObject: Boolean
    get() = classKind == ClassKind.OBJECT && hasSerializableAnnotationWithoutArgs

internal val FirClassSymbol<*>.isSealedSerializableInterface: Boolean
    get() = classKind == ClassKind.INTERFACE && rawStatus.modality == Modality.SEALED && hasSerializableAnnotation

internal val FirClassSymbol<*>.isInternalSerializable: Boolean
    get() {
        if (classKind != ClassKind.CLASS) return false
        return hasSerializableAnnotationWithoutArgs
    }

internal fun FirClassSymbol<*>.isSerializableEnum(): Boolean = classKind == ClassKind.ENUM_CLASS && hasSerializableAnnotation

internal fun FirClassSymbol<*>.isInternallySerializableEnum(): Boolean =
    classKind == ClassKind.ENUM_CLASS && hasSerializableAnnotationWithoutArgs

internal val FirClassSymbol<*>.shouldHaveGeneratedSerializer: Boolean
    get() = (isInternalSerializable && isFinalOrOpen()) || isInternallySerializableEnum()

private fun FirClassSymbol<*>.isFinalOrOpen(): Boolean {
    val modality = rawStatus.modality
    // null means default modality, final
    return (modality == null || modality == Modality.FINAL || modality == Modality.OPEN)
}

internal fun FirSession.getSerializableClassDescriptorByCompanion(thisDescriptor: FirClassSymbol<*>): FirClassSymbol<*>? {
    if (thisDescriptor.isSerializableObject) return thisDescriptor
    if (!thisDescriptor.isCompanion) return null
    val classDescriptor = (thisDescriptor.getContainingDeclarationSymbol(this) as? FirClassSymbol<*>) ?: return null
    if (!classDescriptor.shouldHaveGeneratedMethodsInCompanion) return null
    return classDescriptor
}