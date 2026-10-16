/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.signatures

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.base.signatures.KotlinSignatureUtils.drisOfAllNestedBounds
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.AnnotationTarget
import org.jetbrains.dokka.model.properties.WithExtraProperties

internal interface JvmSignatureUtils {
    fun <T : Documentable> WithExtraProperties<T>.modifiers(): SourceSetDependent<Set<ExtraModifiers>>

    fun Annotations.Annotation.isIgnored(): Boolean

    fun Collection<ExtraModifiers>.toSignatureString(): String =
        joinToString("") { it.name.lowercase() + " " }

    @Suppress("UNCHECKED_CAST")
    fun Documentable.annotations(): Map<DokkaSourceSet, List<Annotations.Annotation>> {
        return (this as? WithExtraProperties<Documentable>)?.annotations() ?: emptyMap()
    }

    fun <T : AnnotationTarget> WithExtraProperties<T>.annotations(): SourceSetDependent<List<Annotations.Annotation>> =
        extra[Annotations]?.directAnnotations ?: emptyMap()

    @Suppress("UNCHECKED_CAST")
    operator fun <T : Iterable<*>> SourceSetDependent<T>.plus(other: SourceSetDependent<T>): SourceSetDependent<T> {
        return LinkedHashMap(this).apply {
            for ((k, v) in other) {
                put(k, get(k).let { if (it != null) (it + v) as T else v })
            }
        }
    }

    fun DProperty.annotations(): SourceSetDependent<List<Annotations.Annotation>> {
        return (extra[Annotations]?.directAnnotations ?: emptyMap()) +
                (getter?.annotations() ?: emptyMap()).mapValues { it.value.map { it.copy( scope = Annotations.AnnotationScope.GETTER) } } +
                (setter?.annotations() ?: emptyMap()).mapValues { it.value.map { it.copy( scope = Annotations.AnnotationScope.SETTER) } }
    }



    infix fun DFunction.uses(typeParameter: DTypeParameter): Boolean {
        val parameterDris = parameters.flatMap { listOf(it.dri) + it.type.drisOfAllNestedBounds }
        val receiverDris =
            listOfNotNull(
                receiver?.dri,
                *receiver?.type?.drisOfAllNestedBounds?.toTypedArray() ?: emptyArray()
            )
        val allDris = parameterDris + receiverDris
        return typeParameter.dri in allDris
    }

}
