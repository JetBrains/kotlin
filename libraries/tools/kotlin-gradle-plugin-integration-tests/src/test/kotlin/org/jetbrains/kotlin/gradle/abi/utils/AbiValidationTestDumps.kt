/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.abi.utils

import org.jetbrains.kotlin.gradle.testbase.assertFileExists
import java.io.File
import kotlin.test.assertEquals

object AbiValidationTestDumps {
    val SIMPLE_CLASS = """
        package test.classes
        
        class SimpleClass {
            fun foo() = 42
        }
    """.trimIndent()

    val SIMPLE_DUMP_JVM = """
        public final class test/classes/SimpleClass {
        	public fun <init> ()V
        	public final fun foo ()I
        }


    """.trimIndent()

    val FILE_FOR_FILTERS = """
        package test.classes
        
        annotation class Exclude
        annotation class Include
        
        @Include
        class FirstClass {
        }
        
        @Include
        class SecondClass {
        }

        class NoAnnotationClass {
        }
        
        @Include
        class ExcludedByNameClass {
        }
        
        @Exclude
        @Include
        class ExcludedByAnnotationClass {
        }
        
        @Include
        class Foo {
            
        }

        @Include
        class IncludedByName {
            
        }

        @Include
        class IncludedByAnnotation {
            
        }
        
        
        
    """.trimIndent()

    fun assertDumpsEqual(expected: String, actualFile: File) {
        assertFileExists(actualFile)
        assertEquals(expected.normalizeNewlines(), actualFile.readText().normalizeNewlines(), "Dumps are not equal")
    }

    internal fun CharSequence.normalizeNewlines(): String {
        return toString().replace("\r\n", "\n")
    }
}
