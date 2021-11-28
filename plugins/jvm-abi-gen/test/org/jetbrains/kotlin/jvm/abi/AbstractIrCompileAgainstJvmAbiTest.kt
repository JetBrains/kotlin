package org.jetbrains.kotlin.jvm.abi

import java.io.File
import java.net.URLClassLoader

abstract class AbstractIrCompileAgainstJvmAbiTest : AbstractCompileAgainstJvmAbiTest() {
    override val useIrBackend = true
}
