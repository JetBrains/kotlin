/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.jvm.java

import org.jetbrains.dokka.analysis.test.api.javaTestProject
import org.jetbrains.dokka.analysis.test.api.parse
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DInterface
import org.jetbrains.dokka.model.JavaModifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JavaModalityTest {

    @Test
    fun `should parse java class`() {
        val project = javaTestProject {
            javaFile("Class.java") {
                +"class Class {}"
            }
        }

        val module = project.parse()
        val pkg = module.packages.single()
        val classlike = pkg.classlikes.single()

        assertEquals("Class", classlike.name)
        assertTrue(classlike is DClass)
        assertEquals(JavaModifier.Empty, classlike.modifier.values.single())
    }

    @Test
    fun `should parse java abstract class`() {
        val project = javaTestProject {
            javaFile("Abstract.java") {
                +"abstract class Abstract {}"
            }
        }

        val module = project.parse()
        val pkg = module.packages.single()
        val classlike = pkg.classlikes.single()

        assertEquals("Abstract", classlike.name)
        assertTrue(classlike is DClass)
        assertEquals(JavaModifier.Abstract, classlike.modifier.values.single())
    }

    @Test
    fun `should parse java final class`() {
        val project = javaTestProject {
            javaFile("Final.java") {
                +"final class Final {}"
            }
        }

        val module = project.parse()
        val pkg = module.packages.single()
        val classlike = pkg.classlikes.single()

        assertEquals("Final", classlike.name)
        assertTrue(classlike is DClass)
        assertEquals(JavaModifier.Final, classlike.modifier.values.single())
    }

    @Test
    fun `should parse java interface`() {
        val project = javaTestProject {
            javaFile("Interface.java") {
                +"interface Interface {}"
            }
        }

        val module = project.parse()
        val pkg = module.packages.single()
        val classlike = pkg.classlikes.single()

        assertEquals("Interface", classlike.name)
        assertTrue(classlike is DInterface)
        assertEquals(JavaModifier.Empty, classlike.modifier.values.single())
    }

    @Test
    fun `should parse abstract java interface`() {
        val project = javaTestProject {
            javaFile("Interface.java") {
                +"abstract interface Interface {}"
            }
        }

        val module = project.parse()
        val pkg = module.packages.single()
        val classlike = pkg.classlikes.single()

        assertEquals("Interface", classlike.name)
        assertTrue(classlike is DInterface)
        assertEquals(JavaModifier.Empty, classlike.modifier.values.single())
    }

}
