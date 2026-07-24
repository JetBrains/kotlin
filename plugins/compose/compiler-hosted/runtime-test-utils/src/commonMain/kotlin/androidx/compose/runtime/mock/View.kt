/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("ListIterator")

package androidx.compose.runtime.mock

import androidx.compose.runtime.ComposeNodeLifecycleCallback
import androidx.compose.runtime.Stable

fun indent(indent: Int, builder: StringBuilder) {
    repeat(indent) { builder.append(' ') }
}

@Stable
interface Modifier {
    companion object : Modifier {}
}

open class View : ComposeNodeLifecycleCallback {
    var name: String = ""
    val children = mutableListOf<View>()
    val attributes = mutableMapOf<String, Any>()
    var onAttach = {}
    var onDetach = {}
    var active = true
    var released = false

    // Used to validated insert/remove constraints
    private var parent: View? = null

    private fun render(indent: Int = 0, builder: StringBuilder) {
        indent(indent, builder)
        builder.append("<$name$attributesAsString")
        if (children.size > 0) {
            builder.appendLine(">")
            children.forEach { it.render(indent + 2, builder) }
            indent(indent, builder)
            builder.appendLine("</$name>")
        } else {
            builder.appendLine(" />")
        }
    }

    fun addAt(index: Int, view: View) {
        val parent = view.parent
        if (parent != null) {
            error(
                "Inserting a view named ${view.name} into a view named $name which already has " +
                    "a parent named ${parent.name}"
            )
        }
        view.parent = this
        view.onAttach()
        children.add(index, view)
    }

    fun removeAt(index: Int, count: Int) {
        check(index in 0 until children.size) { "Expected $index to be less than ${children.size}" }
        if (count == 1) {
            val removedChild = children.removeAt(index)
            removedChild.onDetach()
            removedChild.parent = null
        } else {
            val removedChildren = children.subList(index, index + count)
            removedChildren.forEach { child ->
                child.onDetach()
                child.parent = null
            }
            removedChildren.clear()
        }
    }

    fun moveAt(from: Int, to: Int, count: Int) {
        if (count == 1) {
            val insertLocation = if (from > to) to else (to - 1)
            children.add(insertLocation, children.removeAt(from))
        } else {
            val insertLocation = if (from > to) to else (to - count)
            val itemsToMove = children.subList(from, from + count)
            val copyOfItems = itemsToMove.map { it }
            itemsToMove.clear()
            children.addAll(insertLocation, copyOfItems)
        }
    }

    fun removeAllChildren() {
        children.forEach { child -> child.parent = null }
        children.clear()
    }

    fun attribute(name: String, value: Any) {
        attributes[name] = value
    }

    var value: String?
        get() = attributes["value"] as? String
        set(value) {
            check(active) { "Node modified when it wasn't active: $this" }
            check(!released) { "Node modified when it after release: $this" }
            if (value != null) {
                attributes["value"] = value
            } else {
                attributes.remove("value")
            }
        }

    var text: String?
        get() = attributes["text"] as? String
        set(value) {
            check(active) { "Node modified when it wasn't active: $this" }
            check(!released) { "Node modified when it after release: $this" }
            if (value != null) {
                attributes["text"] = value
            } else {
                attributes.remove("text")
            }
        }

    private val attributesAsString
        get() =
            if (attributes.isEmpty()) ""
            else attributes.map { " ${it.key}='${it.value}'" }.joinToString()

    private val childrenAsString: String
        get() = children.map { it.toString() }.joinToString(" ")

    override fun toString() =
        if (children.isEmpty()) "<$name$attributesAsString>"
        else "<$name$attributesAsString>$childrenAsString</$name>"

    fun toFmtString() =
        StringBuilder().let {
            render(0, it)
            it.toString()
        }

    private fun findFirstOrNull(predicate: (view: View) -> Boolean): View? {
        if (predicate(this)) return this
        for (child in children) {
            child.findFirstOrNull(predicate)?.let {
                return it
            }
        }
        return null
    }

    fun findFirst(predicate: (view: View) -> Boolean) =
        findFirstOrNull(predicate) ?: error("View not found")

    override fun onReuse() {
        // Nodes can be reused without being deactivated. After this call they should be considered
        // active.
        active = true
    }

    override fun onDeactivate() {
        check(active) { "Node deactivated when it was already inactive: $this" }
        active = false
    }

    override fun onRelease() {
        // Re-enable this check when b/411129499 is fixed
        // check(!released) { "Node was released twice: $this" }
        released = true
    }
}

fun View.flatten(): List<View> = listOf(this) + children.flatMap { it.flatten() }
