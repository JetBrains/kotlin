package com.jetbrains.kotlin.structuralsearch

import com.intellij.structuralsearch.PredefinedConfigurationUtil.createSearchTemplateInfo
import com.intellij.structuralsearch.plugin.ui.Configuration
import org.jetbrains.kotlin.idea.KotlinFileType

object KotlinPredefinedConfigurations {

    private val EXPRESSION_TYPE = KSSRBundle.message("expressions.category")

    private fun searchTemplate(name: String, pattern: String, category: String) =
        createSearchTemplateInfo(name, pattern, category, KotlinFileType.INSTANCE)

    fun createPredefinedTemplates(): Array<Configuration> = arrayOf(
        searchTemplate(KSSRBundle.message("predefined.configuration.assignments"), "'_Inst = '_Expr", EXPRESSION_TYPE),
        searchTemplate(KSSRBundle.message("predefined.configuration.method.calls"), "'_Before?.'MethodCall('_Parameter*)", EXPRESSION_TYPE),
        searchTemplate(KSSRBundle.message("predefined.configuration.string.literals"), "\"'_String\"", EXPRESSION_TYPE),
        searchTemplate(KSSRBundle.message("predefined.configuration.array.access"), "'_Array['_Index]", EXPRESSION_TYPE),
        searchTemplate(KSSRBundle.message("predefined.configuration.casts"), "'_Expr as '_Type", EXPRESSION_TYPE),
        searchTemplate(KSSRBundle.message("predefined.configuration.instance"), "'_Expr is '_Type", EXPRESSION_TYPE),
        searchTemplate(KSSRBundle.message("predefined.configuration.elvis"), "'_Expr ?: '_Fallback", EXPRESSION_TYPE)
    )

}