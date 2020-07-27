package com.jetbrains.kotlin.structuralsearch

import com.intellij.openapi.project.Project
import com.intellij.structuralsearch.StructuralReplaceHandler
import com.intellij.structuralsearch.impl.matcher.MatcherImplUtil
import com.intellij.structuralsearch.impl.matcher.PatternTreeContext
import com.intellij.structuralsearch.plugin.replace.ReplaceOptions
import com.intellij.structuralsearch.plugin.replace.ReplacementInfo

class KotlinReplaceHandler(private val project: Project) : StructuralReplaceHandler() {
    override fun replace(info: ReplacementInfo, options: ReplaceOptions) {
        val replaceTree = MatcherImplUtil.createTreeFromText(
            info.replacement, PatternTreeContext.Block, options.matchOptions.fileType, project
        ).first()

        // TODO add KT PSI specific replacements here

        (0 until info.matchesCount).mapNotNull(info::getMatch).forEach { it.replace(replaceTree) }
    }
}