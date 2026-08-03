/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.java

import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.lombok.LombokNames.JAVA_COLLECTION_ID
import org.jetbrains.kotlin.lombok.LombokNames.JAVA_ITERABLE_ID
import org.jetbrains.kotlin.lombok.LombokNames.JAVA_MAP_ID
import org.jetbrains.kotlin.lombok.LombokNames.JAVA_OBJECT_ID
import org.jetbrains.kotlin.lombok.LombokNames.TABLE_ID
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

object JavaClasses {
    val Object = DummyJavaClass(JAVA_OBJECT_ID, numberOfTypeParameters = 0)
    val Iterable = DummyJavaClass(JAVA_ITERABLE_ID, numberOfTypeParameters = 1)
    val Collection = DummyJavaClass(JAVA_COLLECTION_ID, numberOfTypeParameters = 1)
    val Map = DummyJavaClass(JAVA_MAP_ID, numberOfTypeParameters = 2)
    val Table = DummyJavaClass(TABLE_ID, numberOfTypeParameters = 3)
}

class DummyJavaClass(val classId: ClassId, numberOfTypeParameters: Int) : JavaClass {
    override val fqName: FqName = classId.asSingleFqName()
    override val name: Name = fqName.shortName()

    override val isFromSource: Boolean
        get() = shouldNotBeCalled()
    override val annotations: Collection<JavaAnnotation>
        get() = shouldNotBeCalled()
    override val isDeprecatedInJavaDoc: Boolean
        get() = shouldNotBeCalled()

    override fun findAnnotation(fqName: FqName): JavaAnnotation? {
        return null
    }

    override val isAbstract: Boolean
        get() = shouldNotBeCalled()
    override val isStatic: Boolean
        get() = shouldNotBeCalled()
    override val isFinal: Boolean
        get() = shouldNotBeCalled()
    override val visibility: Visibility
        get() = shouldNotBeCalled()
    override val typeParameters: List<JavaTypeParameter> = (1..numberOfTypeParameters).map {
        DummyJavaTypeParameter(Name.identifier("T_$it"))
    }

    override val supertypes: Collection<JavaClassifierType>
        get() = shouldNotBeCalled()
    override val innerClassNames: Collection<Name>
        get() = shouldNotBeCalled()

    override fun findInnerClass(name: Name): JavaClass? {
        shouldNotBeCalled()
    }

    override val outerClass: JavaClass?
        get() = null
    override val isInterface: Boolean
        get() = shouldNotBeCalled()
    override val isAnnotationType: Boolean
        get() = shouldNotBeCalled()
    override val isEnum: Boolean
        get() = shouldNotBeCalled()
    override val isRecord: Boolean
        get() = shouldNotBeCalled()
    override val isValue: Boolean
        get() = shouldNotBeCalled()
    override val isSealed: Boolean
        get() = shouldNotBeCalled()
    override val permittedTypes: Sequence<JavaClassifierType>
        get() = shouldNotBeCalled()
    override val lightClassOriginKind: LightClassOriginKind
        get() = shouldNotBeCalled()
    override val methods: Collection<JavaMethod>
        get() = shouldNotBeCalled()
    override val fields: Collection<JavaField>
        get() = shouldNotBeCalled()
    override val constructors: Collection<JavaConstructor>
        get() = shouldNotBeCalled()
    override val recordComponents: Collection<JavaRecordComponent>
        get() = shouldNotBeCalled()

    override fun hasDefaultConstructor(): Boolean {
        shouldNotBeCalled()
    }
}

class DummyJavaTypeParameter(override val name: Name) : JavaTypeParameter {
    override val isFromSource: Boolean
        get() = shouldNotBeCalled()
    override val annotations: Collection<JavaAnnotation>
        get() = shouldNotBeCalled()
    override val isDeprecatedInJavaDoc: Boolean
        get() = shouldNotBeCalled()

    override fun findAnnotation(fqName: FqName): JavaAnnotation? {
        shouldNotBeCalled()
    }

    override val upperBounds: Collection<JavaClassifierType>
        get() = emptyList()
}
