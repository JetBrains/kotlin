/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.preprocessors

import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.services.ReversibleSourceFilePreprocessor
import org.jetbrains.kotlin.test.services.TestServices
import java.util.regex.Pattern
import java.util.regex.Pattern.MULTILINE
import java.util.regex.Pattern.compile

/**
 * A preprocessor that normalizes single line standard comments (`//`) into configuration style comments (`#`).
 * This transformation is reversible and operates on files with the `.config` extension.
 */
class ConfigCommentTransformerPreprocessor(testServices: TestServices) : ReversibleSourceFilePreprocessor(testServices) {
    companion object {
        private const val LOMBOK_CONFIG_FILE: String = "lombok.config"
        private const val LEADING_SPACES_GROUP_NAME: String = "leadingspaces"
        private const val COMMENT_CONTENT_GROUP_NAME: String = "commentcontent"

        private val standardCommentPattern: Pattern = compile(
            """^(?<${LEADING_SPACES_GROUP_NAME}>\s*)//(?<${COMMENT_CONTENT_GROUP_NAME}>.*)$""",
            MULTILINE
        )
    }

    override fun process(file: TestFile, content: String): String {
        if (!file.checkFileName()) return content

        var previousIndex = 0
        val matcher = standardCommentPattern.matcher(content)
        return buildString {
            while (matcher.find()) {
                append(content.subSequence(previousIndex, matcher.start()))
                append(matcher.group(LEADING_SPACES_GROUP_NAME))
                append('#')
                append(matcher.group(COMMENT_CONTENT_GROUP_NAME))

                previousIndex = matcher.end()
            }
            append(content.subSequence(previousIndex, content.length))
        }
    }

    override fun revert(file: TestFile, actualContent: String): String {
        if (!file.checkFileName()) return actualContent
        // Return just the original content because it's not supposed to be changed.
        return file.originalContent.trim()
    }

    private fun TestFile.checkFileName(): Boolean {
        return name == LOMBOK_CONFIG_FILE
    }
}