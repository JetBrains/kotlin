package com.jetbrains.kotlin.structuralsearch

import com.intellij.structuralsearch.PatternContext
import com.intellij.structuralsearch.PredefinedConfigurationUtil.createSearchTemplateInfo
import com.intellij.structuralsearch.plugin.ui.Configuration
import org.jetbrains.kotlin.idea.KotlinFileType

object KotlinPredefinedConfigurations {
    private val CLASS_TYPE = KSSRBundle.message("category.class")
    private val EXPRESSION_TYPE = KSSRBundle.message("category.expressions")
    private val FUNCTION_TYPE = KSSRBundle.message("category.functions")
    private val OPERATOR_TYPE = KSSRBundle.message("category.operators")
    private val COMMENT_TYPE = KSSRBundle.message("category.comments")
    private val INTERESTING_TYPE = KSSRBundle.message("category.interesting")

    private fun searchTemplate(
        name: String,
        pattern: String,
        category: String,
        context: PatternContext = KotlinStructuralSearchProfile.DEFAULT_CONTEXT
    ) = createSearchTemplateInfo(name, pattern, category, KotlinFileType.INSTANCE, context)

    fun createPredefinedTemplates(): Array<Configuration> = arrayOf(
        // Classes
        searchTemplate(
            KSSRBundle.message("predefined.configuration.all.vars.of.the.class"),
            """
                class '_Class {  
                    var 'Field+ = '_Init?
                }
            """.trimIndent(),
            CLASS_TYPE
        ),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.all.methods.of.the.class"),
            """
                class '_Class {  
                    fun 'Method+ ('_Parameter* : '_ParameterType): '_ReturnType
                }
            """.trimIndent(),
            CLASS_TYPE
        ),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.all.vars.of.the.object"),
            """
                object '_Object {  
                    var 'Field+ = '_Init?
                }
            """.trimIndent(),
            CLASS_TYPE
        ),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.anonymous.class"), "fun '_Function() = object { }", CLASS_TYPE
        ),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.class.annotation"),
            """
                @'_Annotation class 'Name
            """.trimIndent(),
            CLASS_TYPE
        ),

        // Expressions
        searchTemplate(KSSRBundle.message("predefined.configuration.assignments"), "'_Inst = '_Expr", EXPRESSION_TYPE),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.method.calls"),
            "'_Before?.'MethodCall('_Parameter*)",
            EXPRESSION_TYPE
        ),
        searchTemplate(KSSRBundle.message("predefined.configuration.string.literals"), "\"'_String\"", EXPRESSION_TYPE),
        searchTemplate(KSSRBundle.message("predefined.configuration.array.access"), "'_Array['_Index]", EXPRESSION_TYPE),
        searchTemplate(KSSRBundle.message("predefined.configuration.casts"), "'_Expr as '_Type", EXPRESSION_TYPE),
        searchTemplate(KSSRBundle.message("predefined.configuration.instance"), "'_Expr is '_Type", EXPRESSION_TYPE),
        searchTemplate(KSSRBundle.message("predefined.configuration.elvis"), "'_Expr ?: '_Fallback", EXPRESSION_TYPE),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.safe.call.operator"),
            "\$Expr\$?.'_Property",
            EXPRESSION_TYPE
        ),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.assert.not.null"),
            "'_Expr!!",
            EXPRESSION_TYPE
        ),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.lambda"),
            "{ '_Parameter* -> '_Expr* }",
            EXPRESSION_TYPE
        ),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.strings"),
            """ "$$'_Entry*" """,
            EXPRESSION_TYPE
        ),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.strings.with.long.template"),
            """ "$$'_EntryBefore*${'$'}{ '_LongTemplateExpr }$$'_EntryAfter*" """,
            EXPRESSION_TYPE
        ),

        // Methods
        searchTemplate(
            KSSRBundle.message("predefined.configuration.function.signature"),
            "fun '_Name('_Param*) : '_Type",
            FUNCTION_TYPE
        ),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.function.annotation"),
            "@'_Annotation fun 'Name('_Param*)",
            FUNCTION_TYPE
        ),

        // Comments, KDoc and Metadata
        searchTemplate(
            KSSRBundle.message("predefined.configuration.comments.containing.word"),
            "// '_before bug '_after".trimIndent(),
            COMMENT_TYPE
        ),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.kdoc.tag"),
            """
                /**
                 * @'_Tag '_Text
                 */
            """.trimIndent(),
            COMMENT_TYPE
        ),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.annotations"),
            "@'Annotation",
            COMMENT_TYPE
        ),

        // Operators
        searchTemplate(
            KSSRBundle.message("predefined.configuration.trys"),
            """
                try {
                    '_TryStatement+
                } catch('_Exception : '_ExceptionType) {
                    '_CatchStatement*
                }
            """.trimIndent(),
            OPERATOR_TYPE
        ),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.ifs"),
            """
                if ('_Condition) {
                  '_ThenStatement*
                } else {
                  '_ElseStatement*
                }
            """.trimIndent(),
            OPERATOR_TYPE
        ),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.when"),
            """
                when ('_Argument) {
                  '_Condition -> '_Expression
                }
            """.trimIndent(),
            OPERATOR_TYPE
        ),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.for"),
            """
                for ('_Item in '_Collection) {
                  '_Statement*
                }
            """.trimIndent(),
            OPERATOR_TYPE
        ),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.while"),
            """
                while ('_Condition) {
                  '_Statement*
                }
            """.trimIndent(),
            OPERATOR_TYPE
        ),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.do.while"),
            """
                do {
                  '_Statement*
                } while ('_Condition)
            """.trimIndent(),
            OPERATOR_TYPE
        ),

        // Interesting
        searchTemplate(
            KSSRBundle.message("predefined.configuration.properties.getter"),
            "var '_Inst = '_Expr\n\tget() = '_Getter",
            INTERESTING_TYPE,
            KotlinStructuralSearchProfile.PROPERTY_CONTEXT
        )
    )
}