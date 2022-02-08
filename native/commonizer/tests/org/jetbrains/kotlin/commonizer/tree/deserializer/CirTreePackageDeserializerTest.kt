/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.tree.deserializer

import org.jetbrains.kotlin.commonizer.utils.createCirTree

class CirTreePackageDeserializerTest : AbstractCirTreeDeserializerTest() {

    fun `test package with multiple classes properties and functions`() {
        val module = createCirTree {
            source(
                """
                    typealias rootTypeAlias = Int
                    val rootProperty: String = "hello,"
                    fun rootFunction(): String = "it is me"
                    class RootClass
            """.trimIndent(), "root.kt"
            )

            source(
                """
                    package test.pkg1
                    typealias pkg1TypeAlias = Int
                    val pkg1Property: Int = 42
                    fun pkg1Function(): String = "answer"
                    class Pkg1
            """.trimIndent(), "pkg1.kt"
            )

            source(
                """
                    package test.pkg2
                    typealias pkg2TypeAlias = Int
                    val pkg2Property: Int = 42
                    fun pkg2Function(): String = "answer"
                    class Pkg2
                """.trimIndent(), "pkg2.kt"
            )
        }

        val rootPackage = module.packages.singleOrNull { it.pkg.packageName.isRoot() }
            ?: kotlin.test.fail("Missing root package")

        val pkg1 = module.packages.singleOrNull { it.pkg.packageName.toMetadataString() == "test/pkg1" }
            ?: kotlin.test.fail("Missing pkg1")

        val pkg2 = module.packages.singleOrNull { it.pkg.packageName.toMetadataString() == "test/pkg2" }
            ?: kotlin.test.fail("Missing pkg2")


        rootPackage.run {
            kotlin.test.assertTrue(
                typeAliases.any { it.typeAlias.name.toStrippedString() == "rootTypeAlias" },
                "Expected 'rootTypeAlias'"
            )

            kotlin.test.assertTrue(
                properties.any { it.name.toStrippedString() == "rootProperty" },
                "Expected 'rootProperty'"
            )

            kotlin.test.assertTrue(
                functions.any { it.name.toStrippedString() == "rootFunction" },
                "Expected 'rootFunction'"
            )

            kotlin.test.assertTrue(
                classes.any { it.clazz.name.toStrippedString() == "RootClass" },
                "Expected 'RootClass'"
            )
        }

        pkg1.run {
            kotlin.test.assertTrue(
                typeAliases.any { it.typeAlias.name.toStrippedString() == "pkg1TypeAlias" },
                "Expected 'pkg1TypeAlias'"
            )

            kotlin.test.assertTrue(
                properties.any { it.name.toStrippedString() == "pkg1Property" },
                "Expected 'pkg1Property'"
            )

            kotlin.test.assertTrue(
                functions.any { it.name.toStrippedString() == "pkg1Function" },
                "Expected 'pkg1Function'"
            )

            kotlin.test.assertTrue(
                classes.any { it.clazz.name.toStrippedString() == "Pkg1" },
                "Expected 'Pkg1'"
            )
        }

        pkg2.run {
            kotlin.test.assertTrue(
                typeAliases.any { it.typeAlias.name.toStrippedString() == "pkg2TypeAlias" },
                "Expected 'pkg2TypeAlias'"
            )

            kotlin.test.assertTrue(
                properties.any { it.name.toStrippedString() == "pkg2Property" },
                "Expected 'pkg2Property'"
            )

            kotlin.test.assertTrue(
                functions.any { it.name.toStrippedString() == "pkg2Function" },
                "Expected 'pkg2Function'"
            )

            kotlin.test.assertTrue(
                classes.any { it.clazz.name.toStrippedString() == "Pkg2" },
                "Expected 'Pkg2'"
            )
        }
    }
}
