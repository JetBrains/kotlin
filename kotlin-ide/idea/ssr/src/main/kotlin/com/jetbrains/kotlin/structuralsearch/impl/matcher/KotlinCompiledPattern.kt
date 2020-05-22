package com.jetbrains.kotlin.structuralsearch.impl.matcher

import com.intellij.structuralsearch.impl.matcher.CompiledPattern
import com.jetbrains.kotlin.structuralsearch.impl.matcher.strategies.KotlinMatchingStrategy

class KotlinCompiledPattern : CompiledPattern() {
    init {
        strategy = KotlinMatchingStrategy
    }

    override fun getTypedVarPrefixes(): Array<String> = arrayOf(TYPED_VAR_PREFIX)

    override fun isTypedVar(str: String): Boolean = when {
        str.isEmpty() -> false
        str[0] == '@' -> str.regionMatches(1, TYPED_VAR_PREFIX, 0, TYPED_VAR_PREFIX.length)
        else -> str.startsWith(TYPED_VAR_PREFIX)
    }

    companion object {
        const val TYPED_VAR_PREFIX: String = "_____"
    }
}