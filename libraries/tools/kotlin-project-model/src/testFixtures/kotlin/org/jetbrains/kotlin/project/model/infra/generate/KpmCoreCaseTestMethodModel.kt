/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model.infra.generate

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.generators.model.MethodModel
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File

class KpmCoreCaseTestMethodModel(
    override val name: String, // equals to name of corresponding KpmCoreCase
    internal val pathToTestSourcesRootDir: File,
    internal val pathToTestCase: File,
) : MethodModel {
    object Kind : MethodModel.Kind()

    override val dataString: String
        get() {
            val path = FileUtil.getRelativePath(pathToTestSourcesRootDir, pathToTestCase)!!
            return KtTestUtil.getFilePath(File(path))
        }
    override val tags: List<String>
        get() = emptyList()

    override val kind: MethodModel.Kind
        get() = Kind
}
