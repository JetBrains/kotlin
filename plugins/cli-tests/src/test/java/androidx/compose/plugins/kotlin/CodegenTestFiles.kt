/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.plugins.kotlin

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.checkers.utils.CheckerTestUtil
import org.jetbrains.kotlin.psi.KtFile

class CodegenTestFiles private constructor(
    val psiFiles: List<KtFile>
) {

    companion object {
        fun create(ktFiles: List<KtFile>): CodegenTestFiles {
            assert(!ktFiles.isEmpty()) { "List should have at least one file" }
            return CodegenTestFiles(ktFiles)
        }

        fun create(
            fileName: String,
            contentWithDiagnosticMarkup: String,
            project: Project
        ): CodegenTestFiles {
            val content = CheckerTestUtil.parseDiagnosedRanges(
                contentWithDiagnosticMarkup,
                ArrayList(),
                null
            )

            val file = createFile(fileName, content, project)

            return create(listOf(file))
        }
    }
}
