/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.nj2k.tree

import org.jetbrains.kotlin.nj2k.tree.impl.JKBranchElementBase
import kotlin.jvm.internal.CallableReference
import kotlin.reflect.KProperty0


inline fun <reified T : JKElement> JKElement.parentOfType(): T? {
    return generateSequence(parent) { it.parent }.filterIsInstance<T>().firstOrNull()
}

private fun <T : JKElement> KProperty0<Any>.detach(element: T) {
    if (element.parent == null) return
    // TODO: Fix when KT-16818 is implemented
    val boundReceiver = (this as CallableReference).boundReceiver
    require(boundReceiver != CallableReference.NO_RECEIVER)
    require(boundReceiver is JKElement)
    element.detach(boundReceiver)
}


fun <T : JKElement> KProperty0<T>.detached(): T =
    get().also { detach(it) }

fun <T : JKElement> KProperty0<List<T>>.detached(): List<T> =
    get().also { list -> list.forEach { detach(it) } }

fun <T : JKElement> T.detached(from: JKElement): T =
    also { it.detach(from) }

fun <T : JKBranchElement> T.invalidated(): T =
    also { it.invalidate() }

fun <R : JKTreeElement, T> applyRecursive(
    element: R,
    data: T,
    onElementChanged: (JKTreeElement, JKTreeElement) -> Unit,
    func: (JKTreeElement, T) -> JKTreeElement
): R {
    fun <T> applyRecursiveToList(
        element: JKTreeElement,
        child: List<JKTreeElement>,
        iter: MutableListIterator<Any>,
        data: T,
        func: (JKTreeElement, T) -> JKTreeElement
    ): List<JKTreeElement> {

        val newChild = child.map {
            func(it, data)
        }

        child.forEach { it.detach(element) }
        iter.set(child)
        newChild.forEach { it.attach(element) }
        newChild.zip(child).forEach { (old, new) ->
            if (old !== new) {
                onElementChanged(new, old)
            }
        }
        return newChild
    }

    if (element is JKBranchElementBase) {
        val iter = element.children.listIterator()
        while (iter.hasNext()) {
            val child = iter.next()

            if (child is List<*>) {
                @Suppress("UNCHECKED_CAST")
                iter.set(applyRecursiveToList(element, child as List<JKTreeElement>, iter, data, func))
            } else if (child is JKTreeElement) {
                val newChild = func(child, data)
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

fun <R : JKTreeElement> applyRecursive(
    element: R,
    func: (JKTreeElement) -> JKTreeElement
): R = applyRecursive(element, null, { _, _ -> }) { it, _ -> func(it) }



