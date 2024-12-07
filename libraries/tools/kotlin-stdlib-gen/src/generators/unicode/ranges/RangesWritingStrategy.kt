/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.unicode.ranges

import templates.Backend
import templates.KotlinTarget
import templates.Platform
import java.io.FileWriter

internal sealed class RangesWritingStrategy {
    abstract val indentation: String
    abstract val rangesVisibilityModifier: String
    abstract fun beforeWritingRanges(writer: FileWriter)
    abstract fun afterWritingRanges(writer: FileWriter)
    abstract fun rangeRef(name: String): String

    companion object {
        fun of(target: KotlinTarget, wrapperName: String? = null): RangesWritingStrategy {
            return when (target.platform) {
                Platform.JS -> JsRangesWritingStrategy(wrapperName!!)
                else -> NativeRangesWritingStrategy
            }
        }
    }
}

internal object NativeRangesWritingStrategy : RangesWritingStrategy() {
    override val indentation: String get() = ""
    override val rangesVisibilityModifier: String get() = "private"
    override fun beforeWritingRanges(writer: FileWriter) {}
    override fun afterWritingRanges(writer: FileWriter) {}
    override fun rangeRef(name: String): String = name
}

// see KT-42461, KT-40482
internal class JsRangesWritingStrategy(
    private val wrapperName: String
) : RangesWritingStrategy() {
    override val indentation: String get() = " ".repeat(4)
    override val rangesVisibilityModifier: String get() = "internal"

    override fun beforeWritingRanges(writer: FileWriter) {
        writer.appendLine("private object $wrapperName {")
    }

    override fun afterWritingRanges(writer: FileWriter) {
        writer.appendLine("}")
    }

    override fun rangeRef(name: String): String = "$wrapperName.$name"
}