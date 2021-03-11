package org.jetbrains.kotlin.testGenerator.model

import org.jetbrains.kotlin.test.KotlinRoot
import java.io.File

interface TGroup {
    val modulePath: String

    val testSourcesPath: String
    val testDataPath: String

    val kotlinRoot: File
    val moduleRoot: File
    val testSourcesRoot: File
    val testDataRoot: File

    val suites: List<TSuite>
}

interface MutableTGroup : TGroup {
    override val suites: MutableList<TSuite>
}

class TGroupImpl(override val modulePath: String, override val testSourcesPath: String, override val testDataPath: String) : MutableTGroup {
    override val kotlinRoot = KotlinRoot.DIR
    override val moduleRoot = File(kotlinRoot, modulePath)
    override val testSourcesRoot = File(moduleRoot, testSourcesPath)
    override val testDataRoot = File(moduleRoot, testDataPath)
    override val suites = mutableListOf<TSuite>()
}

fun MutableTWorkspace.testGroup(
    modulePath: String,
    testSourcesPath: String = "test",
    testDataPath: String = "testData",
    block: MutableTGroup.() -> Unit
) {
    groups += TGroupImpl(modulePath, testSourcesPath, testDataPath).apply(block)
}