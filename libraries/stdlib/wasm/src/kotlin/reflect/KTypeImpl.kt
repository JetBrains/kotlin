/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

import kotlin.reflect.*

internal class KTypeImpl(
    override val classifier: KClassifier?,
    override val arguments: List<KTypeProjection>,
    override val isMarkedNullable: Boolean
) : KType {
    override fun equals(other: Any?): Boolean =
        other is KTypeImpl &&
                classifier == other.classifier && arguments == other.arguments && isMarkedNullable == other.isMarkedNullable

    override fun hashCode(): Int =
        (classifier.hashCode() * 31 + arguments.hashCode()) * 31 + isMarkedNullable.hashCode()

    override fun toString(): String {
        val kClass = (classifier as? KClass<*>)
        val classifierName = when {
            kClass == null -> classifier.toString()
            kClass.simpleName != null -> kClass.simpleName
            else -> "(non-denotable type)"
        }

        val args =
            if (arguments.isEmpty()) ""
            else arguments.joinToString(", ", "<", ">")
        val nullable = if (isMarkedNullable) "?" else ""

        return classifierName + args + nullable
    }
}