/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter

import com.intellij.application.options.CodeStyleAbstractConfigurable
import com.intellij.application.options.CodeStyleAbstractPanel
import com.intellij.application.options.IndentOptionsEditor
import com.intellij.application.options.SmartIndentOptionsEditor
import com.intellij.application.options.codeStyle.properties.CodeStyleFieldAccessor
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationBundle
import com.intellij.psi.codeStyle.*
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings
import java.lang.reflect.Field
import kotlin.reflect.KProperty

class KotlinLanguageCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {
    override fun getLanguage(): Language = KotlinLanguage.INSTANCE
    override fun getConfigurableDisplayName(): String = KotlinBundle.message("codestyle.name.kotlin")
    override fun createConfigurable(settings: CodeStyleSettings, modelSettings: CodeStyleSettings): CodeStyleConfigurable =
        object : CodeStyleAbstractConfigurable(settings, modelSettings, KotlinLanguage.NAME) {
            override fun createPanel(settings: CodeStyleSettings): CodeStyleAbstractPanel =
                KotlinCodeStylePanel(currentSettings, settings)

            override fun getHelpTopic(): String = "reference.settingsdialog.codestyle.kotlin"
        }

    override fun getIndentOptionsEditor(): IndentOptionsEditor = SmartIndentOptionsEditor()

    override fun getDefaultCommonSettings(): CommonCodeStyleSettings = KotlinCommonCodeStyleSettings().apply {
        initIndentOptions()
    }

    override fun createCustomSettings(settings: CodeStyleSettings?): CustomCodeStyleSettings = KotlinCodeStyleSettings(settings)

    override fun getAccessor(codeStyleObject: Any, field: Field): CodeStyleFieldAccessor<*, *>? {
        return KotlinCodeStyleFieldAccessor.create(codeStyleObject, field) ?: super.getAccessor(codeStyleObject, field)
    }

    override fun customizeSettings(consumer: CodeStyleSettingsCustomizable, settingsType: SettingsType) {
        fun showCustomOption(field: KProperty<*>, title: String, groupName: String? = null, vararg options: Any) {
            consumer.showCustomOption(KotlinCodeStyleSettings::class.java, field.name, title, groupName, *options)
        }

        when (settingsType) {
            SettingsType.SPACING_SETTINGS -> {
                consumer.showStandardOptions(
                    "SPACE_AROUND_ASSIGNMENT_OPERATORS",
                    "SPACE_AROUND_LOGICAL_OPERATORS",
                    "SPACE_AROUND_EQUALITY_OPERATORS",
                    "SPACE_AROUND_RELATIONAL_OPERATORS",
                    "SPACE_AROUND_ADDITIVE_OPERATORS",
                    "SPACE_AROUND_MULTIPLICATIVE_OPERATORS",
                    "SPACE_AROUND_UNARY_OPERATOR",
                    "SPACE_AFTER_COMMA",
                    "SPACE_BEFORE_COMMA",
                    "SPACE_BEFORE_IF_PARENTHESES",
                    "SPACE_BEFORE_WHILE_PARENTHESES",
                    "SPACE_BEFORE_FOR_PARENTHESES",
                    "SPACE_BEFORE_CATCH_PARENTHESES"
                )

                showCustomOption(
                    KotlinCodeStyleSettings::SPACE_AROUND_RANGE,
                    KotlinBundle.message("formatter.title.range.operator"),
                    CodeStyleSettingsCustomizable.SPACES_AROUND_OPERATORS
                )

                showCustomOption(
                    KotlinCodeStyleSettings::SPACE_BEFORE_TYPE_COLON,
                    KotlinBundle.message("formatter.title.before.colon.after.declaration.name"),
                    CodeStyleSettingsCustomizable.SPACES_OTHER
                )

                showCustomOption(
                    KotlinCodeStyleSettings::SPACE_AFTER_TYPE_COLON,
                    KotlinBundle.message("formatter.title.after.colon.before.declaration.type"),
                    CodeStyleSettingsCustomizable.SPACES_OTHER
                )

                showCustomOption(
                    KotlinCodeStyleSettings::SPACE_BEFORE_EXTEND_COLON,
                    KotlinBundle.message("formatter.title.before.colon.in.new.type.definition"),
                    CodeStyleSettingsCustomizable.SPACES_OTHER
                )

                showCustomOption(
                    KotlinCodeStyleSettings::SPACE_AFTER_EXTEND_COLON,
                    KotlinBundle.message("formatter.title.after.colon.in.new.type.definition"),
                    CodeStyleSettingsCustomizable.SPACES_OTHER
                )

                showCustomOption(
                    KotlinCodeStyleSettings::INSERT_WHITESPACES_IN_SIMPLE_ONE_LINE_METHOD,
                    KotlinBundle.message("formatter.title.in.simple.one.line.methods"),
                    CodeStyleSettingsCustomizable.SPACES_OTHER
                )

                showCustomOption(
                    KotlinCodeStyleSettings::SPACE_AROUND_FUNCTION_TYPE_ARROW,
                    KotlinBundle.message("formatter.title.around.arrow.in.function.types"),
                    CodeStyleSettingsCustomizable.SPACES_OTHER
                )

                showCustomOption(
                    KotlinCodeStyleSettings::SPACE_AROUND_WHEN_ARROW,
                    KotlinBundle.message("formatter.title.around.arrow.in"),
                    CodeStyleSettingsCustomizable.SPACES_OTHER
                )

                showCustomOption(
                    KotlinCodeStyleSettings::SPACE_BEFORE_LAMBDA_ARROW,
                    KotlinBundle.message("formatter.title.before.lambda.arrow"),
                    CodeStyleSettingsCustomizable.SPACES_OTHER
                )

                showCustomOption(
                    KotlinCodeStyleSettings::SPACE_BEFORE_WHEN_PARENTHESES,
                    KotlinBundle.message("formatter.title.when.parentheses"),
                    CodeStyleSettingsCustomizable.SPACES_BEFORE_PARENTHESES
                )
            }
            SettingsType.WRAPPING_AND_BRACES_SETTINGS -> {
                consumer.showStandardOptions(
                    // "ALIGN_MULTILINE_CHAINED_METHODS",
                    "RIGHT_MARGIN",
                    "WRAP_ON_TYPING",
                    "KEEP_FIRST_COLUMN_COMMENT",
                    "KEEP_LINE_BREAKS",
                    "ALIGN_MULTILINE_EXTENDS_LIST",
                    "ALIGN_MULTILINE_PARAMETERS",
                    "ALIGN_MULTILINE_PARAMETERS_IN_CALLS",
                    "ALIGN_MULTILINE_METHOD_BRACKETS",
                    "ALIGN_MULTILINE_BINARY_OPERATION",
                    "ELSE_ON_NEW_LINE",
                    "WHILE_ON_NEW_LINE",
                    "CATCH_ON_NEW_LINE",
                    "FINALLY_ON_NEW_LINE",
                    "CALL_PARAMETERS_WRAP",
                    "METHOD_PARAMETERS_WRAP",
                    "EXTENDS_LIST_WRAP",
                    "METHOD_ANNOTATION_WRAP",
                    "CLASS_ANNOTATION_WRAP",
                    "PARAMETER_ANNOTATION_WRAP",
                    "VARIABLE_ANNOTATION_WRAP",
                    "FIELD_ANNOTATION_WRAP",
                    "METHOD_PARAMETERS_LPAREN_ON_NEXT_LINE",
                    "METHOD_PARAMETERS_RPAREN_ON_NEXT_LINE",
                    "CALL_PARAMETERS_LPAREN_ON_NEXT_LINE",
                    "CALL_PARAMETERS_RPAREN_ON_NEXT_LINE",
                    "ENUM_CONSTANTS_WRAP",
                    "METHOD_CALL_CHAIN_WRAP",
                    "WRAP_FIRST_METHOD_IN_CALL_CHAIN",
                    "ASSIGNMENT_WRAP",
                )

                consumer.renameStandardOption(
                    CodeStyleSettingsCustomizable.WRAPPING_SWITCH_STATEMENT,
                    KotlinBundle.message("formatter.title.when.statements")
                )

                consumer.renameStandardOption("FIELD_ANNOTATION_WRAP", KotlinBundle.message("formatter.title.property.annotations"))
                consumer.renameStandardOption(
                    "METHOD_PARAMETERS_WRAP",
                    KotlinBundle.message("formatter.title.function.declaration.parameters")
                )

                consumer.renameStandardOption("CALL_PARAMETERS_WRAP", KotlinBundle.message("formatter.title.function.call.arguments"))
                consumer.renameStandardOption("METHOD_CALL_CHAIN_WRAP", KotlinBundle.message("formatter.title.chained.function.calls"))
                consumer.renameStandardOption("METHOD_ANNOTATION_WRAP", KotlinBundle.message("formatter.title.function.annotations"))
                consumer.renameStandardOption(
                    CodeStyleSettingsCustomizable.WRAPPING_METHOD_PARENTHESES,
                    KotlinBundle.message("formatter.title.function.parentheses")
                )

                showCustomOption(
                    KotlinCodeStyleSettings::ALIGN_IN_COLUMNS_CASE_BRANCH,
                    KotlinBundle.message("formatter.title.align.when.branches.in.columns"),
                    CodeStyleSettingsCustomizable.WRAPPING_SWITCH_STATEMENT
                )

                showCustomOption(
                    KotlinCodeStyleSettings::LBRACE_ON_NEXT_LINE,
                    KotlinBundle.message("formatter.title.put.left.brace.on.new.line"),
                    CodeStyleSettingsCustomizable.WRAPPING_BRACES
                )

                showCustomOption(
                    KotlinCodeStyleSettings::CONTINUATION_INDENT_IN_PARAMETER_LISTS,
                    KotlinBundle.message("formatter.title.use.continuation.indent"),
                    CodeStyleSettingsCustomizable.WRAPPING_METHOD_PARAMETERS
                )

                showCustomOption(
                    KotlinCodeStyleSettings::CONTINUATION_INDENT_IN_ARGUMENT_LISTS,
                    KotlinBundle.message("formatter.title.use.continuation.indent"),
                    CodeStyleSettingsCustomizable.WRAPPING_METHOD_ARGUMENTS_WRAPPING
                )

                showCustomOption(
                    KotlinCodeStyleSettings::CONTINUATION_INDENT_FOR_CHAINED_CALLS,
                    KotlinBundle.message("formatter.title.use.continuation.indent"),
                    CodeStyleSettingsCustomizable.WRAPPING_CALL_CHAIN
                )

                showCustomOption(
                    KotlinCodeStyleSettings::CONTINUATION_INDENT_IN_SUPERTYPE_LISTS,
                    KotlinBundle.message("formatter.title.use.continuation.indent"),
                    CodeStyleSettingsCustomizable.WRAPPING_EXTENDS_LIST
                )

                showCustomOption(
                    KotlinCodeStyleSettings::WRAP_EXPRESSION_BODY_FUNCTIONS,
                    KotlinBundle.message("formatter.title.expression.body.functions"),
                    options = *arrayOf(
                        CodeStyleSettingsCustomizable.WRAP_OPTIONS_FOR_SINGLETON,
                        CodeStyleSettingsCustomizable.WRAP_VALUES_FOR_SINGLETON
                    )
                )

                showCustomOption(
                    KotlinCodeStyleSettings::CONTINUATION_INDENT_FOR_EXPRESSION_BODIES,
                    KotlinBundle.message("formatter.title.use.continuation.indent"),
                    KotlinBundle.message("formatter.title.expression.body.functions")
                )

                showCustomOption(
                    KotlinCodeStyleSettings::WRAP_ELVIS_EXPRESSIONS,
                    KotlinBundle.message("formatter.title.elvis.expressions"),
                    options = *arrayOf(
                        CodeStyleSettingsCustomizable.WRAP_OPTIONS_FOR_SINGLETON,
                        CodeStyleSettingsCustomizable.WRAP_VALUES_FOR_SINGLETON
                    )
                )
                showCustomOption(
                    KotlinCodeStyleSettings::CONTINUATION_INDENT_IN_ELVIS,
                    title = KotlinBundle.message("formatter.title.use.continuation.indent"),
                    groupName = KotlinBundle.message("formatter.title.elvis.expressions")
                )

                showCustomOption(
                    KotlinCodeStyleSettings::IF_RPAREN_ON_NEW_LINE,
                    ApplicationBundle.message("wrapping.rpar.on.new.line"),
                    CodeStyleSettingsCustomizable.WRAPPING_IF_STATEMENT
                )

                showCustomOption(
                    KotlinCodeStyleSettings::CONTINUATION_INDENT_IN_IF_CONDITIONS,
                    KotlinBundle.message("formatter.title.use.continuation.indent.in.conditions"),
                    CodeStyleSettingsCustomizable.WRAPPING_IF_STATEMENT
                )
            }
            SettingsType.BLANK_LINES_SETTINGS -> {
                consumer.showStandardOptions(
                    "KEEP_BLANK_LINES_IN_CODE",
                    "KEEP_BLANK_LINES_IN_DECLARATIONS",
                    "KEEP_BLANK_LINES_BEFORE_RBRACE",
                    "BLANK_LINES_AFTER_CLASS_HEADER"
                )

                showCustomOption(
                    KotlinCodeStyleSettings::BLANK_LINES_AROUND_BLOCK_WHEN_BRANCHES,
                    KotlinBundle.message("formatter.title.around.when.branches.with"),
                    CodeStyleSettingsCustomizable.BLANK_LINES
                )

                showCustomOption(
                    KotlinCodeStyleSettings::BLANK_LINES_BEFORE_DECLARATION_WITH_COMMENT_OR_ANNOTATION_ON_SEPARATE_LINE,
                    KotlinBundle.message("formatter.title.before.declaration.with.comment.or.annotation"),
                    CodeStyleSettingsCustomizable.BLANK_LINES
                )
            }
            SettingsType.COMMENTER_SETTINGS -> {
                consumer.showAllStandardOptions()
            }
            else -> consumer.showStandardOptions()
        }
    }

    override fun getCodeSample(settingsType: SettingsType): String = when (settingsType) {
        SettingsType.WRAPPING_AND_BRACES_SETTINGS ->
            """
               @Deprecated("Foo") public class ThisIsASampleClass : Comparable<*>, Appendable {
                   val test =
                       12

                   @Deprecated("Foo") fun foo1(i1: Int, i2: Int, i3: Int) : Int {
                       when (i1) {
                           is Number -> 0
                           else -> 1
                       }
                       if (i2 > 0 &&
                               i3 < 0) {
                           return 2
                       }
                       return 0
                   }
                   private fun foo2():Int {
               // todo: something
                       try {            return foo1(12, 13, 14)
                       }        catch (e: Exception) {            return 0        }        finally {           if (true) {               return 1           }           else {               return 2           }        }    }
                   private val f = {a: Int->a*2}

                   fun longMethod(@Named("param1") param1: Int,
                    param2: String) {
                       @Deprecated val foo = 1
                   }

                   fun multilineMethod(
                           foo: String,
                           bar: String?,
                           x: Int?
                       ) {
                       foo.toUpperCase().trim()
                           .length
                       val barLen = bar?.length() ?: x ?: -1
                       if (foo.length > 0 &&
                           barLen > 0) {
                           println("> 0")
                       }
                   }
               }

               @Deprecated val bar = 1

               enum class Enumeration {
                   A, B
               }

               fun veryLongExpressionBodyMethod() = "abc"
            """.trimIndent()

        SettingsType.BLANK_LINES_SETTINGS ->
            """
                class Foo {
                   private var field1: Int = 1
                   private val field2: String? = null


                   init {
                       field1 = 2;
                   }

                   fun foo1() {
                       run {



                           field1
                       }

                       when(field1) {
                           1 -> println("1")
                           2 -> println("2")
                           3 ->
                                println("3" +
                                     "4")
                       }

                       when(field2) {
                           1 -> {
                               println("1")
                           }

                           2 -> {
                               println("2")
                           }
                       }
                   }


                   class InnerClass {
                   }
               }



               class AnotherClass {
               }

               interface TestInterface {
               }
               fun run(f: () -> Unit) {
                   f()
               }
               
               class Bar {
                   @Annotation
                   val a = 42
                   @Annotation
                   val b = 43
                   fun c() {
                       a + b
                   }
                   fun d() = Unit
                   // smth
                   fun e() {
                       d()
                   }
                   fun f() = d()
               }
               """.trimIndent()

        else -> """open class Some {
                       private val f: (Int)->Int = { a: Int -> a * 2 }
                       fun foo(): Int {
                           val test: Int = 12
                           for (i in 10..42) {
                               println (when {
                                   i < test -> -1
                                   i > test -> 1
                                   else -> 0
                               })
                           }
                           if (true) { }
                           while (true) { break }
                           try {
                               when (test) {
                                   12 -> println("foo")
                                   else -> println("bar")
                               }
                           } catch (e: Exception) {
                           } finally {
                           }
                           return test
                       }
                       private fun <T>foo2(): Int where T : List<T> {
                           return 0
                       }

                       fun multilineMethod(
                           foo: String,
                           bar: String
                       ) {
                           foo
                               .length
                       }

                       fun expressionBodyMethod() =
                               "abc"
                   }
                   class AnotherClass<T : Any> : Some()
                   """.trimIndent()
    }
}
