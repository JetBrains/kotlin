/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.tree

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.nj2k.tree.visitors.JKVisitor
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


interface JKElement {
    val parent: JKElement?
    fun detach(from: JKElement)
    fun attach(to: JKElement)
}

private class JKChild<T : JKElement>(val value: Int) : ReadWriteProperty<JKTreeElement, T> {
    override operator fun getValue(thisRef: JKTreeElement, property: KProperty<*>): T {
        @Suppress("UNCHECKED_CAST")
        return thisRef.children[value] as T
    }

    override operator fun setValue(thisRef: JKTreeElement, property: KProperty<*>, value: T) {
        @Suppress("UNCHECKED_CAST")
        (thisRef.children[this.value] as T).detach(thisRef)
        thisRef.children[this.value] = value
        value.attach(thisRef)
    }
}

private class JKListChild<T : JKElement>(val value: Int) : ReadWriteProperty<JKTreeElement, List<T>> {
    override operator fun getValue(thisRef: JKTreeElement, property: KProperty<*>): List<T> {
        @Suppress("UNCHECKED_CAST")
        return thisRef.children[value] as List<T>
    }

    override operator fun setValue(thisRef: JKTreeElement, property: KProperty<*>, value: List<T>) {
        @Suppress("UNCHECKED_CAST")
        (thisRef.children[this.value] as List<T>).forEach { it.detach(thisRef) }
        thisRef.children[this.value] = value
        value.forEach { it.attach(thisRef) }
    }
}


abstract class JKTreeElement : JKElement, JKFormattingOwner, Cloneable {
    override val trailingComments: MutableList<JKComment> = mutableListOf()
    override val leadingComments: MutableList<JKComment> = mutableListOf()
    override var hasTrailingLineBreak = false
    override var hasLeadingLineBreak = false

    override var parent: JKElement? = null

    override fun detach(from: JKElement) {
        val prevParent = parent
        require(from == prevParent) {
            "Incorrect detach: From: $from, Actual: $prevParent"
        }
        parent = null
    }

    override fun attach(to: JKElement) {
        check(parent == null)
        parent = to
    }

    open fun accept(visitor: JKVisitor) = visitor.visitTreeElement(this)

    private var childNum = 0

    protected fun <T : JKTreeElement, U : T> child(v: U): ReadWriteProperty<JKTreeElement, T> {
        children.add(childNum, v)
        v.attach(this)
        return JKChild(childNum++)
    }

    protected inline fun <reified T : JKTreeElement> children(): ReadWriteProperty<JKTreeElement, List<T>> {
        return children(emptyList())
    }

    protected fun <T : JKTreeElement> children(v: List<T>): ReadWriteProperty<JKTreeElement, List<T>> {
        children.add(childNum, v)
        v.forEach { it.attach(this) }
        return JKListChild(childNum++)
    }

    open fun acceptChildren(visitor: JKVisitor) {
        forEachChild { it.accept(visitor) }
    }

    private inline fun forEachChild(block: (JKTreeElement) -> Unit) {
        children.forEach {
            @Suppress("UNCHECKED_CAST")
            if (it is JKTreeElement)
                block(it)
            else
                (it as? List<JKTreeElement>)?.forEach { block(it) }
        }
    }


    private var valid: Boolean = true

    fun invalidate() {
        forEachChild { it.detach(this) }
        valid = false
    }

    fun onAttach() {
        check(valid)
    }

    var children: MutableList<Any> = mutableListOf()
        private set

    @Suppress("UNCHECKED_CAST")
    open fun copy(): JKTreeElement {
        val cloned = clone() as JKTreeElement
        val deepClonedChildren =
            cloned.children.map {
                when (it) {
                    is JKTreeElement -> it.copy()
                    is List<*> -> (it as List<JKTreeElement>).map { it.copy() }
                    else -> error("Tree is corrupted")
                }
            }

        deepClonedChildren.forEach { child ->
            when (child) {
                is JKTreeElement -> {
                    child.detach(this)
                    child.attach(cloned)
                }
                is List<*> -> (child as List<JKTreeElement>).forEach {
                    it.detach(this)
                    it.attach(cloned)
                }
            }
        }
        cloned.children = deepClonedChildren.toMutableList()
        return cloned
    }
}

abstract class JKAnnotationMemberValue : JKTreeElement()

interface PsiOwner {
    var psi: PsiElement?
}

class PsiOwnerImpl(override var psi: PsiElement? = null) : PsiOwner

interface JKTypeArgumentListOwner : JKFormattingOwner {
    var typeArgumentList: JKTypeArgumentList
}

interface JKTypeParameterListOwner : JKFormattingOwner {
    var typeParameterList: JKTypeParameterList
}

