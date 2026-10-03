/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.compiler

import org.jetbrains.kotlin.arguments.collectProperties
import org.jetbrains.kotlin.cli.common.arguments.ArgumentParseErrors
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.compilerRunner.copyProperties
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaField

class ArgumentUtilTest {
    @Test
    fun testCopyDoesNotCopyTransientFields() {
        assertTrue(Modifier.isTransient(K2JVMCompilerArguments::errors.javaField!!.modifiers))

        val a = K2JVMCompilerArguments()
        a.errors = ArgumentParseErrors()
        a.moduleName = "my module name"

        val b = K2JVMCompilerArguments()
        assertNull(b.errors)
        assertNull(b.moduleName)

        copyBeanTo(a, b)

        assertNull(b.errors)
        assertEquals("my module name", b.moduleName)
    }

    private fun <T : CommonToolArguments> copyBeanTo(from: T, to: T, filter: ((KProperty1<T, Any?>, Any?) -> Boolean)? = null): T {
        @Suppress("UNCHECKED_CAST")
        val propertiesToCopy = collectProperties(from::class as KClass<T>, inheritedOnly = false)
        return copyProperties(
            from, to,
            deepCopyWhenNeeded = true,
            propertiesToCopy,
            filter = filter
        )
    }
}
