package org.jetbrains.kotlin.idea.structuralsearch

import com.intellij.structuralsearch.PatternContext
import com.intellij.structuralsearch.PredefinedConfigurationUtil.createConfiguration
import com.intellij.structuralsearch.plugin.ui.Configuration
import org.jetbrains.kotlin.idea.structuralsearch.filters.OneStateFilter
import org.jetbrains.kotlin.idea.structuralsearch.filters.ValOnlyFilter
import org.jetbrains.kotlin.idea.structuralsearch.filters.VarOnlyFilter
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.idea.KotlinFileType

object KotlinPredefinedConfigurations {
    private val CLASS_TYPE = KSSRBundle.message("category.class")
    private val EXPRESSION_TYPE = KSSRBundle.message("category.expressions")
    private val FUNCTION_TYPE = KSSRBundle.message("category.functions")
    private val OPERATOR_TYPE = KSSRBundle.message("category.operators")
    private val COMMENT_TYPE = KSSRBundle.message("category.comments")
    private val INTERESTING_TYPE = KSSRBundle.message("category.interesting")

    private fun searchTemplate(
        @Nls name: String,
        @NonNls refName: String,
        @NonNls pattern: String,
        @Nls category: String,
        context: PatternContext = KotlinStructuralSearchProfile.DEFAULT_CONTEXT
    ) = createConfiguration(name, refName, pattern, category, KotlinFileType.INSTANCE, context)

    fun createPredefinedTemplates(): Array<Configuration> = arrayOf(
        // Classes
        searchTemplate(
            KSSRBundle.message("predefined.configuration.all.vars.of.the.class"),
            "all vars/vals of a class",
            """
                class '_Class {  
                    var 'Field+ = '_Init?
                }
            """.trimIndent(),
            CLASS_TYPE
        ),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.all.methods.of.the.class"),
            "all methods of a class",
            """
                class '_Class {  
                    fun 'Method+ ('_Parameter* : '_ParameterType): '_ReturnType
                }
            """.trimIndent(),
            CLASS_TYPE
        ),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.all.vars.of.the.object"),
            "all vars/vals of an object or companion object",
            """
                object '_Object {  
                    var 'Field+ = '_Init?
                }
            """.trimIndent(),
            CLASS_TYPE
        ),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.anonymous.class"),
            "anonymous class",
            "fun '_Function() = object { }", CLASS_TYPE
        ),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.class.annotation"),
            "annotated classes",
            """
                @'_Annotation class 'Name
            """.trimIndent(),
            CLASS_TYPE
        ),

        // Expressions
        searchTemplate(
            KSSRBundle.message("predefined.configuration.assignments"),
            "assignments",
            "'_Inst = '_Expr",
            EXPRESSION_TYPE
        ),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.method.calls"),
            "method calls",
            "'_Before?.'MethodCall('_Parameter*)",
            EXPRESSION_TYPE
        ),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.string.literals"),
            "string literals", "\"'_String\"", EXPRESSION_TYPE
        ),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.array.access"),
            "array access", "'_Array['_Index]", EXPRESSION_TYPE
        ),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.casts"),
            "casts", "'_Expr as '_Type", EXPRESSION_TYPE
        ),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.instance"),
            "instances", "'_Expr is '_Type", EXPRESSION_TYPE
        ),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.elvis"),
            "elvis operators", "'_Expr ?: '_Fallback", EXPRESSION_TYPE
        ),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.safe.call.operator"),
            "safe call operators",
            "\$Expr\$?.'_Property",
            EXPRESSION_TYPE
        ),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.assert.not.null"),
            "not-null assertion operators",
            "'_Expr!!",
            EXPRESSION_TYPE
        ),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.lambda"),
            "lambda expressions",
            "{ '_Parameter* -> '_Expr* }",
            EXPRESSION_TYPE
        ),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.strings"),
            "strings",
            """ "$$'_Entry*" """,
            EXPRESSION_TYPE
        ),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.strings.with.long.template"),
            "strings containing a long template",
            """ "$$'_EntryBefore*${'$'}{ '_LongTemplateExpr }$$'_EntryAfter*" """,
            EXPRESSION_TYPE
        ),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.vars.only"),
            "vars only",
            """var '_Variable:[_${VarOnlyFilter.CONSTRAINT_NAME}(${OneStateFilter.ENABLED})]""",
            EXPRESSION_TYPE
        ),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.vals.only"),
            "vals only",
            """val '_Value:[_${ValOnlyFilter.CONSTRAINT_NAME}(${OneStateFilter.ENABLED})]""",
            EXPRESSION_TYPE
        ),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.vars.of.given.type"),
            "vars and vals of given type",
            """var '_Variable:[exprtype(Int)] = '_Init""",
            EXPRESSION_TYPE
        ),

        // Methods
        searchTemplate(
            KSSRBundle.message("predefined.configuration.function.signature"),
            "function signature",
            "fun '_Name('_Param*) : '_Type",
            FUNCTION_TYPE
        ),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.function.annotation"),
            "annotated functions",
            "@'_Annotation fun 'Name('_Param*)",
            FUNCTION_TYPE
        ),

        // Comments, KDoc and Metadata
        searchTemplate(
            KSSRBundle.message("predefined.configuration.comments.containing.word"),
            "comments containing a given word",
            "// '_before bug '_after",
            COMMENT_TYPE
        ),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.kdoc.tag"),
            "KDoc tags",
            """
                /**
                 * @'_Tag '_Text
                 */
            """.trimIndent(),
            COMMENT_TYPE
        ),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.annotations"),
            "annotations",
            "@'Annotation",
            COMMENT_TYPE
        ),

        // Operators
        searchTemplate(
            KSSRBundle.message("predefined.configuration.trys"),
            "try's",
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
            "if's",
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
            "when expressions",
            """
                when ('_Argument?) {
                  '_Condition -> '_Expression
                }
            """.trimIndent(),
            OPERATOR_TYPE
        ),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.for"),
            "for loops",
            """
                for ('_Item in '_Collection) {
                  '_Statement*
                }
            """.trimIndent(),
            OPERATOR_TYPE
        ),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.while"),
            "while loops",
            """
                while ('_Condition) {
                  '_Statement*
                }
            """.trimIndent(),
            OPERATOR_TYPE
        ),
        searchTemplate(
            KSSRBundle.message("predefined.configuration.do.while"),
            "do...while loops",
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
            "Properties with explicit getter",
            "var '_Inst = '_Expr\n\tget() = '_Getter",
            INTERESTING_TYPE,
            KotlinStructuralSearchProfile.PROPERTY_CONTEXT
        )
    )
}