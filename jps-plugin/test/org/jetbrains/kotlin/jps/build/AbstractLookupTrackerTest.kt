/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.jps.build

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.ScopeKind
import org.jetbrains.kotlin.test.JetTestUtils
import org.jetbrains.kotlin.utils.join
import java.io.File
import java.util.*

private val DECLARATION_KEYWORDS = listOf("interface", "class", "enum class", "object", "fun", "val", "var")

abstract class AbstractLookupTrackerTest : AbstractIncrementalJpsTest(
        allowNoFilesWithSuffixInTestData = true,
        allowNoBuildLogFileInTestData = true
) {
    // ignore KDoc like comments which starts with `/**`, example: /** text */
    val COMMENT_WITH_LOOKUP_INFO = "/\\*[^*]+\\*/".toRegex()

    override fun createLookupTracker(): LookupTracker = TestLookupTracker()

    override fun checkLookups(modifications: List<Modification>, lookupTracker: LookupTracker) {
        if (lookupTracker !is TestLookupTracker) throw AssertionError("Expected TestLookupTracker, but: ${lookupTracker.javaClass}")

        val fileToLookups = lookupTracker.lookups.groupBy { it.lookupContainingFile }
        val workSrcDir = File(workDir, "src")

        fun checkLookupsInFile(expectedFile: File, actualFile: File) {

            val independentFilePath = FileUtil.toSystemIndependentName(actualFile.path)
            val lookupsFromFile = fileToLookups[independentFilePath] ?: return

            val text = actualFile.readText()

            val matchResult = COMMENT_WITH_LOOKUP_INFO.find(text)
            if (matchResult != null) {
                fail("File $actualFile unexpectedly contains multiline comments. In range ${matchResult.range} found: ${matchResult.value} in $text")
            }

            val lines = text.lines().toArrayList()

            for ((line, lookupsFromLine) in lookupsFromFile.groupBy { it.lookupLine!! }) {
                val columnToLookups = lookupsFromLine.groupBy { it.lookupColumn!! }.toList().sortedBy { it.first }

                val lineContent = lines[line - 1]
                val parts = ArrayList<CharSequence>(columnToLookups.size * 2)

                var start = 0

                for ((column, lookupsFromColumn) in columnToLookups) {
                    val end = column - 1
                    parts.add(lineContent.subSequence(start, end))

                    val lookups = lookupsFromColumn.distinct().joinToString(separator = " ", prefix = "/*", postfix = "*/") {
                        val rest = lineContent.substring(end)

                        val name =
                                when {
                                    rest.startsWith(it.name) || // same name
                                    rest.startsWith("$" + it.name) || // backing field
                                    DECLARATION_KEYWORDS.any { w -> rest.startsWith(w) } // it's declaration
                                         -> ""
                                    else -> "(" + it.name + ")"
                                }

                        it.scopeKind.toString()[0].toLowerCase().toString() + ":" + it.scopeFqName.let { if (it.isNotEmpty()) it else "<root>" } + name
                    }

                    parts.add(lookups)

                    start = end
                }

                lines[line - 1] = parts.join("") + lineContent.subSequence(start, lineContent.length)
            }

            val actual = lines.joinToString("\n")

            JetTestUtils.assertEqualsToFile(expectedFile, actual)
        }

        if (modifications.isNotEmpty()) {
            for (modification in modifications) {
                if (modification !is ModifyContent) continue

                val expectedFile = modification.dataFile
                val actualFile = File(workDir, modification.path)

                checkLookupsInFile(expectedFile, actualFile)
            }
        }
        else {
            for (actualFile in workSrcDir.walkTopDown()) {
                if (!actualFile.isFile) continue

                val independentFilePath = FileUtil.toSystemIndependentName(actualFile.path)
                val expectedFile = File(testDataDir, independentFilePath.replace(".*/src/".toRegex(), ""))

                checkLookupsInFile(expectedFile, actualFile)
            }
        }
    }

    override fun preProcessSources(srcDir: File) = dropBlockComments(srcDir)

    private fun dropBlockComments(workSrcDir: File) {
        for (file in workSrcDir.walkTopDown()) {
            if (!file.isFile) continue

            val original = file.readText()
            val modified = original.replace(COMMENT_WITH_LOOKUP_INFO, "")

            if (original != modified) {
                file.writeText(modified)
            }
        }
    }
}

private data class LookupInfo(
        val lookupContainingFile: String,
        val lookupLine: Int?,
        val lookupColumn: Int?,
        val scopeFqName: String,
        val scopeKind: ScopeKind,
        val name: String
)

private class TestLookupTracker : LookupTracker {
    val lookups = arrayListOf<LookupInfo>()

    override val requiresLookupLineAndColumn: Boolean
        get() = true

    override fun record(
            lookupContainingFile: String, lookupLine: Int?, lookupColumn: Int?,
            scopeFqName: String, scopeKind: ScopeKind, name: String
    ) {
        lookups.add(LookupInfo(lookupContainingFile, lookupLine, lookupColumn, scopeFqName, scopeKind, name))
    }
}


