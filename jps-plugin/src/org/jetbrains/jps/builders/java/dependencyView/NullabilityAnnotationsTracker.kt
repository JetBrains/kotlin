/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jps.builders.java.dependencyView

import gnu.trove.TIntHashSet
import org.jetbrains.jps.builders.java.dependencyView.TypeRepr.ClassType
import org.jetbrains.kotlin.fileClasses.internalNameWithoutInnerClasses
import org.jetbrains.kotlin.load.java.JAVAX_NONNULL_ANNOTATION
import org.jetbrains.kotlin.load.java.NOT_NULL_ANNOTATIONS
import org.jetbrains.kotlin.load.java.NULLABLE_ANNOTATIONS
import java.util.*

internal class NullabilityAnnotationsTracker : AnnotationsChangeTracker() {
    private val annotations = (NULLABLE_ANNOTATIONS + JAVAX_NONNULL_ANNOTATION + NOT_NULL_ANNOTATIONS).mapTo(HashSet()) { it.internalNameWithoutInnerClasses }.toTypedArray()

    override fun methodAnnotationsChanged(
            context: DependencyContext,
            method: MethodRepr,
            annotationsDiff: Difference.Specifier<ClassType, Difference>,
            paramAnnotationsDiff: Difference.Specifier<ParamAnnotation, Difference>
    ): Set<Recompile> {
        val changedAnnotations = annotationsDiff.addedOrRemoved() +
                                 paramAnnotationsDiff.addedOrRemoved().map { it.type }

        return handleNullAnnotationsChanges(context, method, changedAnnotations)
    }


    override fun fieldAnnotationsChanged(
            context: NamingContext,
            field: FieldRepr,
            annotationsDiff: Difference.Specifier<ClassType, Difference>
    ): Set<Recompile> {
        return handleNullAnnotationsChanges(context, field, annotationsDiff.addedOrRemoved())
    }

    private fun handleNullAnnotationsChanges(context: NamingContext, protoMember: ProtoMember, annotations: Sequence<TypeRepr.ClassType>): Set<Recompile> {
        val nullabilityAnnotations = TIntHashSet(this.annotations.toIntArray { context.get(it) })
        val changedNullAnnotation = annotations.firstOrNull { nullabilityAnnotations.contains(it.className) }

        val result = EnumSet.noneOf(Recompile::class.java)
        if (changedNullAnnotation != null) {
            result.add(Recompile.USAGES)

            if (protoMember is MethodRepr && !protoMember.isFinal) {
                // methods can be overridden, whereas fields cannot be
                result.add(Recompile.SUBCLASSES)
            }
        }

        return result
    }

    private fun <T> Difference.Specifier<T, Difference>.addedOrRemoved(): Sequence<T> =
            added().asSequence() + removed().asSequence()

    private inline fun <T> Array<T>.toIntArray(fn: (T)->Int): IntArray =
            IntArray(size) { i -> fn(get(i)) }
}