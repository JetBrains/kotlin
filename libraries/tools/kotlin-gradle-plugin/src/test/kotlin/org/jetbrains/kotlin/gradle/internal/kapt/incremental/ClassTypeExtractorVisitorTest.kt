/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.kapt.incremental

import org.jetbrains.kotlin.gradle.util.compileSources
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
class ClassTypeExtractorVisitorTest {
    @Rule
    @JvmField
    var tmp = TemporaryFolder()

    @Test
    fun testSupertypes() {
        val src = """
            public class A extends B implements java.lang.Runnable {
              public void run() {}
            }

            class B {}
        """.trimIndent()
        val (abiTypes, privateTypes) = extractTypesFor(src)
        assertEquals(setOf("B", "java/lang/Runnable"), abiTypes)
        assertEquals(emptySet<String>(), privateTypes)
    }

    @Test
    fun testObjectIgnored() {
        val src = """
            public class A {}
        """.trimIndent()
        val (abiTypes, privateTypes) = extractTypesFor(src)
        assertEquals(emptySet<String>(), abiTypes)
        assertEquals(emptySet<String>(), privateTypes)
    }

    @Test
    fun testMethod() {
        val src = """
            public class A {
              public String process(Cloneable c) {
                Runnable ignored = null;
                return null;
              }
              public int ignorePrimitiveTypes(char c, boolean z, float f, double d, short s, long j, long[] jArray) {
                return 1;
              }
              private java.util.HashSet[] getSets(java.util.ArrayList list) {
                Runnable ignored = null;
                return null;
              }
            }
        """.trimIndent()
        val (abiTypes, privateTypes) = extractTypesFor(src)
        assertEquals(setOf("java/lang/String", "java/lang/Cloneable"), abiTypes)
        assertEquals(setOf("java/util/HashSet", "java/util/ArrayList"), privateTypes)
    }

    @Test
    fun testField() {
        val src = """
            public class A {
              public String first;
              protected String second;
              String third;
              private Runnable fourth;
              public int ignored;
            }
        """.trimIndent()
        val (abiTypes, privateTypes) = extractTypesFor(src)
        assertEquals(setOf("java/lang/String"), abiTypes)
        assertEquals(setOf("java/lang/Runnable"), privateTypes)
    }

    @Test
    fun testClassAnnotations() {
        val src = """
            import java.lang.annotation.*;

            @Annotation
            public class A extends @TypeAnnotation B {
            }
            class B {}
            @interface Annotation {}

            @Target(value=ElementType.TYPE_USE)
            @interface TypeAnnotation {}
        """.trimIndent()
        val (abiTypes, privateTypes) = extractTypesFor(src)
        assertEquals(setOf("B", "Annotation", "TypeAnnotation"), abiTypes)
        assertEquals(emptySet<String>(), privateTypes)
    }

    @Test
    fun testMemberAnnotations() {
        val src = """
            import java.lang.annotation.*;

            public class A {
              @FieldAnnotation String data;

              @MethodAnnotation
              String getName(@ParameterAnnotation String originalName) {
                return originalName + "suffix";
              }
            }
            @interface FieldAnnotation {}
            @interface MethodAnnotation {}
            @interface ParameterAnnotation {}
        """.trimIndent()
        val (abiTypes, privateTypes) = extractTypesFor(src)
        assertEquals(setOf("FieldAnnotation", "MethodAnnotation", "ParameterAnnotation", "java/lang/String"), abiTypes)
        assertEquals(emptySet<String>(), privateTypes)
    }

    private fun extractTypesFor(source: String, className: String = "A"): Pair<Set<String>, Set<String>> {
        val src = tmp.newFolder().resolve("$className.java")
        src.writeText(source)

        val output = tmp.newFolder()
        compileSources(listOf(src), output)
        val classFile = output.walk().filter { it.name == "$className.class" }.single()
        val extractor = ClassTypeExtractorVisitor(object : ClassVisitor(Opcodes.API_VERSION) {})

        classFile.inputStream().use {
            ClassReader(it.readBytes()).accept(extractor, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
            return Pair(extractor.getAbiTypes(), extractor.getPrivateTypes())
        }
    }
}