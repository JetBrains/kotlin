/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

import kotlin.internal.UsedFromCompilerGeneratedCode
import kotlin.internal.staticInitializationFailure
import kotlin.reflect.KClass

@UsedFromCompilerGeneratedCode
internal fun THROW_CCE(): Nothing {
    throw ClassCastException()
}

@UsedFromCompilerGeneratedCode
internal fun THROW_CCE_WITH_INFO(obj: Any?, klass: KClass<*>, isNullable: Boolean): Nothing {
    @Suppress("UNSUPPORTED_REFLECTION_API")
    val targetType = klass.qualifiedName ?: klass.simpleName ?: "<unknown>"

    if (!isNullable && obj == null) {
        throw ClassCastException("Cannot cast null to $targetType: target type is non-nullable")
    }

    val targetTypeWithNullability = if (isNullable) "$targetType?" else targetType

    val message = if (obj != null) {
        @Suppress("UNSUPPORTED_REFLECTION_API")
        val valueType = (obj::class).let { it.qualifiedName ?: it.simpleName } ?: "<unknown>"
        if (klass == Nothing::class && isNullable) {
            "Expected null (Nothing?), got an instance of $valueType"
        } else {
            "Cannot cast instance of $valueType to $targetTypeWithNullability: incompatible types"
        }
    } else {
        "Cannot cast null to $targetTypeWithNullability"
    }

    throw ClassCastException(message)
}

@UsedFromCompilerGeneratedCode
internal fun THROW_NPE(): Nothing {
    throw NullPointerException()
}

@UsedFromCompilerGeneratedCode
internal fun THROW_ISE(): Nothing {
    throw IllegalStateException()
}

@UsedFromCompilerGeneratedCode
internal fun THROW_IAE(message: String): Nothing {
    throw IllegalArgumentException(message)
}

@UsedFromCompilerGeneratedCode
internal fun throwNoBranchMatchedException(): Nothing {
    throw NoWhenBranchMatchedException()
}

@UsedFromCompilerGeneratedCode
internal fun throwKotlinNothingValueException(): Nothing {
    throw KotlinNothingValueException()
}

@UsedFromCompilerGeneratedCode
internal fun rangeCheck(index: Int, size: Int) {
  if (index < 0 || index >= size) throw IndexOutOfBoundsException()
}

private const val INITIALIZATION_STATE_INITIALIZED: Int = 1
private const val INITIALIZATION_STATE_ERROR: Int = 2

@UsedFromCompilerGeneratedCode
internal fun checkStaticInitializationState(state: Int, klass: KClass<*>?): Boolean {
    if (state == INITIALIZATION_STATE_ERROR) {
        staticInitializationFailure(null, klass?.qualifiedName ?: klass?.simpleName)
    }
    return state == INITIALIZATION_STATE_INITIALIZED
}
