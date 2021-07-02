/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("TypeUtils")

package org.jetbrains.kotlin.idea.util

import com.intellij.psi.*
import org.jetbrains.kotlin.builtins.getReturnTypeFromFunctionType
import org.jetbrains.kotlin.builtins.isBuiltinFunctionalType
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMapper
import org.jetbrains.kotlin.builtins.replaceReturnType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.MutablePackageFragmentDescriptor
import org.jetbrains.kotlin.idea.FrontendInternals
import org.jetbrains.kotlin.idea.imports.canBeReferencedViaImport
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.components.TypeUsage
import org.jetbrains.kotlin.load.java.lazy.JavaResolverComponents
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.load.java.lazy.TypeParameterResolver
import org.jetbrains.kotlin.load.java.lazy.child
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaTypeParameterDescriptor
import org.jetbrains.kotlin.load.java.lazy.types.JavaTypeAttributes
import org.jetbrains.kotlin.load.java.lazy.types.JavaTypeResolver
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.load.java.structure.impl.JavaTypeImpl
import org.jetbrains.kotlin.load.java.structure.impl.JavaTypeParameterImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.utils.findClassifier
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.*
import org.jetbrains.kotlin.utils.SmartSet

fun KotlinType.approximateFlexibleTypes(
    preferNotNull: Boolean = false,
    preferStarForRaw: Boolean = false
): KotlinType {
    if (isDynamic()) return this
    return unwrapEnhancement().approximateNonDynamicFlexibleTypes(preferNotNull, preferStarForRaw)
}

private fun KotlinType.approximateNonDynamicFlexibleTypes(
    preferNotNull: Boolean = false,
    preferStarForRaw: Boolean = false
): SimpleType {
    if (this is ErrorType) return this

    if (isFlexible()) {
        val flexible = asFlexibleType()
        val lowerClass = flexible.lowerBound.constructor.declarationDescriptor as? ClassDescriptor?
        val isCollection = lowerClass != null && JavaToKotlinClassMapper.isMutable(lowerClass)
        // (Mutable)Collection<T>! -> MutableCollection<T>?
        // Foo<(Mutable)Collection<T>!>! -> Foo<Collection<T>>?
        // Foo! -> Foo?
        // Foo<Bar!>! -> Foo<Bar>?
        var approximation =
            if (isCollection)
                flexible.lowerBound.makeNullableAsSpecified(!preferNotNull)
            else
                if (this is RawType && preferStarForRaw) flexible.upperBound.makeNullableAsSpecified(!preferNotNull)
                else
                    if (preferNotNull) flexible.lowerBound else flexible.upperBound

        approximation = approximation.approximateNonDynamicFlexibleTypes()

        approximation = if (nullability() == TypeNullability.NOT_NULL) approximation.makeNullableAsSpecified(false) else approximation

        if (approximation.isMarkedNullable && !flexible.lowerBound
                .isMarkedNullable && TypeUtils.isTypeParameter(approximation) && TypeUtils.hasNullableSuperType(approximation)
        ) {
            approximation = approximation.makeNullableAsSpecified(false)
        }

        return approximation
    }

    (unwrap() as? AbbreviatedType)?.let {
        return AbbreviatedType(it.expandedType, it.abbreviation.approximateNonDynamicFlexibleTypes(preferNotNull))
    }
    return KotlinTypeFactory.simpleTypeWithNonTrivialMemberScope(
        annotations,
        constructor,
        arguments.map { it.substitute { type -> type.approximateFlexibleTypes(preferNotNull = true) } },
        isMarkedNullable,
        ErrorUtils.createErrorScope("This type is not supposed to be used in member resolution", true)
    )
}

fun KotlinType.isResolvableInScope(scope: LexicalScope?, checkTypeParameters: Boolean, allowIntersections: Boolean = false): Boolean {
    if (constructor is IntersectionTypeConstructor) {
        if (!allowIntersections) return false
        return constructor.supertypes.all { it.isResolvableInScope(scope, checkTypeParameters, allowIntersections) }
    }

    if (canBeReferencedViaImport()) return true

    val descriptor = constructor.declarationDescriptor
    if (descriptor == null || descriptor.name.isSpecial) return false
    if (!checkTypeParameters && descriptor is TypeParameterDescriptor) return true

    return scope != null && scope.findClassifier(descriptor.name, NoLookupLocation.FROM_IDE) == descriptor
}

fun KotlinType.approximateWithResolvableType(scope: LexicalScope?, checkTypeParameters: Boolean): KotlinType {
    if (isError || isResolvableInScope(scope, checkTypeParameters)) return this
    return supertypes().firstOrNull { it.isResolvableInScope(scope, checkTypeParameters) }
        ?: builtIns.anyType
}

fun KotlinType.anonymousObjectSuperTypeOrNull(): KotlinType? {
    val classDescriptor = constructor.declarationDescriptor
    if (classDescriptor != null && DescriptorUtils.isAnonymousObject(classDescriptor)) {
        return immediateSupertypes().firstOrNull() ?: classDescriptor.builtIns.anyType
    }
    return null
}

fun KotlinType.getResolvableApproximations(
    scope: LexicalScope?,
    checkTypeParameters: Boolean,
    allowIntersections: Boolean = false
): Sequence<KotlinType> {
    return (listOf(this) + TypeUtils.getAllSupertypes(this))
        .asSequence()
        .mapNotNull {
            it.asTypeProjection()
                .fixTypeProjection(scope, checkTypeParameters, allowIntersections, isOutVariance = true)
                ?.type
        }
}

private fun TypeProjection.fixTypeProjection(
    scope: LexicalScope?,
    checkTypeParameters: Boolean,
    allowIntersections: Boolean,
    isOutVariance: Boolean
): TypeProjection? {
    if (!type.isResolvableInScope(scope, checkTypeParameters, allowIntersections)) return null
    if (type.arguments.isEmpty()) return this

    val resolvableArgs = type.arguments.filterTo(SmartSet.create()) { typeProjection ->
        typeProjection.type.isResolvableInScope(scope, checkTypeParameters, allowIntersections)
    }

    if (resolvableArgs.containsAll(type.arguments)) {
        fun fixArguments(type: KotlinType): KotlinType? = type.replace(
            (type.arguments zip type.constructor.parameters).map { (arg, param) ->
                if (arg.isStarProjection) arg
                else arg.fixTypeProjection(
                    scope,
                    checkTypeParameters,
                    allowIntersections,
                    isOutVariance = isOutVariance && param.variance == Variance.OUT_VARIANCE
                ) ?: when {
                    !isOutVariance -> return null
                    param.variance == Variance.OUT_VARIANCE -> arg.type.approximateWithResolvableType(
                        scope,
                        checkTypeParameters
                    ).asTypeProjection()
                    else -> type.replaceArgumentsWithStarProjections().arguments.first()
                }
            })

        return if (type.isBuiltinFunctionalType) {
            val returnType = type.getReturnTypeFromFunctionType()
            type.replaceReturnType(fixArguments(returnType) ?: return null).asTypeProjection()
        } else fixArguments(type)?.asTypeProjection()
    }

    if (!isOutVariance) return null

    val newArguments = (type.arguments zip type.constructor.parameters).map { (arg, param) ->
        when {
            arg in resolvableArgs -> arg

            arg.projectionKind == Variance.OUT_VARIANCE ||
                    param.variance == Variance.OUT_VARIANCE -> TypeProjectionImpl(
                arg.projectionKind,
                arg.type.approximateWithResolvableType(scope, checkTypeParameters)
            )

            else -> return if (isOutVariance) type.replaceArgumentsWithStarProjections().asTypeProjection() else null
        }
    }

    return type.replace(newArguments).asTypeProjection()
}

fun KotlinType.isAbstract(): Boolean {
    val modality = (constructor.declarationDescriptor as? ClassDescriptor)?.modality
    return modality == Modality.ABSTRACT || modality == Modality.SEALED
}

/**
 * NOTE: this is a very shaky implementation of [PsiType] to [KotlinType] conversion,
 * produced types are fakes and are usable only for code generation. Please be careful using this method.
 */
@OptIn(FrontendInternals::class)
fun PsiType.resolveToKotlinType(resolutionFacade: ResolutionFacade): KotlinType {
    if (this == PsiType.NULL) {
        return resolutionFacade.moduleDescriptor.builtIns.nullableAnyType
    }

    val typeParameters = collectTypeParameters()
    val components = resolutionFacade.getFrontendService(JavaResolverComponents::class.java)
    val rootContext = LazyJavaResolverContext(components, TypeParameterResolver.EMPTY) { null }
    val dummyPackageDescriptor = MutablePackageFragmentDescriptor(resolutionFacade.moduleDescriptor, FqName("dummy"))
    val dummyClassDescriptor = ClassDescriptorImpl(
        dummyPackageDescriptor,
        Name.identifier("Dummy"),
        Modality.FINAL,
        ClassKind.CLASS,
        emptyList(),
        SourceElement.NO_SOURCE,
        false,
        LockBasedStorageManager.NO_LOCKS
    )
    val typeParameterResolver = object : TypeParameterResolver {
        override fun resolveTypeParameter(javaTypeParameter: JavaTypeParameter): TypeParameterDescriptor? {
            val psiTypeParameter = (javaTypeParameter as JavaTypeParameterImpl).psi
            val index = typeParameters.indexOf(psiTypeParameter)
            if (index < 0) return null
            return LazyJavaTypeParameterDescriptor(rootContext.child(this), javaTypeParameter, index, dummyClassDescriptor)
        }
    }
    val typeResolver = JavaTypeResolver(rootContext, typeParameterResolver)
    val attributes = JavaTypeAttributes(TypeUsage.COMMON)
    return typeResolver.transformJavaType(JavaTypeImpl.create(this), attributes).approximateFlexibleTypes(preferNotNull = true)
}


private fun PsiType.collectTypeParameters(): List<PsiTypeParameter> {
    val results = ArrayList<PsiTypeParameter>()
    accept(
        object : PsiTypeVisitor<Unit>() {
            override fun visitArrayType(arrayType: PsiArrayType) {
                arrayType.componentType.accept(this)
            }

            override fun visitClassType(classType: PsiClassType) {
                (classType.resolve() as? PsiTypeParameter)?.let { results += it }
                classType.parameters.forEach { it.accept(this) }
            }

            override fun visitWildcardType(wildcardType: PsiWildcardType) {
                wildcardType.bound?.accept(this)
            }
        }
    )
    return results
}
