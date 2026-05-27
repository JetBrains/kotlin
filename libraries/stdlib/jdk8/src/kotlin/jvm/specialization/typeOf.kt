/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.specialization

import org.jetbrains.kotlin.codegen.util.inlinecodegen.ClassInstance
import org.jetbrains.kotlin.codegen.util.inlinecodegen.LightIrType
import org.jetbrains.kotlin.codegen.util.inlinecodegen.iconstInsnNode
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.FieldInsnNode
import org.jetbrains.org.objectweb.asm.tree.InsnNode
import org.jetbrains.org.objectweb.asm.tree.LdcInsnNode
import org.jetbrains.org.objectweb.asm.tree.TypeInsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode
import kotlin.reflect.KVariance

private const val REFLECTION = "kotlin/jvm/internal/Reflection"
private const val K_TYPE = "kotlin/reflect/KType"
private const val K_CLASS = "kotlin/reflect/KClass"
private const val K_TYPE_PARAMETER = "kotlin/reflect/KTypeParameter"
private const val K_CLASSIFIER = "kotlin/reflect/KClassifier"
private const val K_VARIANCE = "kotlin/reflect/KVariance"
private const val K_TYPE_PROJECTION = "kotlin/reflect/KTypeProjection"

internal fun generateTypeOf(
    typeParameterValue: LightIrType,
    isTypeParameterBound: Boolean,
    emit: (AbstractInsnNode) -> Unit,
) {
    val methodArguments = when (val classifier = typeParameterValue.classifier) {
        is LightIrType.Classifier.Clazz -> {
            emit.classInstance(classifier.typeOfSupportClassInstance)
            val arguments = unrollArrayIfFewerThan(typeParameterValue.arguments.size, 3, Type.getObjectType(K_TYPE_PROJECTION), emit) { i ->
                generateTypeOfArgument(typeParameterValue.arguments[i], isTypeParameterBound, emit)
            }
            arrayOf(Type.getObjectType("java/lang/Class"), *arguments)
        }
        is LightIrType.Classifier.TypeParameter -> {
            if (!isTypeParameterBound && classifier.isReified) {
                error("unreachable for specialized functions")
            } else {
                generateNonReifiedTypeParameter(classifier, emit)
                arrayOf(Type.getObjectType(K_CLASSIFIER))
            }
        }
    }

    val methodName = if (typeParameterValue.nullable) "nullableTypeOf" else "typeOf"
    val signature = Type.getMethodDescriptor(Type.getObjectType(K_TYPE), *methodArguments)
    emit.methodInsnNode(Opcodes.INVOKESTATIC, REFLECTION, methodName, signature)

    // TODO
    // if (intrinsicsSupport.toKotlinType(type).isSuspendFunctionType) {
    //     intrinsicsSupport.reportSuspendTypeUnsupported()
    // }

    // TODO
    // if (intrinsicsSupport.config.stableTypeOf) {
    //     if (intrinsicsSupport.isMutableCollectionType(type)) {
    //         v.invokestatic(REFLECTION, "mutableCollectionType", Type.getMethodDescriptor(K_TYPE, K_TYPE), false)
    //     } else if (type.typeConstructor().isNothingConstructor()) {
    //         v.invokestatic(REFLECTION, "nothingType", Type.getMethodDescriptor(K_TYPE, K_TYPE), false)
    //     }

    //     if (type.isFlexible()) {
    //         // If this is a flexible type, we've just generated its lower bound and have it on the stack.
    //         // Let's generate the upper bound now and call the method that takes lower and upper bound and constructs a flexible KType.
    //         @Suppress("UNCHECKED_CAST")
    //         generateTypeOf(v, type.upperBoundIfFlexible() as KT, intrinsicsSupport, isTypeParameterBound)

    //         v.invokestatic(REFLECTION, "platformType", Type.getMethodDescriptor(K_TYPE, K_TYPE, K_TYPE), false)
    //     }
    // }
}

private fun generateTypeOfArgument(
    projection: LightIrType.TypeArgument,
    isTypeParameterBound: Boolean,
    emit: (AbstractInsnNode) -> Unit,
) {
    when (projection) {
        is LightIrType.TypeArgument.StarProjection -> {
            emit(FieldInsnNode(Opcodes.GETSTATIC, K_TYPE_PROJECTION, "star", "L$K_TYPE_PROJECTION;"))
        }
        is LightIrType.TypeArgument.TypeProjection -> {
            generateTypeOf(projection.type, isTypeParameterBound, emit)
            val methodName = when (projection.variance) {
                LightIrType.TypeArgument.VARIANCE_INV -> "invariant"
                LightIrType.TypeArgument.VARIANCE_IN -> "contravariant"
                LightIrType.TypeArgument.VARIANCE_OUT -> "covariant"
                else -> error("unreachable")
            }
            emit.methodInsnNode(
                Opcodes.INVOKESTATIC,
                K_TYPE_PROJECTION,
                methodName,
                "(L$K_TYPE;)L$K_TYPE_PROJECTION;",
            )
        }
    }
}

private fun generateNonReifiedTypeParameter(
    typeParameter: LightIrType.Classifier.TypeParameter,
    emit: (AbstractInsnNode) -> Unit,
) {
    when (val parent = typeParameter.parent) {
        is LightIrType.Classifier.TypeParameter.Parent.ParentClass -> {
            emit(LdcInsnNode(Type.getObjectType(parent.internalName)))
            emit.methodInsnNode(
                Opcodes.INVOKESTATIC,
                REFLECTION,
                "getOrCreateKotlinClass",
                "(Ljava/lang/Class;)L$K_CLASS;",
            )
        }
        is LightIrType.Classifier.TypeParameter.Parent.Function -> {
            generateCallableReference(
                "kotlin/jvm/internal/FunctionReferenceImpl",
                parent.arity,
                parent.owner,
                parent.declarationName,
                parent.signatureString,
                parent.topLevelFlag,
                emit,
            )
        }
        is LightIrType.Classifier.TypeParameter.Parent.Property -> {
            generateCallableReference(
                parent.implClassInternalName,
                null,
                parent.owner,
                parent.declarationName,
                parent.signatureString,
                parent.topLevelFlag,
                emit,
            )
        }
    }

    emit(LdcInsnNode(typeParameter.name))

    val variance = when (typeParameter.variance) {
        LightIrType.TypeArgument.VARIANCE_INV -> KVariance.INVARIANT.name
        LightIrType.TypeArgument.VARIANCE_IN -> KVariance.IN.name
        LightIrType.TypeArgument.VARIANCE_OUT -> KVariance.OUT.name
        else -> error("unreachable")
    }
    emit(FieldInsnNode(Opcodes.GETSTATIC, K_VARIANCE, variance, "L$K_VARIANCE;"))

    emit(InsnNode(if (typeParameter.isReified) Opcodes.ICONST_1 else Opcodes.ICONST_0))

    emit.methodInsnNode(
        Opcodes.INVOKESTATIC,
        REFLECTION,
        "typeParameter",
        "(Ljava/lang/Object;Ljava/lang/String;L$K_VARIANCE;Z)L$K_TYPE_PARAMETER;",
    )

    val upperBounds = typeParameter.upperBounds ?: return
    if (upperBounds.isEmpty()) return

    emit(InsnNode(Opcodes.DUP))

    val argumentsForBounds = unrollArrayIfFewerThan(upperBounds.size, 2, Type.getObjectType(K_TYPE), emit) { i ->
        generateTypeOf(upperBounds[i], true, emit)
    }

    emit.methodInsnNode(
        Opcodes.INVOKESTATIC,
        REFLECTION,
        "setUpperBounds",
        Type.getMethodDescriptor(Type.VOID_TYPE, Type.getObjectType(K_TYPE_PARAMETER), *argumentsForBounds),
    )
}

private fun generateCallableReference(
    implClassInternalName: String,
    arity: Int?,
    owner: ClassInstance,
    declarationName: String,
    signatureString: String,
    topLevelFlag: Int,
    emit: (AbstractInsnNode) -> Unit
) {
    emit(TypeInsnNode(Opcodes.NEW, implClassInternalName))
    emit(InsnNode(Opcodes.DUP))
    arity?.let { emit(iconstInsnNode(it)) }
    emit.classInstance(owner)
    emit(LdcInsnNode(declarationName))
    emit(LdcInsnNode(signatureString))
    emit(iconstInsnNode(topLevelFlag))
    val descriptor = if (arity != null) {
        "(ILjava/lang/Class;Ljava/lang/String;Ljava/lang/String;I)V"
    } else {
        "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;I)V"
    }
    emit.methodInsnNode(
        Opcodes.INVOKESPECIAL,
        implClassInternalName,
        "<init>",
        descriptor,
    )
}

private fun ((AbstractInsnNode) -> Unit).methodInsnNode(
    opcode: Int,
    owner: String,
    name: String,
    descriptor: String,
    isInterface: Boolean = false,
) = this(
    MethodInsnNode(
        opcode,
        owner,
        name,
        descriptor,
        isInterface,
    )
)

private fun ((AbstractInsnNode) -> Unit).classInstance(classInstance: ClassInstance) = this(
    when (classInstance) {
        is ClassInstance.ConstClass -> LdcInsnNode(Type.getType(classInstance.descriptor))
        is ClassInstance.StaticOf -> FieldInsnNode(Opcodes.GETSTATIC, classInstance.internalName, "TYPE", "Ljava/lang/Class;")
    }
)

private inline fun unrollArrayIfFewerThan(
    n: Int,
    limit: Int,
    type: Type,
    emit: (AbstractInsnNode) -> Unit,
    element: (Int) -> Unit
): Array<Type> {
    require(type.sort == Type.OBJECT)
    if (n < limit) {
        return Array(n) { i ->
            element(i)
            type
        }
    }
    emit(iconstInsnNode(n))
    emit(TypeInsnNode(Opcodes.ANEWARRAY, type.internalName))
    for (i in 0 until n) {
        emit(InsnNode(Opcodes.DUP))
        emit(iconstInsnNode(i))
        element(i)
        emit(InsnNode(Opcodes.AASTORE))
    }
    return arrayOf(Type.getObjectType("[$type"))
}
