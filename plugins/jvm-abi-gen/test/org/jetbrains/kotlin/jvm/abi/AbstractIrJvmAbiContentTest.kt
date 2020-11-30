package org.jetbrains.kotlin.jvm.abi

import com.intellij.openapi.util.io.systemIndependentPath
import org.jetbrains.kotlin.codegen.BytecodeListingTextCollectingVisitor
import org.jetbrains.kotlin.incremental.isClassFile
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.org.objectweb.asm.*
import java.io.File

abstract class AbstractIrJvmAbiContentTest : AbstractJvmAbiContentTest() {
    override val useIrBackend = true
}
