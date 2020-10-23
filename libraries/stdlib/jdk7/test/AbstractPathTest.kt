/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jdk7.test
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.test.*

abstract class AbstractPathTest {
    private val cleanUpActions = mutableListOf<Pair<Path, (Path) -> Unit>>()

    fun Path.cleanup(): Path {
        cleanUpActions.add(this to { it.deleteIfExists() })
        return this
    }

    fun Path.cleanupRecursively(): Path {
        cleanUpActions.add(this to { it.toFile().deleteRecursively() })
        return this
    }

    @AfterTest
    fun cleanUp() {
        for ((path, action) in cleanUpActions) {
            try {
                action(path)
            } catch (e: Throwable) {
                println("Failed to execute cleanup action for $path")
            }
        }
    }
}
