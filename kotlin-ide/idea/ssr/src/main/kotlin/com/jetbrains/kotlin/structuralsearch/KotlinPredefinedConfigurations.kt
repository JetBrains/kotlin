package com.jetbrains.kotlin.structuralsearch

import com.intellij.structuralsearch.PredefinedConfigurationUtil.createSearchTemplateInfo
import com.intellij.structuralsearch.plugin.ui.Configuration
import org.jetbrains.kotlin.idea.KotlinFileType

object KotlinPredefinedConfigurations {

    private val EXPRESSION_TYPE = KSSRBundle.message("expressions.category")
    private val OPERATOR_TYPE = KSSRBundle.message("operators.category")

    private fun searchTemplate(name: String, pattern: String, category: String) =
        createSearchTemplateInfo(name, pattern, category, KotlinFileType.INSTANCE)

    fun createPredefinedTemplates(): Array<Configuration> = arrayOf(

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
        )

    )

}