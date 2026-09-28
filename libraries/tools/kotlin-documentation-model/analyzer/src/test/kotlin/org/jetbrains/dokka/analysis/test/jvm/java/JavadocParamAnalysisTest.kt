/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.jvm.java

import org.jetbrains.dokka.analysis.test.api.javaTestProject
import org.jetbrains.dokka.analysis.test.api.parse
import org.jetbrains.dokka.model.SourceSetDependent
import org.jetbrains.dokka.model.doc.*
import kotlin.test.Test
import kotlin.test.assertEquals

class JavadocParamAnalysisTest {

    @Test
    fun `should parse javadoc @param for type parameter on class`() {
        val testProject = javaTestProject {
            javaFile(pathFromSrc = "Foo.java") {
                +"""
                    /**
                     * class
                     *
                     * @param <Bar> type parameter,
                     *  long type parameter description
                     */
                    public class Foo<Bar> {}
                """
            }
        }

        val module = testProject.parse()
        val pkg = module.packages.single()
        val cls = pkg.classlikes.single()

        assertDocumentationNodeContains(
            cls.documentation,
            listOf(
                Description(customDocTag("class")),
                Param(customDocTag("type parameter, long type parameter description"), "<Bar>"),
            )
        )
    }

    @Test
    fun `should parse javadoc @param for type parameter on function`() {
        val testProject = javaTestProject {
            javaFile(pathFromSrc = "Foo.java") {
                +"""
                    public class Foo {
                        /**
                         * function
                         *
                         * @param <Bar> type parameter,
                         *  long type parameter description
                         */
                        public <Bar> void something(Bar bar) {}
                    }
                """
            }
        }

        val module = testProject.parse()
        val pkg = module.packages.single()
        val cls = pkg.classlikes.single()
        val function = cls.functions.single { it.name == "something" }

        assertDocumentationNodeContains(
            function.documentation,
            listOf(
                Description(customDocTag("function")),
                Param(customDocTag("type parameter, long type parameter description"), "<Bar>"),
            )
        )
    }

    @Test
    fun `should parse javadoc @param for parameter on function`() {
        val testProject = javaTestProject {
            javaFile(pathFromSrc = "Foo.java") {
                +"""
                    public class Foo {
                        /**
                         * function
                         *
                         * @param bar parameter,
                         *  long parameter description
                         */
                        public void something(String bar) {}
                    }
                """
            }
        }

        val module = testProject.parse()
        val pkg = module.packages.single()
        val cls = pkg.classlikes.single()
        val function = cls.functions.single { it.name == "something" }

        assertDocumentationNodeContains(
            function.documentation,
            listOf(
                Description(customDocTag("function")),
                Param(customDocTag("parameter, long parameter description"), "bar"),
            )
        )
    }

    // this test just freezes current behavior - correct way to annotate type parameter is `<Bar>` not `Bar`
    @Test
    fun `should parse javadoc @param for type parameter without angle brackets on function`() {
        val testProject = javaTestProject {
            javaFile(pathFromSrc = "Foo.java") {
                +"""
                    public class Foo {
                        /**
                         * function
                         *
                         * @param Bar type parameter,
                         *  long type parameter description
                         */
                        public <Bar> void something(Bar bar) {}
                    }
                """
            }
        }

        val module = testProject.parse()
        val pkg = module.packages.single()
        val cls = pkg.classlikes.single()
        val function = cls.functions.single { it.name == "something" }

        assertDocumentationNodeContains(
            function.documentation,
            listOf(
                Description(customDocTag("function")),
                Param(customDocTag("type parameter, long type parameter description"), "Bar"),
            )
        )
    }


    private fun customDocTag(text: String): CustomDocTag {
        return CustomDocTag(listOf(P(listOf(Text(text)))), name = "MARKDOWN_FILE")
    }

    private fun assertDocumentationNodeContains(
        node: SourceSetDependent<DocumentationNode>,
        expected: List<TagWrapper>
    ) {
        assertEquals(
            DocumentationNode(expected),
            node.values.single()
        )
    }
}
