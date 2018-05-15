/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.tree.JKTreeElement
import org.jetbrains.kotlin.j2k.tree.impl.JKMutableBranchElement

abstract class MatchBasedConversion : BaseConversion() {

    fun applyRecursive(element: JKTreeElement, func: (JKTreeElement) -> JKTreeElement): JKTreeElement {
        if (element is JKMutableBranchElement) {
            val iter = element.children.listIterator()
            while (iter.hasNext()) {
                val child = iter.next()
                val newChild = func(child)
                if (child !== newChild) {
                    onElementChanged(newChild, child)
                    child.parent = null
                    newChild.parent = element
                    iter.set(newChild)
                }
            }
        }
        return element
    }

    abstract fun onElementChanged(new: JKTreeElement, old: JKTreeElement)
}