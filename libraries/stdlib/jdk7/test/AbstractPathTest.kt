/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jdk7.test

import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.test.*

abstract class AbstractPathTest {
    private val cleanUpActions = mutableListOf<Pair<Path, (Path) -> Unit>>()

    fun Path.cleanup(): Path {
        cleanUpActions.add(this to { it.deleteIfExists() })
        return this
    }

    fun Path.cleanupRecursively(): Path {
        cleanUpActions.add(this to {
            if (it.exists(LinkOption.NOFOLLOW_LINKS)) Files.walkFileTree(it, cleanupVisitor)
        })
        return this
    }

    private val cleanupVisitor = object : SimpleFileVisitor<Path>() {
        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
            file.deleteIfExists()
            return super.visitFile(file, attrs)
        }

        override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
            dir.deleteIfExists()
            return super.postVisitDirectory(dir, exc)
        }
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

    fun withRestrictedRead(vararg paths: Path, block: () -> Unit) {
        try {
            if (paths.all { it.toFile().setReadable(false) }) {
                block()
            } else {
                System.err.println("Couldn't restrict read access")
            }
        } finally {
            paths.forEach { it.toFile().setReadable(true) }
        }
    }
}
