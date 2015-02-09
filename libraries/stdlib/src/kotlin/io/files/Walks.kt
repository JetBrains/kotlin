package kotlin.io

import java.io.File
import java.io.IOException
import java.io.FileNotFoundException
import java.util.NoSuchElementException

/** Enum that can be used to specify an order of a recursive walk of a file tree. */
public enum class WalkOrder {
    /** Visit parents first. */
    PARENTS_FIRST

    /** Visit children first. */
    CHILDREN_FIRST
}

/** Enum that can be used to specify further actions after visiting a file during a recursive walk of a file tree. */
public enum class FileVisitResult {
    /** Continue. */
    CONTINUE

    /** Continue without visiting the siblings of this file or directory. */
    SKIP_SIBLINGS

    /** Continue without visiting the entries of this directory. */
    SKIP_SUBTREE

    /** Terminate */
    TERMINATE
}

/** A visitor of files. An implementation of this trait can be provided to the `File.walk()` function.*/
public trait FileVisitor {
    /** Called before visiting a directory. */
    public fun beforeVisitDirectory(dir: File): FileVisitResult

    /** Called after a successful visit of a directory. */
    public fun afterVisitDirectory(dir: File): FileVisitResult

    /** Called in case of error while reading the entries of this directory. */
    public fun visitDirectoryFailed(dir: File, e: IOException): FileVisitResult

    /** Called on files that are not directories and on directories that are on the lowest allowed depth. */
    public fun visitFile(file: File): FileVisitResult
}

/** A simple implementation of the `FileVisitor` trait with default behaviour to visit all files and ignore errors. */
abstract class AbstractFileVisitor : FileVisitor {
    override fun beforeVisitDirectory(dir: File): FileVisitResult {
        return FileVisitResult.CONTINUE
    }

    override fun afterVisitDirectory(dir: File): FileVisitResult {
        return FileVisitResult.CONTINUE
    }

    override fun visitDirectoryFailed(dir: File, e: IOException): FileVisitResult {
        return FileVisitResult.CONTINUE
    }

    override fun visitFile(file: File): FileVisitResult {
        return FileVisitResult.CONTINUE
    }
}

/**
 * Visit this file and all its children recursively with a specified `FileVisitor`,
 * not going deeper than `maxDepth`.
 * If `maxDepth` is null, then the depth is unlimited.
 *
 * This function takes a snapshot of a directory when entering it.
 * Only that files that were in a directory at the moment of opening it are considered, all changes are ignored.
 *
 * Throws:
 * FileNotFoundException - if the start file doesn't exist
 * IllegalArgumentException - if `maxDepth` < 0
 */
public fun File.walkFileTree(fileVisitor: FileVisitor, maxDepth: Int? = null) {
    if (!exists()) {
        throw FileNotFoundException("This file doesn't exist: $this")
    } else if (maxDepth != null && maxDepth < 0) {
        throw IllegalArgumentException("maxDepth < 0: $maxDepth")
    }

    fun walk(file: File, depth: Int): FileVisitResult {
        if (maxDepth != null && depth >= maxDepth || !file.isDirectory()) {
            //visit as a file
            return fileVisitor.visitFile(file)
        }

        //visiting a directory
        val preVisitResult = fileVisitor.beforeVisitDirectory(file)
        when (preVisitResult) {
            FileVisitResult.CONTINUE -> {
                val children = file.listFiles()
                if (children == null) {
                    return fileVisitor.visitDirectoryFailed(file,
                            AccessDeniedException(file = file.toString(), reason = "Cannot list files in a directory"))
                }
                for (child in children) {
                    val childVisitResult = walk(child, depth + 1)
                    when (childVisitResult) {
                        FileVisitResult.TERMINATE -> return FileVisitResult.TERMINATE
                        FileVisitResult.SKIP_SIBLINGS -> break
                    }
                }
                return fileVisitor.afterVisitDirectory(file)
            }
            FileVisitResult.SKIP_SIBLINGS -> return FileVisitResult.SKIP_SIBLINGS
            FileVisitResult.SKIP_SUBTREE -> return FileVisitResult.CONTINUE
            FileVisitResult.TERMINATE -> return FileVisitResult.TERMINATE
        }
        throw AssertionError("Unreachable state")
    }
    walk(this, 0)
}

/**
 * Visit this file and all its children recursively in a specified order,
 * not going deeper than `maxDepth`, and call a given block on them.
 * If `maxDepth` is null, then the depth is unlimited.
 * The walk order depends on results of the block evaluation.
 * If some directory cannot be opened, then it is processed, but
 * its subtree is skipped.
 *
 * This function takes a snapshot of a directory when entering it.
 * Only that files that were in a directory at the moment of opening it are considered, all changes are ignored.
 *
 * Throws:
 * FileNotFoundException - if the start file doesn't exist
 * IllegalArgumentException - if `maxDepth` < 0
 */
public fun File.walkSelectively(walkOrder: WalkOrder = WalkOrder.PARENTS_FIRST,
                     maxDepth: Int? = null,
                     block: (File) -> FileVisitResult) {
    val fileVisitor = object : AbstractFileVisitor() {
        override fun beforeVisitDirectory(dir: File): FileVisitResult {
            return when (walkOrder) {
                WalkOrder.CHILDREN_FIRST -> FileVisitResult.CONTINUE
                WalkOrder.PARENTS_FIRST -> block(dir)
            }
        }

        override fun afterVisitDirectory(dir: File): FileVisitResult {
            return when (walkOrder) {
                WalkOrder.CHILDREN_FIRST -> block(dir)
                WalkOrder.PARENTS_FIRST -> FileVisitResult.CONTINUE
            }
        }

        override fun visitFile(file: File): FileVisitResult {
            return block(file)
        }

        override fun visitDirectoryFailed(dir: File, e: IOException): FileVisitResult {
            return when (walkOrder) {
                WalkOrder.CHILDREN_FIRST -> block(dir)
                WalkOrder.PARENTS_FIRST -> FileVisitResult.CONTINUE
            }
        }
    }
    walkFileTree(fileVisitor, maxDepth)
}

/**
 * Recursively process this file and all its children with the given block in a specified order,
 * not going deeper than `maxDepth`.
 * If `maxDepth` is null, then the depth is unlimited.
 * If some directory cannot be opened, then it is processed, but
 * its subtree is skipped.
 *
 * This function takes a snapshot of a directory when entering it.
 * Only that files that were in a directory at the moment of opening it are considered, all changes are ignored.
 *
 * Throws:
 * FileNotFoundException - if the start file doesn't exist
 * IllegalArgumentException - if `maxDepth` < 0
 */
public fun File.walkFileTree(walkOrder: WalkOrder = WalkOrder.PARENTS_FIRST,
                        maxDepth: Int? = null,
                        block: (File) -> Unit) {
    walkSelectively(walkOrder, maxDepth, {
        block(it)
        FileVisitResult.CONTINUE
    })
}

deprecated("Use walkSelectively() function instead")
public fun File.recurse(block: (File) -> Unit): Unit {
    block(this)
    listFiles()?.forEach { it.recurse(block) }
}

/**
 * Stream allowing you to get all files that have the specified file as an ancestor in a specified order,
 * not going deeper than `maxDepth`.
 * If `maxDepth` is null, then the depth is unlimited.
 * If some directory cannot be opened, then it is processed, but
 * its subtree is skipped.
 *
 * This stream takes a snapshot of a directory when entering it.
 * Only that files that were in a directory at the moment of opening it are considered, all changes are ignored.
 *
 * If you specify a nonexistent file as a constructor parameter, FileNotFoundException will be thrown.
 */
public class TraverseStream(public val start: File,
                            public val walkOrder: WalkOrder = WalkOrder.PARENTS_FIRST,
                            public val maxDepth: Int? = null) : Stream<File> {
    {
        if (!start.exists()) {
            throw FileNotFoundException("The start file doesn't exist: $start")
        }
    }

    override fun iterator(): Iterator<File> = object : Iterator<File> {
        inner class Cursor(val files: Array<File>) {
            var pos: Int = 0
            val curFile: File
                get() = files[pos]
        }

        val cursors = arrayListOf(Cursor(array(start)))
        var childrenVisited = false
        val cursor: Cursor // cursor.curFile denotes next file
            get() = cursors.last()!!
        {
            if (walkOrder == WalkOrder.CHILDREN_FIRST) {
                findBottom()
            }
        }

        fun findBottom() {
            while (!childrenVisited) {
                val files = cursor.curFile.listFiles()
                if ((maxDepth == null || cursors.size() <= maxDepth!!) && files != null && files.isNotEmpty()) {
                    cursors.add(Cursor(files))
                } else {
                    childrenVisited = true
                }
            }
        }

        fun findNextInChildrenFirstOrder(): File? {
            if (!cursors.isEmpty() && cursor.pos == cursor.files.size() - 1) {
                cursors.remove(cursors.lastIndex)
                childrenVisited = true
                if (cursors.isEmpty()) {
                    return null
                }
            } else {
                cursor.pos++
                childrenVisited = false
                findBottom()
            }
            return cursor.curFile
        }

        fun findNextInParentsFirstOrder(): File? {
            while (childrenVisited && !cursors.isEmpty() && cursor.pos == cursor.files.size() - 1) {
                cursors.remove(cursors.lastIndex)
                childrenVisited = true
            }
            if (cursors.isEmpty()) {
                return null
            }
            if (childrenVisited) {
                cursor.pos++
                childrenVisited = false
            } else {
                val files = cursor.curFile.listFiles()
                if ((maxDepth == null || cursors.size() <= maxDepth!!) && files != null && files.isNotEmpty()) {
                    cursors.add(Cursor(files))
                } else {
                    childrenVisited = true
                    return findNextInParentsFirstOrder()
                }
            }
            return cursor.curFile
        }

        fun findNext(): File? {
            when (walkOrder) {
                WalkOrder.CHILDREN_FIRST -> {
                    findNextInChildrenFirstOrder()
                }
                WalkOrder.PARENTS_FIRST -> {
                    findNextInParentsFirstOrder()
                }
            }
            if (cursors.isEmpty()) {
                return null
            }
            return cursor.curFile
        }

        override fun next(): File {
            if (!hasNext()) {
                throw NoSuchElementException("There is no next file")
            }
            val result = cursor.curFile
            findNext()
            return result
        }

        override fun hasNext(): Boolean {
            return cursors.isNotEmpty()
        }
    }
}

/**
 * Constructs a TraverseStream of this file with the specified parameters and returns it as a result.
 */
public fun File.streamFileTree(walkOrder: WalkOrder = WalkOrder.PARENTS_FIRST, maxDepth: Int? = null): Stream<File> {
    return TraverseStream(this, walkOrder, maxDepth)
}

/**
 * Returns a list of files that have this file as an ancestor including this file itself.
 * You can customize the recursive walk that is used to form the result with `walkOrder` and `maxDepth` parameters.
 */
public fun File.listFileTree(walkOrder: WalkOrder = WalkOrder.PARENTS_FIRST, maxDepth: Int? = null): List<File> {
    return streamFileTree(walkOrder, maxDepth).toList()
}
