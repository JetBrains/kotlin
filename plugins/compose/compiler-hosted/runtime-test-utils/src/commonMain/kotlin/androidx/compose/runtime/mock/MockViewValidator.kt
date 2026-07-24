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

package androidx.compose.runtime.mock

import kotlin.test.assertEquals
import kotlin.test.assertTrue

interface MockViewValidator {
    val view: View

    fun next(): Boolean
}

class MockViewListValidator(private val views: List<View>) : MockViewValidator {
    override lateinit var view: View

    override fun next(): Boolean {
        if (iterator.hasNext()) {
            view = iterator.next()
            return true
        }
        return false
    }

    private val iterator by lazy { views.iterator() }

    fun validate(block: (MockViewValidator.() -> Unit)?) {
        if (block != null) {
            this.block()
            val hasNext = next()
            assertEquals(false, hasNext, "Expected children but none found")
        } else {
            assertEquals(0, views.size, "Not expecting children but some found")
        }
    }

    inline fun inlineValidate(block: MockViewListValidator.() -> Unit) {
        this.block()
        val hasNext = next()
        assertEquals(false, hasNext, "Expected children but none found")
    }
}

fun MockViewValidator.view(name: String, block: (MockViewValidator.() -> Unit)? = null) {
    val hasNext = next()
    assertTrue(hasNext, "Expected a $name, but none found")
    assertEquals(name, view.name)
    MockViewListValidator(view.children).validate(block)
}

inline fun MockViewValidator.inlineView(name: String, block: MockViewValidator.() -> Unit) {
    val hasNext = next()
    assertTrue(hasNext, "Expected a $name, but none found")
    assertEquals(name, view.name)
    MockViewListValidator(view.children).inlineValidate(block)
}

fun <T> MockViewValidator.Repeated(of: Iterable<T>, block: MockViewValidator.(value: T) -> Unit) {
    for (value in of) {
        block(value)
    }
}

fun MockViewValidator.Linear() = view("linear", null)

fun MockViewValidator.Linear(block: MockViewValidator.() -> Unit) = view("linear", block)

inline fun MockViewValidator.InlineLinear(block: MockViewValidator.() -> Unit) =
    inlineView("linear", block)

fun MockViewValidator.box(block: MockViewValidator.() -> Unit) = view("box", block)

fun MockViewValidator.Text(value: String) {
    view("text")
    assertEquals(value, view.attributes["text"])
}

fun MockViewValidator.Edit(value: String) {
    view("edit")
    assertEquals(value, view.attributes["value"])
}

fun MockViewValidator.SelectBox(selected: Boolean, block: MockViewValidator.() -> Unit) {
    if (selected) {
        box(block)
    } else {
        block()
    }
}

fun MockViewValidator.skip(times: Int = 1) {
    repeat(times) {
        val hasNext = next()
        assertEquals(true, hasNext)
    }
}
