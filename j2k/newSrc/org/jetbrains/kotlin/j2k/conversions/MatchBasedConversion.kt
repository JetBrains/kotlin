/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.tree.JKTreeElement
import org.jetbrains.kotlin.j2k.tree.impl.JKBranchElementBase

abstract class MatchBasedConversion : BaseConversion() {

    fun applyRecursive(element: JKTreeElement, func: (JKTreeElement) -> JKTreeElement): JKTreeElement {
        if (element is JKBranchElementBase) {
            val iter = element.children.listIterator()
            while (iter.hasNext()) {
                val child = iter.next()

                if (child is List<*>) {
                    applyRecursiveToList(element, child as List<JKTreeElement>, iter, func)
                } else if (child is JKTreeElement) {
                    val newChild = func(child)
                    if (child !== newChild) {
                        child.detach(element)
                        iter.set(newChild)
                        newChild.attach(element)
                        onElementChanged(newChild, child)
                    }
                } else {
                    error("unsupported child type: ${child::class}")
                }
            }
        }
        return element
    }

    private inline fun applyRecursiveToList(
        element: JKTreeElement,
        child: List<JKTreeElement>,
        iter: MutableListIterator<Any>,
        func: (JKTreeElement) -> JKTreeElement
    ) {

        val newChild = child.map {
            func(it)
        }

        child.forEach { it.detach(element) }
        iter.set(child)
        newChild.forEach { it.attach(element) }
        newChild.zip(child).forEach { (old, new) ->
            if (old !== new) {
                onElementChanged(new, old)
            }
        }
    }


    abstract fun onElementChanged(new: JKTreeElement, old: JKTreeElement)
}