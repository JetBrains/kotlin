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
import org.junit.Assert.assertNull
import org.junit.Test

class TestScheme {
    @Test
    fun canCreateAScheme() {
        val scheme = Scheme(Token("token"))
        assertEquals("[token]", scheme.toString())
    }

    @Test
    fun canCreateASchemeWithOpenAppliers() {
        val scheme = Scheme(Open(0), listOf(Scheme(Open(0))))
        assertEquals("[0, [0]]", scheme.toString())
    }

    @Test
    fun canCreateASchemeWithAnonymousOpens() {
        val scheme = Scheme(Open(-1), listOf(Scheme(Open(-1))))
        assertEquals("[_, [_]]", scheme.toString())
    }

    @Test
    fun canCreateASchemeWithAResultScheme() {
        val scheme = Scheme(Open(0), result = Scheme(Open(0)))
        assertEquals("[0: [0]]", scheme.toString())
    }

    @Test
    fun canCreateAClosedBinding() {
        val scheme = Scheme(Token("token"))
        val bindings = Bindings()
        val context = mutableListOf<Binding>()
        val binding = scheme.target.toBinding(bindings, context)
        assertEquals("token", binding.token)
    }

    @Test
    fun canCreateOpenBindings() {
        val scheme = Scheme(
            Open(0),
            listOf(
                Scheme(Open(0)),
                Scheme(Open(2))
            )
        )
        val bindings = Bindings()
        val context = mutableListOf<Binding>()
        val t1 = scheme.target.toBinding(bindings, context)
        val t2 = scheme.parameters[0].target.toBinding(bindings, context)
        val t3 = scheme.parameters[1].target.toBinding(bindings, context)

        // Binding t1 should bind t2 but leave t3 open
        val closed = bindings.closed("token")
        bindings.unify(t1, closed)
        assertEquals(closed.token, t1.token)
        assertEquals(closed.token, t2.token)
        assertNull(t3.token)
    }

    @Test
    fun testEquals() {
        // Non-unique opens are alpha renamed to lowest unused index
        val schemeA = Scheme(Open(0), listOf(Scheme(Open(0))))
        val schemeB = Scheme(Open(2), listOf(Scheme(Open(2))))
        assertEquals(schemeA, schemeB)

        // Unique opens are alpha renamed to -1
        val schemeOne = Scheme(Open(0), listOf(Scheme(Open(1))))
        val schemeTwo = Scheme(Open(-1), listOf(Scheme(Open(-1))))
        assertEquals(schemeOne, schemeTwo)

        // Bound schemes should be equal
        val boundA = Scheme(Token("one"), listOf(Scheme(Token("two"))))
        val boundB = Scheme(Token("one"), listOf(Scheme(Token("two"))))
        assertEquals(boundA, boundB)
    }

    @Test
    fun testHashCode() {
        // Non-unique opens are alpha renamed to lowest unused index
        val schemeA = Scheme(Open(0), listOf(Scheme(Open(0))))
        val schemeB = Scheme(Open(2), listOf(Scheme(Open(2))))
        assertEquals(schemeA.hashCode(), schemeB.hashCode())

        // Unique opens are alpha renamed to -1
        val schemeOne = Scheme(Open(0), listOf(Scheme(Open(1))))
        val schemeTwo = Scheme(Open(-1), listOf(Scheme(Open(-1))))
        assertEquals(schemeOne.hashCode(), schemeTwo.hashCode())

        // Bound schemes should renamed
        val boundA = Scheme(Token("one"), listOf(Scheme(Token("two"))))
        val boundB = Scheme(Token("one"), listOf(Scheme(Token("two"))))
        assertEquals(boundA, boundB)
    }

    @Test
    fun testSerialization() {
        val uiToken = Token("androidx.compose.ui.UI")
        val ui = Scheme(uiToken)
        val a = Open(-1)
        val aScheme = Scheme(a)
        val z = Open(0)
        val one = Open(1)
        val oneScheme = Scheme(one)
        val schemes = listOf(
            ui,
            Scheme(z, listOf(Scheme(z))),
            Scheme(Token("This is a really long token with special chars [],_,123")),
            Scheme(Token("Contains a \" character")),
            Scheme(Token("Contains a \\ character")),
            Scheme(one, listOf(Scheme(Open(2), listOf(Scheme(Open(3)))))),
            Scheme(uiToken, listOf(ui, ui, ui, ui, ui, ui)),
            Scheme(a, listOf(aScheme, aScheme, aScheme)),
            Scheme(one, listOf(oneScheme, aScheme, oneScheme, aScheme)),
            Scheme(Open(Int.MAX_VALUE), listOf(Scheme(Open(Int.MAX_VALUE)))),
            Scheme(Open(Int.MIN_VALUE), listOf(Scheme(Open(Int.MIN_VALUE)))),
            Scheme(
                target = z,
                result = oneScheme
            ),
            Scheme(
                target = z,
                anyParameters = true
            )
        )

        for (scheme in schemes) {
            val serialized = scheme.serialize()
            val processedScheme = deserializeScheme(serialized)
            assertEquals(scheme, processedScheme)
        }
    }

    @Test
    fun invalidDeserializationCanBeCaught() {
        val invalids = listOf(
            "",
            "[ ]",
            "[123123123123123123123123123123123123]",
            "[\"",
            "[a[a[a[a[a",
            "[a[a[a[a[a[\"",
            "[UI] ",
            "[\"\\u0000\"]",
            "[0*[0]]",
            "[0[0]*]"
        )
        for (invalid in invalids) {
            val result = deserializeScheme(invalid)
            assertNull(result)
        }
    }
}