/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt4

import com.sun.tools.javac.tree.TreeMaker
import com.sun.tools.javac.util.Context
import org.jetbrains.kotlin.analysis.api.components.KtMetadataCalculator
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.base.kapt3.KaptOptions
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.kapt3.base.KaptContext
import org.jetbrains.kotlin.kapt3.base.util.KaptLogger

class Kapt4ContextForStubGeneration(
    options: KaptOptions,
    withJdk: Boolean,
    logger: KaptLogger,
    val classes: List<KtLightClass>,
    val origins: Map<KtLightClass, KtFile>,
    val metadataCalculator: KtMetadataCalculator
) : KaptContext(options, withJdk, logger) {
    val treeMaker = TreeMaker.instance(context) as Kapt4TreeMaker

    override fun preregisterTreeMaker(context: Context) {
        Kapt4TreeMaker.preRegister(context, this)
    }

    override fun close() {
//        TODO()
    }
}