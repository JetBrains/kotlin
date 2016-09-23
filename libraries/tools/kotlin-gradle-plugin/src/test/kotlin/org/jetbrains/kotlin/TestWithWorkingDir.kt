package org.jetbrains.kotlin

import org.jetbrains.kotlin.com.intellij.openapi.util.io.FileUtil
import org.junit.After
import org.junit.Before
import java.io.File
import kotlin.properties.Delegates

abstract class TestWithWorkingDir {
    protected var workingDir: File by Delegates.notNull()
        private set

    @Before
    open fun setUp() {
        workingDir = FileUtil.createTempDirectory(this.javaClass.simpleName, null)
    }

    @After
    open fun tearDown() {
        workingDir.deleteRecursively()
    }
}