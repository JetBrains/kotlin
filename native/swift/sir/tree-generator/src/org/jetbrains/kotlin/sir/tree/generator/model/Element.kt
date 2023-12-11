/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.tree.generator.model

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.sir.tree.generator.BASE_PACKAGE

class Element(name: String, override val propertyName: String) : AbstractElement<Element, Field, Implementation>(name) {

    override var kDoc: String? = null

    override val fields: MutableSet<Field> = mutableSetOf()

    override val params: MutableList<TypeVariable> = mutableListOf()

    override val elementParents: MutableList<ElementRef<Element>> = mutableListOf()

    override val otherParents: MutableList<ClassRef<*>> = mutableListOf()

    override var visitorParameterName: String = safeDecapitalizedName

    override val hasAcceptMethod: Boolean
        get() = true

    override val hasTransformMethod: Boolean
        get() = true

    override val hasAcceptChildrenMethod: Boolean
        get() = isRootElement

    override val hasTransformChildrenMethod: Boolean
        get() = isRootElement

    override var kind: ImplementationKind? = null

    override val namePrefix: String
        get() = "Sir"

    override val packageName: String
        get() = BASE_PACKAGE

    override val element: Element
        get() = this

    override val nullable: Boolean
        get() = false

    override val args: Map<NamedTypeParameterRef, TypeRef>
        get() = emptyMap()

    operator fun Field.unaryPlus(): Field {
        fields.add(this)
        return this
    }
}