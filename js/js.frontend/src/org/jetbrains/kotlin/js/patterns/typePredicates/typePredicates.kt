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

package org.jetbrains.kotlin.js.patterns.typePredicates

import com.google.common.base.Predicate
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.types.KotlinType

public interface TypePredicate : Predicate<KotlinType> {
    override fun apply(type: KotlinType?): Boolean
}

private val KOTLIN = TypePredicateImpl("kotlin")
val COMPARABLE: TypePredicate = KOTLIN.inner("Comparable")
val CHAR_SEQUENCE: TypePredicate = KOTLIN.inner("CharSequence")

// TODO: replace all NamePredicate usages to TypePredicate
/**
 * A more precise NamePredicate analog, that checks fully-qualified
 * name rather than simple name.
 *
 * @see org.jetbrains.kotlin.js.patterns.NamePredicate
 */
private class TypePredicateImpl
    private constructor (private val nameParts: List<String>)
: TypePredicate {
    constructor(name: String) : this(listOf(name))

    override fun apply(type: KotlinType?): Boolean {
        var descriptor: DeclarationDescriptor? = type?.constructor?.declarationDescriptor ?: return false

        for (i in nameParts.lastIndex downTo 0) {
            if (nameParts[i] != descriptor?.name?.asString()) return false

            descriptor = descriptor.containingDeclaration
        }

        return true
    }

    fun inner(name: String) = TypePredicateImpl(nameParts + listOf(name))
}
