package org.jetbrains.uast.test.kotlin

import org.jetbrains.kotlin.idea.artifacts.KotlinArtifacts
import org.jetbrains.uast.test.env.AbstractLargeProjectTest


abstract class AbstractKotlinLargeProjectTest : AbstractLargeProjectTest() {
    override val projectLibraries
        get() = listOf(Pair("KotlinStdlibTestArtifacts", listOf(KotlinArtifacts.instance.kotlinStdlib)))
}