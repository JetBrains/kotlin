package org.jetbrains.uast.test.kotlin

import junit.framework.TestCase
import org.jetbrains.uast.*
import org.junit.Test


class KotlinUastCommentOwnersTest : AbstractKotlinUastTest() {

    override fun check(testName: String, file: UFile) {
        with(file) {
            // check file comments
            file.ensureHasComments("/** file */", "// file", "/* file */")

            // check class level declarations comments
            classes.single { it.javaPsi.nameIdentifier?.text == "TopLevelClass" }.ensureHasComments("/** TopLevelClass */").apply {
                fields.single().ensureHasComments("/** classLevelProperty */")
                methods.single { it.isConstructor && it.uastParameters.size == 1 }.ensureHasComments("/** secondaryConstructor */")
                methods.single { it.name == "classLevelMethod" }.ensureHasComments("/** classLevelMethod */")
                innerClasses.single().ensureHasComments("/** NestedClass */")
            }

            // check top level declarations comments
            classes.single { it.javaPsi.name == "CommentOwnersKt" }.apply {
                fields.single().ensureHasComments("/** topLevelProperty */")
                methods.single { it.name == "topLevelFun" }.ensureHasComments("/** topLevelFun */")

                methods.single { it.name == "func" }.apply {
                    // check fun declaration parameters comments
                    uastParameters.single().ensureHasComments("/* fun param before */", "/* fun param after */")

                    // check expressions comments
                    (uastBody as UBlockExpression).apply {
                        ensureHasComments(
                            "// funPlainCall",
                            "/* funNamedArgumentsCall */",
                            "// cycle",
                            "// if",
                            "// localValueDefinition"
                        )

                        expressions.apply {
                            listOf<UCallExpression>(
                                singleIsInstance { it.methodName == "funPlainCall" },
                                singleIsInstance { it.methodName == "funNamedArgumentsCall" }
                            ).forEach { it.valueArguments.single().ensureHasComments("/* call arg before */", "/* call arg after */") }
                        }
                    }
                }
            }

            // check enum values comments
            classes.single { it.javaPsi.nameIdentifier?.text == "MyBooleanEnum" }.ensureHasComments("/** enum */").apply {
                fields.asIterable().apply {
                    singleIsInstance<UEnumConstantEx> { it.javaPsi.nameIdentifier.text == "TRUE" }.ensureHasComments("/** enum true value */")
                    singleIsInstance<UEnumConstantEx> { it.javaPsi.nameIdentifier.text == "FALSE" }.ensureHasComments("/** enum false value */")
                }
            }
        }
    }

    @Test
    fun testCommentOwners() = doTest("CommentOwners")
}

private inline fun <reified T> Iterable<*>.singleIsInstance(predicate: (T) -> Boolean): T = filterIsInstance<T>().single(predicate)

private inline fun <reified U : UElement> U.ensureHasComments(vararg comments: String): U =
    also { TestCase.assertEquals(comments.toSet(), this.comments.mapTo(mutableSetOf()) { it.text.trim() }) }