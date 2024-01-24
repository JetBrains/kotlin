/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.documentable

import org.jetbrains.dokka.analysis.test.api.kotlinJvmTestProject
import org.jetbrains.dokka.analysis.test.api.parse
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DInterface
import org.jetbrains.dokka.model.KotlinModifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KotlinModalityTest {

    @Test
    fun `should parse kotlin sealed class`() {
        val project = kotlinJvmTestProject {
            ktFile("Sealed.kt") {
                +"sealed class Sealed"
            }
        }

        val module = project.parse()
        val pkg = module.packages.single()
        val classlike = pkg.classlikes.single()

        assertEquals("Sealed", classlike.name)
        assertTrue(classlike is DClass)
        assertEquals(KotlinModifier.Sealed, classlike.modifier.values.single())
    }

    @Test
    fun `should parse kotlin abstract class`() {
        val project = kotlinJvmTestProject {
            ktFile("Abstract.kt") {
                +"abstract class Abstract"
            }
        }

        val module = project.parse()
        val pkg = module.packages.single()
        val classlike = pkg.classlikes.single()

        assertEquals("Abstract", classlike.name)
        assertTrue(classlike is DClass)
        assertEquals(KotlinModifier.Abstract, classlike.modifier.values.single())
    }

    @Test
    fun `should parse kotlin open class`() {
        val project = kotlinJvmTestProject {
            ktFile("Open.kt") {
                +"open class Open"
            }
        }

        val module = project.parse()
        val pkg = module.packages.single()
        val classlike = pkg.classlikes.single()

        assertEquals("Open", classlike.name)
        assertTrue(classlike is DClass)
        assertEquals(KotlinModifier.Open, classlike.modifier.values.single())
    }

    @Test
    fun `should parse kotlin class`() {
        val project = kotlinJvmTestProject {
            ktFile("Class.kt") {
                +"class Class"
            }
        }

        val module = project.parse()
        val pkg = module.packages.single()
        val classlike = pkg.classlikes.single()

        assertEquals("Class", classlike.name)
        assertTrue(classlike is DClass)
        assertEquals(KotlinModifier.Final, classlike.modifier.values.single())
    }

    @Test
    fun `should parse kotlin sealed interface`() {
        val project = kotlinJvmTestProject {
            ktFile("Sealed.kt") {
                +"sealed interface Sealed"
            }
        }

        val module = project.parse()
        val pkg = module.packages.single()
        val classlike = pkg.classlikes.single()

        assertEquals("Sealed", classlike.name)
        assertTrue(classlike is DInterface)
        assertEquals(KotlinModifier.Sealed, classlike.modifier.values.single())
    }

    @Test
    fun `should parse ordinary kotlin interface`() {
        val project = kotlinJvmTestProject {
            ktFile("Interface.kt") {
                +"interface Interface"
            }
        }

        val module = project.parse()
        val pkg = module.packages.single()
        val classlike = pkg.classlikes.single()

        assertEquals("Interface", classlike.name)
        assertTrue(classlike is DInterface)
        assertEquals(KotlinModifier.Empty, classlike.modifier.values.single())
    }

    @Test
    fun `should parse abstract kotlin interface`() {
        val project = kotlinJvmTestProject {
            ktFile("Interface.kt") {
                +"abstract interface Interface"
            }
        }

        val module = project.parse()
        val pkg = module.packages.single()
        val classlike = pkg.classlikes.single()

        assertEquals("Interface", classlike.name)
        assertTrue(classlike is DInterface)
        assertEquals(KotlinModifier.Empty, classlike.modifier.values.single())
    }

}
