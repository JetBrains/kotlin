/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.java

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.fir.types.jvm.buildJavaTypeRef
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

sealed class NullabilityJavaAnnotation(override val classId: ClassId) : JavaAnnotation {
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
    override val typeArguments: List<JavaType?>,
    override val annotations: List<JavaAnnotation>,
) : JavaClassifierType {
    companion object {
        val ObjectType = DummyJavaClassType(JavaClasses.Object, typeArguments = emptyList(), annotations = emptyList())
    }

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
