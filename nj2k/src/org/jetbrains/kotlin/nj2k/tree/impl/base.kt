/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.tree.impl

import org.jetbrains.kotlin.nj2k.tree.JKBranchElement
import org.jetbrains.kotlin.nj2k.tree.JKElement
import org.jetbrains.kotlin.nj2k.tree.JKNonCodeElement
import org.jetbrains.kotlin.nj2k.tree.JKTreeElement
import org.jetbrains.kotlin.nj2k.tree.visitors.JKVisitor
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


private class JKChild<T : JKElement>(val value: Int) : ReadWriteProperty<JKBranchElementBase, T> {
    override operator fun getValue(thisRef: JKBranchElementBase, property: KProperty<*>): T {
        @Suppress("UNCHECKED_CAST")
        return thisRef.children[value] as T
    }

    override operator fun setValue(thisRef: JKBranchElementBase, property: KProperty<*>, value: T) {
        @Suppress("UNCHECKED_CAST")
        (thisRef.children[this.value] as T).detach(thisRef)
        thisRef.children[this.value] = value
        value.attach(thisRef)
    }
}

private class JKListChild<T : JKElement>(val value: Int) : ReadWriteProperty<JKBranchElementBase, List<T>> {
    override operator fun getValue(thisRef: JKBranchElementBase, property: KProperty<*>): List<T> {
        @Suppress("UNCHECKED_CAST")
        return thisRef.children[value] as List<T>
    }

    override operator fun setValue(thisRef: JKBranchElementBase, property: KProperty<*>, value: List<T>) {
        @Suppress("UNCHECKED_CAST")
        (thisRef.children[this.value] as List<T>).forEach { it.detach(thisRef) }
        thisRef.children[this.value] = value
        value.forEach { it.attach(thisRef) }
    }
}

abstract class JKElementBase : JKTreeElement, Cloneable {
    override var leftNonCodeElements: List<JKNonCodeElement> = emptyList()
    override var rightNonCodeElements: List<JKNonCodeElement> = emptyList()

    override var parent: JKElement? = null

    override fun detach(from: JKElement) {
        val prevParent = parent
        require(from == prevParent)
        parent = null
        onDetach(prevParent)
    }

    open fun onDetach(from: JKElement) {

    }

    override fun attach(to: JKElement) {
        check(parent == null)
        parent = to
        onAttach()
    }

    open fun onAttach() {

    }

    override fun accept(visitor: JKVisitor) = visitor.visitTreeElement(this)

    override fun acceptChildren(visitor: JKVisitor) {}

    override fun copy(): JKTreeElement =
        clone() as JKTreeElement
}

abstract class JKBranchElementBase : JKElementBase(), JKBranchElement {
    private var childNum = 0

    protected fun <T : JKTreeElement, U : T> child(v: U): ReadWriteProperty<JKBranchElementBase, T> {
        children.add(childNum, v)
        v.attach(this)
        return JKChild(childNum++)
    }

    protected inline fun <reified T : JKTreeElement> children(): ReadWriteProperty<JKBranchElementBase, List<T>> {
        return children(emptyList())
    }

    protected fun <T : JKTreeElement> children(v: List<T>): ReadWriteProperty<JKBranchElementBase, List<T>> {
        children.add(childNum, v)
        v.forEach { it.attach(this) }
        return JKListChild(childNum++)
    }

    override fun acceptChildren(visitor: JKVisitor) {
        forEachChild { it.accept(visitor) }
    }

    protected inline fun forEachChild(block: (JKTreeElement) -> Unit) {
        children.forEach {
            @Suppress("UNCHECKED_CAST")
            if (it is JKTreeElement)
                block(it)
            else
                (it as? List<JKTreeElement>)?.forEach { block(it) }
        }
    }


    final override var valid: Boolean = true

    final override fun invalidate() {
        forEachChild { it.detach(this) }
        valid = false
    }

    override fun onAttach() {
        check(valid)
    }

    final override var children: MutableList<Any> = mutableListOf()
        private set

    @Suppress("UNCHECKED_CAST")
    override fun copy(): JKTreeElement {
        val cloned = super.copy() as JKBranchElementBase
        val deepClonedChildren =
            cloned.children.map {
                when (it) {
                    is JKElementBase -> it.copy()
                    is List<*> -> (it as List<JKTreeElement>).map { it.copy() }
                    else -> error("Tree is corrupted")
                }
            }

        deepClonedChildren.forEach { child ->
            when (child) {
                is JKElementBase -> {
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
