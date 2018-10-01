/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.tree.impl

import org.jetbrains.kotlin.j2k.tree.JKBranchElement
import org.jetbrains.kotlin.j2k.tree.JKElement
import org.jetbrains.kotlin.j2k.tree.JKTreeElement
import org.jetbrains.kotlin.j2k.tree.visitors.JKVisitor
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


private class JKChild<T : JKElement>(val value: Int) : ReadWriteProperty<JKBranchElementBase, T> {
    override operator fun getValue(thisRef: JKBranchElementBase, property: KProperty<*>): T {
        return thisRef.children[value] as T
    }

    override operator fun setValue(thisRef: JKBranchElementBase, property: KProperty<*>, value: T) {
        (thisRef.children[this.value] as T).detach(thisRef)
        thisRef.children[this.value] = value
        value.attach(thisRef)
    }
}

private class JKListChild<T : JKElement>(val value: Int) : ReadWriteProperty<JKBranchElementBase, List<T>> {
    override operator fun getValue(thisRef: JKBranchElementBase, property: KProperty<*>): List<T> {
        return thisRef.children[value] as List<T>
    }

    override operator fun setValue(thisRef: JKBranchElementBase, property: KProperty<*>, value: List<T>) {
        (thisRef.children[this.value] as List<T>).forEach { it.detach(thisRef) }
        thisRef.children[this.value] = value
        value.forEach { it.attach(thisRef) }
    }
}

abstract class JKElementBase : JKTreeElement, Cloneable {
    override var parent: JKElement? = null

    final override fun detach(from: JKElement) {
        val prevParent = parent
        require(from == prevParent)
        parent = null
        onDetach(prevParent)
    }

    open fun onDetach(from: JKElement) {

    }

    final override fun attach(to: JKElement) {
        check(parent == null)
        parent = to
        onAttach()
    }

    open fun onAttach() {

    }

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R = visitor.visitTreeElement(this, data)

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {}

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

    final override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D) {
        forEachChild { it.accept(visitor, data) }
    }

    protected inline fun forEachChild(block: (JKTreeElement) -> Unit) {
        children.forEach {
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
