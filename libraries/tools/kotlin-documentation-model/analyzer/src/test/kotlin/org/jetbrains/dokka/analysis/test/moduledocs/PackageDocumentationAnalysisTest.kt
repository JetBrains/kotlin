/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.moduledocs

import org.jetbrains.dokka.analysis.test.api.kotlinJvmTestProject
import org.jetbrains.dokka.analysis.test.api.useServices
import org.jetbrains.dokka.model.doc.CodeInline
import org.jetbrains.dokka.model.doc.Description
import org.jetbrains.dokka.model.doc.P
import org.jetbrains.dokka.model.doc.Text
import kotlin.test.Test
import kotlin.test.assertEquals

class PackageDocumentationAnalysisTest {

    @Test
    fun `should parse include description for a nested package in kotlin-jvm`() {
        val testProject = kotlinJvmTestProject {
            dokkaConfiguration {
                kotlinSourceSet {
                    includes = setOf("/documentation/cool-package-description.md")
                }
            }

            ktFile(pathFromSrc = "org/jetbrains/dokka/pckg/docs/test/TestFile.kt") {
                +"class TestFile {}"
            }

            mdFile(pathFromProjectRoot = "/documentation/cool-package-description.md") {
                +"""
                    # Package org.jetbrains.dokka.pckg.docs.test
                    
                    This is my test description for the package `org.jetbrains.dokka.pckg.docs.test`,
                    which contains only one file named TestFile.kt. It has one empty class.
                """
            }
        }

        testProject.useServices { context ->
            val pckg = context.module.packages.single { it.name == "org.jetbrains.dokka.pckg.docs.test" }

            val allPackageDocs = moduleAndPackageDocumentationReader.read(pckg)
            assertEquals(1, allPackageDocs.size)

            val sourceSetPackageDocs = allPackageDocs.entries.single().value
            assertEquals(1, sourceSetPackageDocs.children.size)

            val descriptionTag = sourceSetPackageDocs.children.single() as Description
            assertEquals(1, descriptionTag.children.size)

            val paragraphTag = descriptionTag.children.single() as P
            assertEquals(3, paragraphTag.children.size)

            val expectedParagraphChildren = listOf(
                Text("This is my test description for the package "),
                CodeInline(children = listOf(Text(
                    "org.jetbrains.dokka.pckg.docs.test"
                ))),
                Text(", which contains only one file named TestFile.kt. It has one empty class.")
            )
            assertEquals(expectedParagraphChildren, paragraphTag.children)
        }
    }
}
