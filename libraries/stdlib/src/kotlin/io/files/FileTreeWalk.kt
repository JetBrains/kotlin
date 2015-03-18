package kotlin.io

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.ArrayList
import java.util.NoSuchElementException
import java.util.Stack

/**
 * Created by Mikhail.Glukhikh on 13/03/2015.
 *
 * An alternative version of file walker
 */

/**
 * An enumeration to describe possible walk directions.
 * There are two of them: beginning from parents, ending with children,
 * and beginning from children, ending with parents. Both use depth-first search.
 */
public enum class FileWalkDirection {
    /** Depth-first search, directory is visited BEFORE its files */
    TOP_DOWN
    /** Depth-first search, directory is visited AFTER its files */
    BOTTOM_UP
    // Do we want also breadth-first search?
}

/**
 * This class is intended to implement different file walk methods.
 * It allows to iterate through all files inside [start] directory.
 * If [start] is just a file, walker iterates only it.
 * If [start] does not exist, walker does not do any iterations at all.
 */
public class FileTreeWalk(private val start: File,
                          private val direction: FileWalkDirection = FileWalkDirection.TOP_DOWN,
                          private val enter: (File) -> Unit = {},
                          private val leave: (File) -> Unit = {},
                          private val fail: (f: File, e: IOException) -> Unit = { f, e -> Unit },
                          private val filter: (File) -> Boolean = { true },
                          private val maxDepth: Int = Int.MAX_VALUE
) : Sequence<File> {

    private abstract class DirectoryState(public val rootDir: File) {
        init {
            if (!rootDir.isDirectory())
                throw IllegalArgumentException("Directory is needed")
        }

        abstract public fun step(): File?
    }

    private inner class BottomUpDirectoryState(rootDir: File) : DirectoryState(rootDir) {

        private var rootVisited = false

        private var fileList: Array<File>? = null

        private var fileIndex = 0

        private var failed = false

        override public fun step(): File? {
            if (!failed && fileList == null) {
                enter(rootDir)
                fileList = rootDir.listFiles()
                if (fileList == null) {
                    fail(rootDir, AccessDeniedException(file = rootDir, reason = "Cannot list files in a directory"))
                    failed = true
                }
            }
            if (fileList != null && fileIndex < fileList!!.size()) {
                // First visit all files
                return fileList!![fileIndex++]
            } else if (!rootVisited) {
                // Then visit root
                rootVisited = true
                return rootDir
            } else {
                // That's all
                leave(rootDir)
                return null
            }
        }
    }

    private inner class TopDownDirectoryState(rootDir: File) : DirectoryState(rootDir) {

        private var rootVisited = false

        private var fileList: Array<File>? = null

        private var fileIndex = 0

        override public fun step(): File? {
            if (!rootVisited) {
                // First visit root
                enter(rootDir)
                rootVisited = true
                return rootDir
            } else if (fileList == null || fileIndex < fileList!!.size()) {
                if (fileList == null) {
                    // Then read an array of files, if any
                    fileList = rootDir.listFiles()
                    if (fileList == null) {
                        fail(rootDir, AccessDeniedException(file = rootDir, reason = "Cannot list files in a directory"))
                    }
                    if (fileList == null || fileList!!.size() == 0) {
                        leave(rootDir)
                        return null
                    }
                }
                // Then visit all files
                return fileList!![fileIndex++]
            } else {
                // That's all
                leave(rootDir)
                return null
            }
        }
    }

    // Stack of directory states, beginning from the start directory
    private val state = Stack<DirectoryState>()

    // We are already at the end or not?
    private var end = false

    // A future result of next() call
    private var nextFile: File? = null

    init {
        if (!start.exists()) {
            end = true
        } else if (start.isDirectory() && filter(start)) {
            pushState(start)
        }
    }

    private fun pushState(root: File) {
        state.push(when (direction) {
            FileWalkDirection.TOP_DOWN -> TopDownDirectoryState(root)
            FileWalkDirection.BOTTOM_UP -> BottomUpDirectoryState(root)
        })
    }

    tailRecursive
    private fun gotoNext(): File? {
        if (end) {
            // We are already at the end
            return null
        } else if (state.empty()) {
            // There is nothing in the state
            // We must visit "start" if it's a file and matches the filter
            end = true
            return if (start.exists() && !start.isDirectory() && filter(start)) start else null
        }
        // Take next file from the top of the stack
        val topState = state.peek()
        val file = topState.step()
        if (file == null) {
            // There is nothing more on the top of the stack, go back
            state.pop()
            return gotoNext()
        } else {
            // Check that file/directory matches the filter
            if (!filter(file))
                return gotoNext()
            if (file == topState.rootDir || !file.isDirectory() || state.size() >= maxDepth) {
                // Proceed a root directory or a simple file
                return file
            } else {
                // Proceed a sub-directory
                pushState(file)
                return gotoNext()
            }
        }
    }

    /**
     * Sets enter directory function.
     * Enter function is called BEFORE the corresponding directory and its files are visited
     */
    public fun enter(f: (File) -> Unit): FileTreeWalk {
        return FileTreeWalk(start, direction, f, leave, fail, filter, maxDepth)
    }

    /**
     * Sets leave directory function.
     * Leave function is called AFTER the corresponding directory and its files are visited
     */
    public fun leave(f: (File) -> Unit): FileTreeWalk {
        return FileTreeWalk(start, direction, enter, f, fail, filter, maxDepth)
    }

    /**
     * Set fail entering directory function.
     * Fail function is called when walker is unable to get list of directory files.
     * Enter and leave functions are called even in this case.
     */
    public fun fail(f: (File, IOException) -> Unit): FileTreeWalk {
        return FileTreeWalk(start, direction, enter, leave, f, filter, maxDepth)
    }

    /**
     * Sets filter function.
     * Filter function is called before visiting files and entering directories.
     * If it returns false, file is not visited, directory is not entered and all its content is also not visited.
     * If it returns true, everything goes in the regular way
     */
    public fun filter(f: (File) -> Boolean): FileTreeWalk {
        return FileTreeWalk(start, direction, enter, leave, fail, f, maxDepth)
    }

    /**
     * Sets maximum depth of walk. Int.MAX_VALUE is used for unlimited.
     * Negative and zero values are not allowed
     */
    public fun maxDepth(depth: Int): FileTreeWalk {
        if (depth <= 0)
            throw IllegalArgumentException("Use positive depth value")
        return FileTreeWalk(start, direction, enter, leave, fail, filter, depth)
    }

    private val it = object : Iterator<File> {
        override public fun hasNext(): Boolean {
            if (nextFile == null)
                nextFile = gotoNext()
            return (nextFile != null)
        }

        override public fun next(): File {
            if (nextFile == null)
                nextFile = gotoNext()
            val res = nextFile
            if (res == null)
                throw NoSuchElementException()
            // With nextFile = gotoNext() here some enter() / leave() can be called BEFORE visiting res
            nextFile = null
            // Visit this directory or file
            return res
        }
    }

    override public fun iterator(): Iterator<File> {
        return it
    }
}

/**
 * Gets a sequence for visiting this directory and all its content.
 *
 * @param direction walk direction, top-down (by default) or bottom-up
 */
public fun File.walk(direction: FileWalkDirection = FileWalkDirection.TOP_DOWN): FileTreeWalk =
        FileTreeWalk(this, direction)

/**
 * Gets a sequence for visiting this directory and all its content in top-down order.
 * Depth-first search is used and directories are visited before all their files
 */
public fun File.walkTopDown(): FileTreeWalk = walk(FileWalkDirection.TOP_DOWN)

/**
 * Gets a sequence for visiting this directory and all its content in bottom-up order.
 * Depth-first search is used and directories are visited after all their files
 */
public fun File.walkBottomUp(): FileTreeWalk = walk(FileWalkDirection.BOTTOM_UP)

/**
 * Recursively process this file and all children with the given block.
 * Note that if this file doesn't exist, then the block will be executed on it anyway.
 *
 * @param block the function to call on each file
 */
deprecated("It's recommended to use walkTopDown() / walkBottomUp()")
public fun File.recurse(block: (File) -> Unit): Unit {
    walkTopDown().forEach { block(it) }
}
