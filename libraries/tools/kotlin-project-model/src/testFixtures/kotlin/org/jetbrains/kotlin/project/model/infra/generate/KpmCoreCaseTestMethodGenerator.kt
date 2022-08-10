/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model.infra.generate

import org.jetbrains.kotlin.generators.MethodGenerator
import org.jetbrains.kotlin.generators.model.MethodModel
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.utils.Printer

object KpmCoreCaseTestMethodGenerator : MethodGenerator<KpmCoreCaseTestMethodModel>() {
    override val kind: MethodModel.Kind
        get() = KpmCoreCaseTestMethodModel.Kind

    override fun generateSignature(method: KpmCoreCaseTestMethodModel, p: Printer) {
        p.print("public void test${method.name}(KpmTestCase kpmCase) throws Exception")
    }

    override fun generateBody(method: KpmCoreCaseTestMethodModel, p: Printer) {
        val filePath = KtTestUtil.getFilePath(method.pathToTestCase)
        p.println("runTest(SourcesKt.addSourcesFromCanonicalFileStructure(kpmCase, new File(\"$filePath\")));")
    }
}
