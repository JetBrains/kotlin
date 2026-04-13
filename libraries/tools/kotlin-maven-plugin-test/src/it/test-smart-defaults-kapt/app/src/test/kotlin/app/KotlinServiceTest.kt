/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package app

import org.junit.Assert.assertTrue
import org.junit.Test

class KotlinServiceTest {
    @Test
    fun testGeneratedKotlinExtension() {
        val result = KotlinService().generatedExtension()
        assertTrue("Generated extension should contain class name", "KotlinService" in result)
    }

    @Test
    fun testGeneratedJavaClassFromKotlin() {
        val message = KotlinServiceGenerated.getGeneratedMessage()
        assertTrue("Generated message should mention KotlinService", "KotlinService" in message)
    }
}
