/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.java

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.fir.types.jvm.buildJavaTypeRef
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

abstract class WrappedJavaType<T : JavaType>(val original: T, private val ownAnnotations: Collection<JavaAnnotation>?) : JavaType {
    override val annotations: Collection<JavaAnnotation>
        get() = ownAnnotations ?: original.annotations

    override val isDeprecatedInJavaDoc: Boolean
        get() = original.isDeprecatedInJavaDoc
}

class WrappedJavaArrayType(
    original: JavaArrayType,
    ownAnnotations: Collection<JavaAnnotation>?
) : WrappedJavaType<JavaArrayType>(original, ownAnnotations), JavaArrayType {
    override val componentType: JavaType
        get() = original.componentType
}

class WrappedJavaPrimitiveType(
    original: JavaPrimitiveType,
    ownAnnotations: Collection<JavaAnnotation>?
) : WrappedJavaType<JavaPrimitiveType>(original, ownAnnotations), JavaPrimitiveType {
    override val type: PrimitiveType?
        get() = original.type
}

class WrappedJavaWildcardType(
    original: JavaWildcardType,
    ownAnnotations: Collection<JavaAnnotation>?
) : WrappedJavaType<JavaWildcardType>(original, ownAnnotations), JavaWildcardType {
    override val bound: JavaType?
        get() = original.bound
    override val isExtends: Boolean
        get() = original.isExtends
}

class WrappedJavaClassifierType(
    original: JavaClassifierType,
    ownAnnotations: Collection<JavaAnnotation>?,
) : WrappedJavaType<JavaClassifierType>(original, ownAnnotations), JavaClassifierType {
    override val classifier: JavaClassifier?
        get() = original.classifier
    override val typeArguments: List<JavaType?>
        get() = original.typeArguments
    override val isRaw: Boolean
        get() = original.isRaw
    override val classifierQualifiedName: String
        get() = original.classifierQualifiedName
    override val presentableText: String
        get() = original.presentableText
}

fun JavaType.withAnnotations(annotations: Collection<JavaAnnotation>): JavaType = when (this) {
    is JavaArrayType -> WrappedJavaArrayType(this, annotations)
    is JavaClassifierType -> WrappedJavaClassifierType(this, annotations)
    is JavaPrimitiveType -> WrappedJavaPrimitiveType(this, annotations)
    is JavaWildcardType -> WrappedJavaWildcardType(this, annotations)
    else -> this
}

abstract class NullabilityJavaAnnotation(override val classId: ClassId) : JavaAnnotation {
    override val arguments: Collection<JavaAnnotationArgument>
        get() = emptyList()

    override fun resolve(): JavaClass? = null

    object NotNull : NullabilityJavaAnnotation(ClassId(ORG_JETBRAINS_ANNOTATIONS, Name.identifier("NotNull")))
    object Nullable : NullabilityJavaAnnotation(ClassId(ORG_JETBRAINS_ANNOTATIONS, Name.identifier("Nullable")))

    companion object {
        private val ORG_JETBRAINS_ANNOTATIONS = FqName.fromSegments(listOf("org", "jetbrains", "annotations"))
    }
}

class DummyJavaClassType(
    override val classifier: JavaClass,
    override val typeArguments: List<JavaType?>
) : JavaClassifierType {
    companion object {
        val ObjectType = DummyJavaClassType(JavaClasses.Object, typeArguments = emptyList())
    }

    override val annotations: Collection<JavaAnnotation>
        get() = emptyList()
    override val isDeprecatedInJavaDoc: Boolean
        get() = false
    override val isRaw: Boolean
        get() = false
    override val classifierQualifiedName: String
        get() = classifier.fqName?.asString() ?: SpecialNames.NO_NAME_PROVIDED.asString()
    override val presentableText: String
        get() = classifierQualifiedName
}

fun JavaType.toRef(source: KtSourceElement?): FirJavaTypeRef = buildJavaTypeRef {
    type = this@toRef
    annotationBuilder = { emptyList() }
    this.source = source
}
