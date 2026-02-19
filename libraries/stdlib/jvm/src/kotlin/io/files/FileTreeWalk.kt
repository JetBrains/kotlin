/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmMultifileClass
@file:JvmName("FilesKt")

package kotlin.io

import java.io.File
import java.io.IOException
import java.util.ArrayDeque

/**
 * An enumeration to describe possible walk directions.
 * There are two of them: beginning from parents, ending with children,
 * and beginning from children, ending with parents. Both use depth-first search.
 */
public enum class FileWalkDirection {
    /** Depth-first search, directory is visited BEFORE its files */
    TOP_DOWN,
    /** Depth-first search, directory is visited AFTER its files */
    BOTTOM_UP
    // Do we want also breadth-first search?
}

/**
 * This class is intended to implement different file traversal methods.
 * It allows to iterate through all files inside a given directory.
 *
 * Use [File.walk], [File.walkTopDown] or [File.walkBottomUp] extension functions to instantiate a `FileTreeWalk` instance.

 * If the file path given is just a file, walker iterates only it.
 * If the file path given does not exist, walker iterates nothing, i.e. it's equivalent to an empty sequence.
 */
public class FileTreeWalk private constructor(
    private val start: File,
    private val direction: FileWalkDirection = FileWalkDirection.TOP_DOWN,
    private val onEnter: ((File) -> Boolean)?,
    private val onLeave: ((File) -> Unit)?,
    private val onFail: ((f: File, e: IOException) -> Unit)?,
    private val maxDepth: Int = Int.MAX_VALUE
) : Sequence<File> {

    internal constructor(start: File, direction: FileWalkDirection = FileWalkDirection.TOP_DOWN) : this(start, direction, null, null, null)


    /** Returns an iterator walking through files. */
    override fun iterator(): Iterator<File> = FileTreeWalkIterator()

    /** Abstract class that encapsulates file visiting in some order, beginning from a given [root] */
    private abstract class WalkState(val root: File) {
        /** Call of this function proceeds to a next file for visiting and returns it */
        public abstract fun step(): File?
    }

    /** Abstract class that encapsulates directory visiting in some order, beginning from a given [rootDir] */
    private abstract class DirectoryState(rootDir: File) : WalkState(rootDir) {
        init {
            if (_Assertions.ENABLED)
                assert(rootDir.isDirectory) { "rootDir must be verified to be directory beforehand." }
        }
    }

    private inner class FileTreeWalkIterator : AbstractIterator<File>() {

        // Stack of directory states, beginning from the start directory
        private val state = ArrayDeque<WalkState>()

        init {
            when {
                start.isDirectory -> state.push(directoryState(start))
                start.isFile -> state.push(SingleFileState(start))
                else -> done()
            }
        }

        override fun computeNext() {
            val nextFile = gotoNext()
            if (nextFile != null)
                setNext(nextFile)
            else
                done()
        }


        private fun directoryState(root: File): DirectoryState {
            return when (direction) {
                FileWalkDirection.TOP_DOWN -> TopDownDirectoryState(root)
                FileWalkDirection.BOTTOM_UP -> BottomUpDirectoryState(root)
            }
        }

        private tailrec fun gotoNext(): File? {
            // Take next file from the top of the stack or return if there's nothing left
            val topState = state.peek() ?: return null
            val file = topState.step()
            if (file == null) {
                // There is nothing more on the top of the stack, go back
                val _ = state.pop()
                return gotoNext()
            } else {
                // Check that file/directory matches the filter
                if (file == topState.root || !file.isDirectory || state.size >= maxDepth) {
                    // Proceed to a root directory or a simple file
                    return file
                } else {
                    // Proceed to a sub-directory
                    state.push(directoryState(file))
                    return gotoNext()
                }
            }
        }

        /** Visiting in bottom-up order */
        private inner class BottomUpDirectoryState(rootDir: File) : DirectoryState(rootDir) {

            private var rootVisited = false

            private var fileList: Array<File>? = null

            private var fileIndex = 0

            private var failed = false

            /** First all children, then root directory */
            override fun step(): File? {
                if (!failed && fileList == null) {
                    if (onEnter?.invoke(root) == false) {
                        return null
                    }

                    fileList = root.listFiles()
                    if (fileList == null) {
                        onFail?.invoke(root, AccessDeniedException(file = root, reason = "Cannot list files in a directory"))
                        failed = true
                    }
                }
                if (fileList != null && fileIndex < fileList!!.size) {
                    // First visit all files
                    return fileList!![fileIndex++]
                } else if (!rootVisited) {
                    // Then visit root
                    rootVisited = true
                    return root
                } else {
                    // That's all
                    onLeave?.invoke(root)
                    return null
                }
            }
        }

        /** Visiting in top-down order */
        private inner class TopDownDirectoryState(rootDir: File) : DirectoryState(rootDir) {

            private var rootVisited = false

            private var fileList: Array<File>? = null

            private var fileIndex = 0

            /** First root directory, then all children */
            override fun step(): File? {
                if (!rootVisited) {
                    // First visit root
                    if (onEnter?.invoke(root) == false) {
                        return null
                    }

                    rootVisited = true
                    return root
                } else if (fileList == null || fileIndex < fileList!!.size) {
                    if (fileList == null) {
                        // Then read an array of files, if any
                        fileList = root.listFiles()
                        if (fileList == null) {
                            onFail?.invoke(root, AccessDeniedException(file = root, reason = "Cannot list files in a directory"))
                        }
                        if (fileList == null || fileList!!.size == 0) {
                            onLeave?.invoke(root)
                            return null
                        }
                    }
                    // Then visit all files
                    return fileList!![fileIndex++]
                } else {
                    // That's all
                    onLeave?.invoke(root)
                    return null
                }
            }
        }

        private inner class SingleFileState(rootFile: File) : WalkState(rootFile) {
            private var visited: Boolean = false

            init {
                if (_Assertions.ENABLED)
                    assert(rootFile.isFile) { "rootFile must be verified to be file beforehand." }
            }

            override fun step(): File? {
                if (visited) return null
                visited = true
                return root
            }
        }

    }

    /**
     * Sets a predicate [function], that is called on any entered directory before its files are visited
     * and before it is visited itself.
     *
     * If the [function] returns `false` the directory is not entered and neither it nor its files are visited.
     */
    public fun onEnter(function: (File) -> Boolean): FileTreeWalk {
        return FileTreeWalk(start, direction, onEnter = function, onLeave = onLeave, onFail = onFail, maxDepth = maxDepth)
    }

    /**
     * Sets a callback [function], that is called on any left directory after its files are visited and after it is visited itself.
     */
    public fun onLeave(function: (File) -> Unit): FileTreeWalk {
        return FileTreeWalk(start, direction, onEnter = onEnter, onLeave = function, onFail = onFail, maxDepth = maxDepth)
    }

    /**
     * Set a callback [function], that is called on a directory when it's impossible to get its file list.
     *
     * [onEnter] and [onLeave] callback functions are called even in this case.
     */
    public fun onFail(function: (File, IOException) -> Unit): FileTreeWalk {
        return FileTreeWalk(start, direction, onEnter = onEnter, onLeave = onLeave, onFail = function, maxDepth = maxDepth)
    }

    /**
     * Sets the maximum [depth] of a directory tree to traverse. By default there is no limit.
     *
     * The value must be positive and [Int.MAX_VALUE] is used to specify an unlimited depth.
     *
     * With a value of 1, walker visits only the origin directory and all its immediate children,
     * with a value of 2 also grandchildren, etc.
     */
    public fun maxDepth(depth: Int): FileTreeWalk {
        if (depth <= 0)
            throw IllegalArgumentException("depth must be positive, but was $depth.")
        return FileTreeWalk(start, direction, onEnter, onLeave, onFail, depth)
    }
}

/**
 * Gets a sequence for visiting this directory and all its content.
 *
 * @param direction walk direction, top-down (by default) or bottom-up.
 */
public fun File.walk(direction: FileWalkDirection = FileWalkDirection.TOP_DOWN): FileTreeWalk =
    FileTreeWalk(this, direction)

/**
 * Gets a sequence for visiting this directory and all its content in top-down order.
 * Depth-first search is used and directories are visited before all their files.
 */
public fun File.walkTopDown(): FileTreeWalk = walk(FileWalkDirection.TOP_DOWN)

/**
 * Gets a sequence for visiting this directory and all its content in bottom-up order.
 * Depth-first search is used and directories are visited after all their files.
 */
public fun File.walkBottomUp(): FileTreeWalk = walk(FileWalkDirection.BOTTOM_UP)
