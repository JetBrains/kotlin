/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.native.interop.gen.jvm.KotlinPlatform
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

sealed class StubType {
    abstract val nullable: Boolean
    abstract val typeArguments: List<TypeArgument>
}

/**
 * Wrapper over [Classifier].
 */
class ClassifierStubType(
        val classifier: Classifier,
        override val typeArguments: List<TypeArgument> = emptyList(),
        override val nullable: Boolean = false
) : StubType() {

    fun nested(name: String): ClassifierStubType =
            ClassifierStubType(classifier.nested(name))

    override fun toString(): String =
            "${classifier.topLevelName}${typeArguments.ifNotEmpty { joinToString(prefix = "<", postfix = ">") } ?: ""}"
}

class AbbreviatedType(
        val underlyingType: StubType,
        val abbreviatedClassifier: Classifier,
        override val typeArguments: List<TypeArgument>,
        override val nullable: Boolean = false
) : StubType() {
    override fun toString(): String =
            "${abbreviatedClassifier.topLevelName}${typeArguments.ifNotEmpty { joinToString(prefix = "<", postfix = ">") } ?: ""}"
}

/**
 * @return type from kotlinx.cinterop package
 */
fun KotlinPlatform.getRuntimeType(name: String, nullable: Boolean = false): StubType {
    val classifier = Classifier.topLevel(cinteropPackage, name)
    PredefinedTypesHandler.tryExpandPlatformDependentTypealias(classifier, this, nullable)?.let { return it }
    return ClassifierStubType(classifier, nullable = nullable)
}

/**
 * Functional type from kotlin package: ([parameterTypes]) -> [returnType]
 */
class FunctionalType(
      val parameterTypes: List<StubType>, // TODO: Use TypeArguments.
      val returnType: StubType,
      override val nullable: Boolean = false
) : StubType() {
    val classifier: Classifier =
            Classifier.topLevel("kotlin", "Function${parameterTypes.size}")

    override val typeArguments: List<TypeArgument> by lazy {
        listOf(*parameterTypes.toTypedArray(), returnType).map { TypeArgumentStub(it) }
    }
}

class TypeParameterType(
        val name: String,
        override val nullable: Boolean,
        val typeParameterDeclaration: TypeParameterStub
) : StubType() {
    override val typeArguments: List<TypeArgument> = emptyList()
}

fun KotlinType.toStubIrType(): StubType = when (this) {
    is KotlinFunctionType -> this.toStubIrType()
    is KotlinClassifierType -> this.toStubIrType()
    else -> error("Unexpected KotlinType: $this")
}

private fun KotlinFunctionType.toStubIrType(): StubType =
        FunctionalType(parameterTypes.map(KotlinType::toStubIrType), returnType.toStubIrType(), nullable)

private fun KotlinClassifierType.toStubIrType(): StubType {
    val typeArguments = arguments.map(KotlinTypeArgument::toStubIrType)
    PredefinedTypesHandler.tryExpandPredefinedTypealias(classifier, nullable, typeArguments)?.let { return it }
    return if (underlyingType == null) {
        ClassifierStubType(classifier, typeArguments, nullable)
    } else {
        AbbreviatedType(underlyingType.toStubIrType(), classifier, typeArguments, nullable)
    }
}

private fun KotlinTypeArgument.toStubIrType(): TypeArgument = when (this) {
    is KotlinType -> TypeArgumentStub(this.toStubIrType())
    StarProjection -> TypeArgument.StarProjection
    else -> error("Unexpected KotlinTypeArgument: $this")
}

/**
 * Types that come from kotlinx.cinterop require special handling because we
 * don't have explicit information about their structure.
 * For example, to be able to produce metadata-based interop library we need to know
 * that ByteVar is a typealias to ByteVarOf<Byte>.
 */
private object PredefinedTypesHandler {
    private const val cInteropPackage = "kotlinx.cinterop"

    private val nativePtrClassifier = Classifier.topLevel(cInteropPackage, "NativePtr")

    private val primitives = setOf(
            KotlinTypes.boolean,
            KotlinTypes.byte, KotlinTypes.short, KotlinTypes.int, KotlinTypes.long,
            KotlinTypes.uByte, KotlinTypes.uShort, KotlinTypes.uInt, KotlinTypes.uLong,
            KotlinTypes.float, KotlinTypes.double,
            KotlinTypes.vector128
    )

    /**
     * kotlinx.cinterop.{primitive}Var -> kotlin.{primitive}
     */
    private val primitiveVarClassifierToPrimitiveType: Map<Classifier, KotlinClassifierType> =
            primitives.associateBy {
                val typeVar = "${it.classifier.topLevelName}Var"
                Classifier.topLevel(cInteropPackage, typeVar)
            }

    /**
     * @param primitiveType primitive type from kotlin package.
     * @return kotlinx.cinterop.[primitiveType]VarOf<[primitiveType]>
     */
    private fun getVarOfTypeFor(primitiveType: KotlinClassifierType, nullable: Boolean): ClassifierStubType {
        val typeVarOf = "${primitiveType.classifier.topLevelName}VarOf"
        val classifier = Classifier.topLevel(cInteropPackage, typeVarOf)
        return ClassifierStubType(classifier, listOf(TypeArgumentStub(primitiveType.toStubIrType())), nullable = nullable)
    }

    private fun expandCOpaquePointerVar(nullable: Boolean): AbbreviatedType {
        val typeArgument = TypeArgumentStub(expandCOpaquePointer(nullable=false))
        val underlyingType = ClassifierStubType(
                KotlinTypes.cPointerVarOf, listOf(typeArgument), nullable = nullable
        )
        return AbbreviatedType(underlyingType, KotlinTypes.cOpaquePointerVar.classifier, emptyList(), nullable)
    }

    private fun expandCOpaquePointer(nullable: Boolean): AbbreviatedType {
        val typeArgument = TypeArgumentStub(ClassifierStubType(KotlinTypes.cPointed), TypeArgument.Variance.OUT)
        val underlyingType = ClassifierStubType(
                KotlinTypes.cPointer, listOf(typeArgument), nullable = nullable
        )
        return AbbreviatedType(underlyingType, KotlinTypes.cOpaquePointer.classifier, emptyList(), nullable)
    }

    private fun expandCPointerVar(typeArguments: List<TypeArgument>, nullable: Boolean): AbbreviatedType {
        require(typeArguments.size == 1) { "CPointerVar has only one type argument." }
        val cPointer = ClassifierStubType(KotlinTypes.cPointer, typeArguments)
        val cPointerVarOfTypeArgument = TypeArgumentStub(cPointer)
        val underlyingType = ClassifierStubType(
                KotlinTypes.cPointerVarOf, listOf(cPointerVarOfTypeArgument), nullable = nullable
        )
        return AbbreviatedType(underlyingType, KotlinTypes.cPointerVar, typeArguments, nullable)
    }

    /**
     * @param primitiveVarType one of kotlinx.cinterop.{primitive}Var types.
     * @return typealias in terms of StubIR types.
     */
    private fun expandPrimitiveVarType(primitiveVarClassifier: Classifier, nullable: Boolean): AbbreviatedType {
        val primitiveType = primitiveVarClassifierToPrimitiveType.getValue(primitiveVarClassifier)
        val underlyingType = getVarOfTypeFor(primitiveType, nullable)
        return AbbreviatedType(underlyingType, primitiveVarClassifier, listOf(), nullable)
    }

    private fun expandNativePtr(platform: KotlinPlatform, nullable: Boolean): StubType {
        val underlyingTypeClassifier = when (platform) {
            KotlinPlatform.JVM -> KotlinTypes.long.classifier
            KotlinPlatform.NATIVE -> Classifier.topLevel("kotlin.native.internal", "NativePtr")
        }
        val underlyingType = ClassifierStubType(underlyingTypeClassifier, nullable = nullable)
        return AbbreviatedType(underlyingType, nativePtrClassifier, listOf(), nullable)
    }

    private fun expandObjCObjectMeta(typeArguments: List<TypeArgument>, nullable: Boolean): AbbreviatedType {
        require(typeArguments.isEmpty())
        val objCClass = ClassifierStubType(KotlinTypes.objCClass, emptyList(), nullable)
        return AbbreviatedType(objCClass, KotlinTypes.objCObjectMeta, emptyList(), nullable)
    }

    private fun expandCArrayPointer(typeArguments: List<TypeArgument>, nullable: Boolean): AbbreviatedType {
        val cPointer = ClassifierStubType(KotlinTypes.cPointer, typeArguments)
        return AbbreviatedType(cPointer, KotlinTypes.cArrayPointer, typeArguments, nullable)
    }

    private fun expandObjCBlockVar(typeArguments: List<TypeArgument>, nullable: Boolean): AbbreviatedType {
        val underlyingType = ClassifierStubType(KotlinTypes.objCNotImplementedVar, typeArguments, nullable)
        return AbbreviatedType(underlyingType, KotlinTypes.objCBlockVar, typeArguments, nullable)
    }

    /**
     * @return [ClassifierStubType] if [classifier] is a typealias from [kotlinx.cinterop] package.
     */
    fun tryExpandPredefinedTypealias(classifier: Classifier, nullable: Boolean, typeArguments: List<TypeArgument>): AbbreviatedType? =
            when (classifier) {
                in primitiveVarClassifierToPrimitiveType.keys -> expandPrimitiveVarType(classifier, nullable)
                KotlinTypes.cOpaquePointer.classifier -> expandCOpaquePointer(nullable)
                KotlinTypes.cOpaquePointerVar.classifier -> expandCOpaquePointerVar(nullable)
                KotlinTypes.cPointerVar -> expandCPointerVar(typeArguments, nullable)
                KotlinTypes.objCObjectMeta -> expandObjCObjectMeta(typeArguments, nullable)
                KotlinTypes.cArrayPointer -> expandCArrayPointer(typeArguments, nullable)
                KotlinTypes.objCBlockVar -> expandObjCBlockVar(typeArguments, nullable)
                else -> null
            }

    /**
     * Variant of [tryExpandPredefinedTypealias] with [platform]-dependent result.
     */
    fun tryExpandPlatformDependentTypealias(
            classifier: Classifier, platform: KotlinPlatform, nullable: Boolean
    ): StubType? =
            when (classifier) {
                nativePtrClassifier -> expandNativePtr(platform, nullable)
                else -> null
            }
}