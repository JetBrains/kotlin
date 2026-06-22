/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.java

import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaClassifier
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaType
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import kotlin.collections.get

internal class JavaTypeParameterStub(val original: JavaTypeParameter) : JavaTypeParameter {
    override val name: Name
        get() = original.name
    override val isFromSource: Boolean
        get() = true
    override val annotations: Collection<JavaAnnotation>
        get() = original.annotations
    override val isDeprecatedInJavaDoc: Boolean
        get() = original.isDeprecatedInJavaDoc

    override fun findAnnotation(fqName: FqName): JavaAnnotation? {
        return original.findAnnotation(fqName)
    }

    override val upperBounds: Collection<JavaClassifierType>
        get() = original.upperBounds
}

private class JavaClassifierTypeStub(
    val original: JavaClassifierType,
    override val typeArguments: List<JavaType?>,
) : JavaClassifierType {
    override val annotations: Collection<JavaAnnotation>
        get() = original.annotations
    override val isDeprecatedInJavaDoc: Boolean
        get() = original.isDeprecatedInJavaDoc
    override val classifier: JavaClassifier?
        get() = original.classifier
    override val isRaw: Boolean
        get() = original.isRaw
    override val classifierQualifiedName: String
        get() = original.classifierQualifiedName
    override val presentableText: String
        get() = original.presentableText
}

internal class JavaTypeParameterTypeStub(
    override val classifier: JavaTypeParameter,
) : JavaClassifierType {
    override val annotations: Collection<JavaAnnotation>
        get() = emptyList()
    override val isDeprecatedInJavaDoc: Boolean
        get() = false
    override val typeArguments: List<JavaType?>
        get() = emptyList()
    override val isRaw: Boolean
        get() = false
    override val classifierQualifiedName: String
        get() = classifier.name.identifier
    override val presentableText: String
        get() = classifierQualifiedName
}

internal sealed class JavaTypeSubstitutor {
    object Empty : JavaTypeSubstitutor() {
        override fun substituteOrNull(type: JavaType): JavaType? {
            return null
        }
    }

    fun substituteOrSelf(type: JavaType): JavaType {
        return substituteOrNull(type) ?: type
    }

    abstract fun substituteOrNull(type: JavaType): JavaType?
}

internal class JavaTypeSubstitutorByMap(val map: Map<JavaClassifier, JavaType>) : JavaTypeSubstitutor() {
    override fun substituteOrNull(type: JavaType): JavaType? {
        if (type !is JavaClassifierType) return null
        map[type.classifier]?.let { return it }
        var hasNewArguments = false
        val newArguments = type.typeArguments.map { argument ->
            if (argument == null) return@map null
            val newArgument = substituteOrSelf(argument)
            if (newArgument !== argument) {
                hasNewArguments = true
                newArgument
            } else {
                argument
            }
        }
        return runIf(hasNewArguments) {
            JavaClassifierTypeStub(type, newArguments)
        }
    }
}
