@file:JvmVersion
@file:JvmMultifileClass
@file:JvmName("FilesKt")
package kotlin.io

import java.io.File
import kotlin.*

/**
 * Estimation of a root name by a given file name.
 *
 * This implementation is able to find /, Drive:/, Drive: or
 * //network.name/root as possible root names.
 * / denotes File.separator here so \ can be used instead.
 * All other possible roots cannot be identified by this implementation.
 * It's also not guaranteed (but possible) that function will be able to detect a root
 * which is incorrect for current OS. For instance, in Unix function cannot detect
 * network root names like //network.name/root, but can detect Windows roots like C:/.
 *
 * @return length or a substring representing the root for this path, or zero if this file name is relative.
 */
private fun String.getRootLength(): Int {
    // Note: separators should be already replaced to system ones
    var first = indexOf(File.separatorChar, 0)
    if (first == 0) {
        if (length > 1 && this[1] == File.separatorChar) {
            // Network names like //my.host/home/something ? => //my.host/home/ should be root
            // NB: does not work in Unix because //my.host/home is converted into /my.host/home there
            // So in Windows we'll have root of //my.host/home but in Unix just /
            first = indexOf(File.separatorChar, 2)
            if (first >= 0) {
                first = indexOf(File.separatorChar, first + 1)
                if (first >= 0)
                    return first + 1
                else
                    return length
            }
        }
        return 1
    }
    // C:\
    if (first > 0 && this[first - 1] == ':') {
        first++
        return first
    }
    // C:
    if (first == -1 && endsWith(':'))
        return length
    return 0
}

/**
 * Estimation of a root name for this file.
 *
 * This implementation is able to find /, Drive:/, Drive: or
 * //network.name/root as possible root names.
 * / denotes File.separator here so \ can be used instead.
 * All other possible roots cannot be identified by this implementation.
 * It's also not guaranteed (but possible) that function will be able to detect a root
 * which is incorrect for current OS. For instance, in Unix function cannot detect
 * network root names like //network.name/root, but can detect Windows roots like C:/.
 *
 * @return string representing the root for this file, or empty string is this file name is relative.
 */
internal val File.rootName: String
    get() = path.substring(0, path.getRootLength())

/**
 * Returns root component of this abstract name, like / from /home/user, or C:\ from C:\file.tmp,
 * or //my.host/home for //my.host/home/user
 */
internal val File.root: File
    get() = File(rootName)

/**
 * Determines whether this file has a root or it represents a relative path.
 *
 * Returns `true` when this file has non-empty root.
 */
public val File.isRooted: Boolean
    get() = path.getRootLength() > 0

/**
 * Represents the path to a file as a collection of directories.
 *
 * @property root the [File] object representing root of the path (for example, `/` or `C:` or empty for relative paths).
 * @property segments the list of [File] objects representing every directory in the path to the file,
 *     up to an including the file itself.
 */
internal data class FilePathComponents
    internal constructor(public val root: File, public val segments: List<File>) {

    /**
     *  Returns a string representing the root for this file, or an empty string is this file name is relative.
     */
    public val rootName: String get() = root.path

    /**
     * Returns `true` when the [root] is not empty.
     */
    public val isRooted: Boolean get() = root.path.isNotEmpty()

    /**
     * Returns the number of elements in the path to the file.
     */
    public val size: Int get() = segments.size

    /**
     * Returns a sub-path of the path, starting with the directory at the specified [beginIndex] and up
     * to the specified [endIndex].
     */
    public fun subPath(beginIndex: Int, endIndex: Int): File {
        if (beginIndex < 0 || beginIndex > endIndex || endIndex > size)
            throw IllegalArgumentException()

        return File(segments.subList(beginIndex, endIndex).joinToString(File.separator))
    }
}

/**
 * Splits the file into path components (the names of containing directories and the name of the file
 * itself) and returns the resulting collection of components.
 */
internal fun File.toComponents(): FilePathComponents {
    val path = path
    val rootLength = path.getRootLength()
    val rootName = path.substring(0, rootLength)
    val subPath = path.substring(rootLength)
    val list = if (subPath.isEmpty()) listOf() else subPath.split(File.separatorChar).map(::File)
    return FilePathComponents(File(rootName), list)
}

/**
 * Returns a relative pathname which is a subsequence of this pathname,
 * beginning from component [beginIndex], inclusive,
 * ending at component [endIndex], exclusive.
 * Number 0 belongs to a component closest to the root,
 * number count-1 belongs to a component farthest from the root.
 * @throws IllegalArgumentException if [beginIndex] is negative,
* or [endIndex] is greater than existing number of components,
* or [beginIndex] is greater than [endIndex].
 */
internal fun File.subPath(beginIndex: Int, endIndex: Int): File = toComponents().subPath(beginIndex, endIndex)
