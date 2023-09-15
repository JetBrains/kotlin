/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt4

import com.sun.tools.javac.tree.TreeMaker
import com.sun.tools.javac.util.Context
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.findFacadeClass
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.kapt3.base.KaptContext
import org.jetbrains.kotlin.kapt3.base.KaptOptions
import org.jetbrains.kotlin.kapt3.base.util.KaptLogger
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile

internal class Kapt4ContextForStubGeneration(
    options: KaptOptions,
    withJdk: Boolean,
    logger: KaptLogger,
    val analysisSession: KtAnalysisSession,
    val files: List<KtFile>,
    val overriddenMetadataVersion: BinaryVersion? = null
) : KaptContext(options, withJdk, logger) {
    val classes: Iterable<KtLightClass> = buildSet {
        files.flatMapTo(this) { file ->
            file.children.filterIsInstance<KtClassOrObject>().mapNotNull {
                it.toLightClass()
            }
        }
        files.mapNotNullTo(this) { ktFile -> ktFile.findFacadeClass() }.distinct()
    }

    internal val treeMaker = TreeMaker.instance(context) as Kapt4TreeMaker

    override fun preregisterTreeMaker(context: Context) {
        Kapt4TreeMaker.preRegister(context)
    }
}
