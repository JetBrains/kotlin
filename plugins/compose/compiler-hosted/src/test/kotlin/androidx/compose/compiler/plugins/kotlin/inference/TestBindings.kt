/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.compiler.plugins.kotlin.inference

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TestBindings {

    @Test
    fun canCreateABindings() {
        val bindings = Bindings()
        assertNotNull(bindings)
    }

    @Test
    fun canCreateAnOpenBinding() {
        val bindings = Bindings()
        val open = bindings.open()
        assertNotNull(open)
    }

    @Test
    fun canCreateAClosedBinding() {
        val bindings = Bindings()
        val closed = bindings.closed("token")
        assertNotNull(closed)
        assertEquals("token", closed.token)
    }

    @Test
    fun canBindAOpenBindingToAClosedBinding() {
        val bindings = Bindings()
        val open1 = bindings.open()
        val open2 = bindings.open()
        val closed = bindings.closed("token")
        bindings.unify(open1, closed)
        assertEquals(closed.token, open1.token)
        assertNull(open2.token)
        bindings.unify(closed, open2)
    }

    @Test
    fun canBindOpenBindingsTogetherAndTheyCloseTogether() {
        val bindings = Bindings()
        val opens = Array(100) { bindings.open() }
        val closed = bindings.closed("token")

        // Bind them together
        opens.forEach { bindings.unify(opens[0], it) }

        // Ensure they are all still open.
        opens.forEach { assertNull(it.token) }

        // Bind one
        bindings.unify(closed, opens[42])

        // Binding one should bind them all.
        opens.forEach { assertEquals(closed.token, it.token) }
    }

    @Test
    fun canBindLargeGroupsOfBindingsTogether() {
        val bindings = Bindings()
        val opens1 = Array(100) { bindings.open() }
        val opens2 = Array(100) { bindings.open() }
        val closed = bindings.closed("token")

        opens1.forEach { bindings.unify(opens1[0], it) }
        opens2.forEach { bindings.unify(opens2[0], it) }

        bindings.unify(opens1[42], opens2[42])
        bindings.unify(opens1.first(), closed)

        opens1.forEach { assertEquals(closed.token, it.token) }
        opens2.forEach { assertEquals(closed.token, it.token) }
    }

    @Test
    fun detectUnifyConflicts() {
        val bindings = Bindings()
        val open1 = bindings.open()
        val open2 = bindings.open()
        val closed1 = bindings.closed("token 1")
        val closed2 = bindings.closed("token 2")
        assertTrue(bindings.unify(open1, closed1))
        assertTrue(bindings.unify(open2, closed2))

        assertTrue(bindings.unify(open1, open1))
        assertFalse(bindings.unify(open1, open2))
        assertTrue(bindings.unify(open1, closed1))
        assertFalse(bindings.unify(open1, closed2))
        assertFalse(bindings.unify(open2, open1))
        assertTrue(bindings.unify(open2, open2))
        assertFalse(bindings.unify(open2, closed1))
        assertTrue(bindings.unify(open2, closed2))
        assertTrue(bindings.unify(closed1, open1))
        assertFalse(bindings.unify(closed1, open2))
        assertTrue(bindings.unify(closed1, closed1))
        assertFalse(bindings.unify(closed1, closed2))
        assertFalse(bindings.unify(closed2, open1))
        assertTrue(bindings.unify(closed2, open2))
        assertFalse(bindings.unify(closed2, closed1))
        assertTrue(bindings.unify(closed2, closed2))
    }

    @Test
    fun canObserveChanges() {
        val bindings = Bindings()
        var changes = 0
        val remove = bindings.onChange { changes++ }
        val open1 = bindings.open()
        val open2 = bindings.open()
        val closed = bindings.closed("token")
        assertEquals(0, changes)
        bindings.unify(open1, open2)
        assertEquals(1, changes)
        bindings.unify(closed, open1)
        assertEquals(2, changes)
        bindings.unify(closed, open2)
        assertEquals(2, changes)

        // Remove the observer
        remove()
        val open3 = bindings.open()
        bindings.unify(open3, closed)
        assertEquals(2, changes)
    }

    @Test
    fun canChangeBindingInForeignBindingAndReceiveNotificationsInBoth() {
        val aBindings = Bindings()
        var aChanges = 0
        aBindings.onChange { aChanges++ }
        val bBindings = Bindings()
        var bChanges = 0
        bBindings.onChange { bChanges++ }
        val cBindings = Bindings()
        var cChanges = 0
        cBindings.onChange { cChanges++ }

        fun expect(a: Int, b: Int, c: Int) {
            assertEquals(aChanges, a)
            assertEquals(bChanges, b)
            assertEquals(cChanges, c)
        }

        val aOpen = aBindings.open()
        val bOpen = bBindings.open()
        val cClosed = cBindings.closed("c")
        cBindings.unify(aOpen, bOpen)
        expect(a = 1, b = 1, c = 0)
        cBindings.unify(aOpen, cClosed)
        expect(a = 2, b = 2, c = 0)
        cBindings.unify(bOpen, cClosed)
        expect(a = 2, b = 2, c = 0)
    }
}