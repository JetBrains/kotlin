/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.exceptions

import kotlin.addSuppressed as addSuppressedExtension
import kotlin.test.*

class ExceptionTest {

    @Test
    fun addSuppressedWorksWithoutJdk7Extensions() {
        val e1 = Throwable()
        val e2 = Exception("Suppressed")

        e1.addSuppressedExtension(e2)

        assertSame(e2, e1.suppressed.singleOrNull())
    }

}