/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect

internal class KTypeImpl(
    override val classifier: KClassifier?,
    override val arguments: List<KTypeProjection>,
    override val isMarkedNullable: Boolean,
) : KType {
    override fun equals(other: Any?): Boolean =
        other is KTypeImpl &&
                classifier == other.classifier && arguments == other.arguments && isMarkedNullable == other.isMarkedNullable

    override fun hashCode(): Int =
        (classifier.hashCode() * 31 + arguments.hashCode()) * 31 + isMarkedNullable.hashCode()

    override fun toString(): String {
        val classifierString = when (classifier) {
            is KClass<*> -> classifier.qualifiedName ?: classifier.simpleName
            is KTypeParameter -> classifier.name
            else -> null
        } ?: return "(non-denotable type)"

        return buildString {
            append(classifierString)

            if (arguments.isNotEmpty()) {
                append('<')
                for ((index, argument) in arguments.withIndex()) {
                    if (index > 0) append(", ")
                    append(argument)
                }
                append('>')
            }

            if (isMarkedNullable) append('?')
        }
    }
}
