/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.kapt.incremental

import org.jetbrains.kotlin.gradle.util.compileSources
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.Opcodes
import org.junit.Assert
import org.junit.Assert.assertArrayEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.*

class ClassAbiExtractorTest {
    @Rule
    @JvmField
    var tmp = TemporaryFolder()

    @Test
    fun testDifferentClassName() {
        val firstHash = getHash(
            """
                public class A {
                }
        """.trimIndent()
        )

        val secondHash = getHash(
            """
                public class B {
                }
        """.trimIndent(), "B"
        )

        assertArrayNotEquals(firstHash, secondHash)
    }

    @Test
    fun testAbiMethod() {
        val firstHash = getHash(
            """
                public class A {
                  public void run() {}
                  void doSomething1() {}
                }
        """.trimIndent()
        )

        val secondHash = getHash(
            """
                public class A {
                  public void run() {}
                  void doSomething2() {}
                }
        """.trimIndent()
        )

        assertArrayNotEquals(firstHash, secondHash)
    }

    @Test
    fun testAbiMethodAnnotations() {
        val firstHash = getHash(
            """
                public class A {
                  @Annotation1
                  public void run() {}
                }
                @interface Annotation1 {}
        """.trimIndent()
        )

        val secondHash = getHash(
            """
                public class A {
                  @Annotation2
                  public void run() {}
                }
                @interface Annotation2 {}
        """.trimIndent()
        )

        assertArrayNotEquals(firstHash, secondHash)
    }

    @Test
    fun testMethodBodiesIgnored() {
        val firstHash = getHash(
            """
                public class A {
                  public void run() {
                    System.out.println("1");
                  }
                }
        """.trimIndent()
        )

        val secondHash = getHash(
            """
                public class A {
                  public void run() {
                    System.out.println("2");
                  }
                }
        """.trimIndent()
        )

        assertArrayEquals(firstHash, secondHash)
    }

    @Test
    fun testPrivateMethodIgnored() {
        val firstHash = getHash(
            """
                public class A {
                  public void run() {}
                  private void doSomething1() {}
                }
        """.trimIndent()
        )

        val secondHash = getHash(
            """
                public class A {
                  public void run() {}
                  private void doSomething2() {}
                }
        """.trimIndent()
        )

        assertArrayEquals(firstHash, secondHash)
    }

    @Test
    fun testAbiField() {
        val firstHash = getHash(
            """
                public class A {
                  protected String value;
                  public String data1;
                }
        """.trimIndent()
        )

        val secondHash = getHash(
            """
                public class A {
                  protected String value;
                  public String data2;
                }
        """.trimIndent()
        )

        assertArrayNotEquals(firstHash, secondHash)
    }

    @Test
    fun testFieldAnnotation() {
        val firstHash = getHash(
            """
                public class A {
                  @Annotation1
                  protected String value;
                }
                @interface Annotation1 {}
        """.trimIndent()
        )

        val secondHash = getHash(
            """
                public class A {
                  @Annotation2
                  protected String value;
                }
                @interface Annotation2 {}
        """.trimIndent()
        )

        assertArrayNotEquals(firstHash, secondHash)
    }

    @Test
    fun testConstants() {
        val firstHash = getHash(
            """
                public class A {
                  static final String VALUE = "value_1";
                }
        """.trimIndent()
        )

        val secondHash = getHash(
            """
                public class A {
                  static final String VALUE = "value_2";
                }
        """.trimIndent()
        )

        assertArrayNotEquals(firstHash, secondHash)
    }

    @Test
    fun testSameConstants() {
        val firstHash = getHash(
            """
                public class A {
                  static final String VALUE = "value_1";
                }
        """.trimIndent()
        )

        val secondHash = getHash(
            """
                public class A {
                  static final String VALUE = "value_1";
                }
        """.trimIndent()
        )

        assertArrayEquals(firstHash, secondHash)
    }

    @Test
    fun testPrivateFieldsIgnored() {
        val firstHash = getHash(
            """
                public class A {
                  protected String value;
                  private String data;
                }
        """.trimIndent()
        )

        val secondHash = getHash(
            """
                public class A {
                  protected String value;
                  private int data;
                }
        """.trimIndent()
        )

        assertArrayEquals(firstHash, secondHash)
    }

    @Test
    fun testAbiInnerClass() {
        val firstHash = getHash(
            """
                public class A {
                  class Inner1 {}
                }
        """.trimIndent()
        )

        val secondHash = getHash(
            """
                public class A {
                  class Inner2 {}
                }
        """.trimIndent()
        )

        assertArrayNotEquals(firstHash, secondHash)
    }


    @Test
    fun testPrivateInnerClassesIgnored() {
        val firstHash = getHash(
            """
                public class A {
                  protected String value;
                  private String data;

                  private static class Inner1 {}
                }
        """.trimIndent()
        )

        val secondHash = getHash(
            """
                public class A {
                  protected String value;
                  private int data;
                  private static class Inner2 {}
                }
        """.trimIndent()
        )

        assertArrayEquals(firstHash, secondHash)
    }

    @Test
    fun testKotlinMetadataIgnored() {
        val firstHash = getHash(
            """
                package kotlin;

                @Metadata
                public class A {
                }
                @interface Metadata {}
        """.trimIndent()
        )

        val secondHash = getHash(
            """
                package kotlin;
                public class A {

                }
        """.trimIndent()
        )

        assertArrayEquals(firstHash, secondHash)
    }

    private fun assertArrayNotEquals(first: ByteArray, second: ByteArray) {
        Assert.assertFalse(Arrays.equals(first, second))
    }


    private fun getHash(source: String, className: String = "A"): ByteArray {
        val src = tmp.newFolder().resolve("$className.java")

        src.writeText(source)

        val output = tmp.newFolder()
        compileSources(listOf(src), output)

        val classFile = output.walk().filter { it.name == "$className.class" }.single()

        classFile.inputStream().use {
            val extractor = ClassAbiExtractor(ClassWriter(0))
            ClassReader(it.readBytes()).accept(extractor, ClassReader.SKIP_CODE or ClassReader.SKIP_FRAMES or ClassReader.SKIP_DEBUG)
            return extractor.getBytes()
        }
    }
}