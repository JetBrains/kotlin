/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.jvm.java

import org.jetbrains.dokka.analysis.test.api.javaTestProject
import org.jetbrains.dokka.analysis.test.api.parse
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.WithExtraProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InnerModifierJavaAnalysisTest {

    @Test
    fun `top level java declarations should not have kotlin inner modifier`() {
        val testProject = javaTestProject {
            javaFile(pathFromSrc = "JavaClass.java") {
                +"public class JavaClass { }"
            }
            javaFile(pathFromSrc = "JavaEnum.java") {
                +"public enum JavaEnum { S; }"
            }
            javaFile(pathFromSrc = "JavaInterface.java") {
                +"public interface JavaInterface { }"
            }
            javaFile(pathFromSrc = "JavaAnnotation.java") {
                +"public @interface JavaAnnotation { }"
            }
        }

        val module = testProject.parse()
        val pkg = module.packages.single()

        assertEquals(4, pkg.classlikes.size)

        pkg.classlikes.forEach {
            @Suppress("UNCHECKED_CAST")
            assertTrue((it as WithExtraProperties<Documentable>).kotlinOnlyModifiers().isEmpty())
        }
    }

    @Test
    fun `java declarations nested inside interfaces should not have kotlin inner modifier`() {
        val testProject = javaTestProject {
            javaFile(pathFromSrc = "JavaInterface.java") {
                +"""
                    public interface JavaInterface {
                        public class InnerClass { }
                        public static class NestedClass { }
                        public enum InnerEnum {S}
                        public static enum NestedEnum {S}
                        public interface InnerInterface { }
                        public static interface NestedInterface { }
                        public @interface InnerAnnotation { }
                        public static @interface NestedAnnotation { }
                    }
                """
            }
        }

        val module = testProject.parse()
        val pkg = module.packages.single()
        val javaInterface = pkg.classlikes.single()

        assertTrue(javaInterface is DInterface)
        assertEquals(8, javaInterface.classlikes.size)

        javaInterface.classlikes.forEach {
            @Suppress("UNCHECKED_CAST")
            assertTrue((it as WithExtraProperties<Documentable>).kotlinOnlyModifiers().isEmpty())
        }
    }

    @Test
    fun `java declarations nested inside annotation interface should not have kotlin inner modifier`() {
        val testProject = javaTestProject {
            javaFile(pathFromSrc = "JavaAnnotation.java") {
                +"""
                    public @interface JavaAnnotation {
                        public class InnerClass { }
                        public static class NestedClass { }
                        public enum InnerEnum {S}
                        public static enum NestedEnum {S}
                        public interface InnerInterface { }
                        public static interface NestedInterface { }
                        public @interface InnerAnnotation { }
                        public static @interface NestedAnnotation { }
                    }
                """
            }
        }

        val module = testProject.parse()
        val pkg = module.packages.single()
        val javaAnnotation = pkg.classlikes.single()

        assertTrue(javaAnnotation is DAnnotation)
        assertEquals(8, javaAnnotation.classlikes.size)

        javaAnnotation.classlikes.forEach {
            @Suppress("UNCHECKED_CAST")
            assertTrue((it as WithExtraProperties<Documentable>).kotlinOnlyModifiers().isEmpty())
        }
    }

    // java classes tests

    @Test
    fun `java class nested inside class should have kotlin inner modifiers`() {
        val testProject = javaTestProject {
            javaFile(pathFromSrc = "JavaClass.java") {
                +"""
                    public class JavaClass {
                        public class NestedClass { }
                    }
                """
            }
        }

        val module = testProject.parse()
        val pkg = module.packages.single()
        val javaClass = pkg.classlikes.single()

        assertTrue(javaClass is DClass)
        assertEquals("JavaClass", javaClass.name)

        val nestedClass = javaClass.classlikes.single()

        assertTrue(nestedClass is DClass)
        assertEquals("NestedClass", nestedClass.name)

        assertEquals(
            setOf(ExtraModifiers.KotlinOnlyModifiers.Inner),
            nestedClass.kotlinOnlyModifiers().values.single()
        )
    }

    @Test
    fun `static java class nested inside class should not have kotlin inner modifiers`() {
        val testProject = javaTestProject {
            javaFile(pathFromSrc = "JavaClass.java") {
                +"""
                    public class JavaClass {
                        public static class NestedClass { }
                    }
                """
            }
        }

        val module = testProject.parse()
        val pkg = module.packages.single()
        val javaClass = pkg.classlikes.single()

        assertTrue(javaClass is DClass)
        assertEquals("JavaClass", javaClass.name)

        val nestedClass = javaClass.classlikes.single()

        assertTrue(nestedClass is DClass)
        assertEquals("NestedClass", nestedClass.name)

        assertTrue(nestedClass.kotlinOnlyModifiers().isEmpty())
    }

    @Test
    fun `java non-classes nested inside class should not have kotlin inner modifier`() {
        val testProject = javaTestProject {
            javaFile(pathFromSrc = "JavaClass.java") {
                +"""
                    public class JavaClass {
                        public enum InnerEnum {S}
                        public static enum NestedEnum {S}
                        public interface InnerInterface { }
                        public static interface NestedInterface { }
                        public @interface InnerAnnotation { }
                        public static @interface NestedAnnotation { }
                    }
                """
            }
        }

        val module = testProject.parse()
        val pkg = module.packages.single()
        val javaClass = pkg.classlikes.single()

        assertTrue(javaClass is DClass)
        assertEquals(6, javaClass.classlikes.size)

        javaClass.classlikes.forEach {
            @Suppress("UNCHECKED_CAST")
            assertTrue((it as WithExtraProperties<Documentable>).kotlinOnlyModifiers().isEmpty())
        }
    }

    // java enums tests

    @Test
    fun `static java class nested inside enum should not have kotlin inner modifiers`() {
        val testProject = javaTestProject {
            javaFile(pathFromSrc = "JavaEnum.java") {
                +"""
                    public enum JavaEnum { S;
                        public static class NestedClass { }
                    }
                """
            }
        }

        val module = testProject.parse()
        val pkg = module.packages.single()
        val javaEnum = pkg.classlikes.single()

        assertTrue(javaEnum is DEnum)
        assertEquals("JavaEnum", javaEnum.name)

        val nestedClass = javaEnum.classlikes.single()

        assertTrue(nestedClass is DClass)
        assertEquals("NestedClass", nestedClass.name)

        assertTrue(nestedClass.kotlinOnlyModifiers().isEmpty())
    }

    @Test
    fun `java class nested inside enum should have kotlin inner modifiers`() {
        val testProject = javaTestProject {
            javaFile(pathFromSrc = "JavaEnum.java") {
                +"""
                    public enum JavaEnum { S;
                        public class NestedClass { }
                    }
                """
            }
        }

        val module = testProject.parse()
        val pkg = module.packages.single()
        val javaEnum = pkg.classlikes.single()

        assertTrue(javaEnum is DEnum)
        assertEquals("JavaEnum", javaEnum.name)

        val nestedClass = javaEnum.classlikes.single()

        assertTrue(nestedClass is DClass)
        assertEquals("NestedClass", nestedClass.name)

        assertEquals(
            setOf(ExtraModifiers.KotlinOnlyModifiers.Inner),
            nestedClass.kotlinOnlyModifiers().values.single()
        )
    }

    @Test
    fun `java non-classes nested inside enum should not have kotlin inner modifier`() {
        val testProject = javaTestProject {
            javaFile(pathFromSrc = "JavaEnum.java") {
                +"""
                    public enum JavaEnum { S;
                        public enum InnerEnum {S}
                        public static enum NestedEnum {S}
                        public interface InnerInterface { }
                        public static interface NestedInterface { }
                        public @interface InnerAnnotation { }
                        public static @interface NestedAnnotation { }
                    }
                """
            }
        }

        val module = testProject.parse()
        val pkg = module.packages.single()
        val javaEnum = pkg.classlikes.single()

        assertTrue(javaEnum is DEnum)
        assertEquals(6, javaEnum.classlikes.size)

        javaEnum.classlikes.forEach {
            @Suppress("UNCHECKED_CAST")
            assertTrue((it as WithExtraProperties<Documentable>).kotlinOnlyModifiers().isEmpty())
        }
    }

    // copied from org.jetbrains.dokka.base.signatures.KotlinSignatureUtils
    private fun <T : Documentable> WithExtraProperties<T>.kotlinOnlyModifiers(): SourceSetDependent<Set<ExtraModifiers>> {
        return extra[AdditionalModifiers]?.content?.entries?.associate {
            it.key to it.value.filterIsInstance<ExtraModifiers.KotlinOnlyModifiers>().toSet()
        } ?: emptyMap()
    }
}
