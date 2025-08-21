/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.metadata.test

import org.junit.jupiter.api.Test
import kotlin.metadata.jvm.hasAnnotationsInBytecode
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Suppress("unused")
class HasAnnotationsTest {
    @Test
    fun testHasAnnotations() {
        val klass = A::class.java.readMetadataAsKmClass()
        assertTrue(klass.hasAnnotationsInBytecode)
        assertFalse(Anno::class.java.readMetadataAsKmClass().hasAnnotationsInBytecode)

        assertTrue(klass.constructors.single { it.valueParameters.size == 1 }.hasAnnotationsInBytecode)
        val ctor = klass.constructors.single { it.valueParameters.size == 2 }
        assertFalse(ctor.hasAnnotationsInBytecode)

        assertTrue(ctor.valueParameters[0].hasAnnotationsInBytecode)
        assertFalse(ctor.valueParameters[1].hasAnnotationsInBytecode)

        assertTrue(klass.functions.single { it.name == "f" }.hasAnnotationsInBytecode)
        assertFalse(klass.functions.single { it.name == "g" }.hasAnnotationsInBytecode)

        val p = klass.properties.single { it.name == "p" }
        val q = klass.properties.single { it.name == "q" }
        assertTrue(p.hasAnnotationsInBytecode)
        assertFalse(q.hasAnnotationsInBytecode)

        assertFalse(p.getter.hasAnnotationsInBytecode)
        assertFalse(p.setter!!.hasAnnotationsInBytecode)

        assertTrue(q.getter.hasAnnotationsInBytecode)
        assertTrue(q.setter!!.hasAnnotationsInBytecode)
    }

    private annotation class Anno

    @Anno
    private class A {
        @Anno
        constructor(x: Int)

        constructor(@Anno x: Int, y: Int)

        @Anno
        fun f() {
        }

        fun g() {}

        @Anno
        var p = 1

        var q = 2
            @Anno get
            @Anno set
    }
}
