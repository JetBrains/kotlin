/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.tree.generator.config

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.sir.tree.generator.Model
import org.jetbrains.kotlin.sir.tree.generator.model.Element
import org.jetbrains.kotlin.sir.tree.generator.model.Field
import org.jetbrains.kotlin.sir.tree.generator.model.ListField
import org.jetbrains.kotlin.sir.tree.generator.model.SimpleField
import org.jetbrains.kotlin.types.Variance
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

// TODO: This class was copy-pasted from the IR tree generator. It'd be good to factor out common parts.
abstract class AbstractSwiftIrTreeBuilder {
    private val configurationCallbacks = mutableListOf<() -> Element>()

    abstract val rootElement: Element

    private fun createElement(name: String? = null, isSealed: Boolean, initializer: Element.() -> Unit = {}): ElementDelegate {
        val del = ElementDelegate(name, isSealed)
        configurationCallbacks.add {
            del.element!!.apply {
                initializer()
                if (elementParents.isEmpty() && this != rootElement) {
                    elementParents.add(ElementRef(rootElement))
                }
            }
        }
        return del
    }

    fun element(name: String? = null, initializer: Element.() -> Unit = {}) =
        createElement(name, isSealed = false, initializer)

    fun sealedElement(name: String? = null, initializer: Element.() -> Unit = {}) =
        createElement(name, isSealed = true, initializer)

    protected fun Element.parent(type: ClassRef<*>) {
        otherParents.add(type)
    }

    protected fun Element.parent(type: ElementOrRef<Element>) {
        elementParents.add(ElementRef(type.element, type.args, type.nullable))
    }

    protected fun param(name: String, vararg bounds: TypeRef, variance: Variance = Variance.INVARIANT): TypeVariable {
        return TypeVariable(name, bounds.toList(), variance)
    }

    protected fun field(
        name: String,
        type: TypeRefWithNullability,
        nullable: Boolean = false,
        mutable: Boolean = false,
        isChild: Boolean = true,
        initializer: SimpleField.() -> Unit = {}
    ): SimpleField {
        return SimpleField(name, type.copy(nullable), mutable).apply {
            this.isChild = isChild
            initializer()
        }
    }

    protected fun listField(
        name: String,
        baseType: TypeRef,
        isChild: Boolean = true,
        initializer: ListField.() -> Unit = {}
    ): ListField {
        return ListField(
            name = name,
            baseType = baseType,
            isMutable = false,
        ).apply {
            this.isChild = isChild
            initializer()
        }
    }

    fun build(): Model {
        val elements = configurationCallbacks.map { it() }
        return Model(elements, rootElement)
    }
}

class ElementDelegate(
    private val name: String?,
    private val isSealed: Boolean,
) : ReadOnlyProperty<AbstractSwiftIrTreeBuilder, Element>, PropertyDelegateProvider<AbstractSwiftIrTreeBuilder, ElementDelegate> {
    var element: Element? = null
        private set

    override fun getValue(thisRef: AbstractSwiftIrTreeBuilder, property: KProperty<*>): Element {
        return element!!
    }

    override fun provideDelegate(thisRef: AbstractSwiftIrTreeBuilder, property: KProperty<*>): ElementDelegate {
        val path = thisRef.javaClass.name + "." + property.name
        element = Element(name ?: property.name.replaceFirstChar(Char::uppercaseChar), path).also {
            it.isSealed = isSealed
        }
        return this
    }
}
