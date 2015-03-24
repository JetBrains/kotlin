package kotlin.io

import java.io.File
import java.util.Collections
import java.util.NoSuchElementException

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
 * @return string representing the root for this file, or empty string is this file name is relative
 */
private fun String.getRootName(): String {
    // Note: separators should be already replaced to system ones
    var first = indexOf(File.separatorChar, 0)
    if (first == 0) {
        if (length() > 1 && this[1] == File.separatorChar) {
            // Network names like //my.host/home/something ? => //my.host/home/ should be root
            // NB: does not work in Unix because //my.host/home is converted into /my.host/home there
            // So in Windows we'll have root of //my.host/home but in Unix just /
            first = indexOf(File.separatorChar, 2)
            if (first >= 0) {
                val dot = indexOf('.', 2)
                if (dot >= 0 && dot < first) {
                    first = indexOf(File.separatorChar, first + 1)
                    if (first >= 0)
                        return substring(0, first + 1)
                }
            }
        }
        return substring(0, 1)
    }
    // C:\
    if (first > 0 && this[first - 1] == ':') {
        first++
        return substring(0, first)
    }
    // C:
    if (first == -1 && endsWith(':'))
        return this
    return ""

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
 * @return string representing the root for this file, or empty string is this file name is relative
 */
public val File.rootName: String
    get() = separatorsToSystem().getRootName()

/**
 * Returns root component of this abstract name, like / from /home/user, or C:\ from C:\file.tmp,
 * or //my.host/home for //my.host/home/user,
 * or null if this name is relative, like bar/gav
 */
public val File.root: File?
    get() {
        val name = rootName
        return if (name.length() > 0) File(name) else null
    }

public data class FilePathComponents(public val rootName: String, public val fileList: List<File>) {
    public fun size(): Int = fileList.size()

    public fun subPath(beginIndex: Int, endIndex: Int): File {
        if (beginIndex < 0 || beginIndex > endIndex || endIndex > size())
            throw IllegalArgumentException()

        return File(fileList.subList(beginIndex, endIndex).joinToString(File.separator))
    }
}

public fun File.filePathComponents(): FilePathComponents {
    val path = separatorsToSystem()
    val rootName = path.getRootName()
    val subPath = path.substring(rootName.length())
    // if: a special case when we have only root component
    // Split not only by / or \, but also by //, ///, \\, \\\, etc.
    val list = if (rootName.length() > 0 && subPath.isEmpty()) listOf() else
        // Looks awful but we split just by /+ or \+ depending on OS
        subPath.split("""\Q${File.separatorChar}\E+""".toRegex()).toList().map { it -> File(it) }
    return FilePathComponents(rootName, list)
}

/**
 * Returns a relative pathname which is a subsequence of this pathname,
 * beginning from component [beginIndex], inclusive,
 * ending at component [endIndex], exclusive.
 * Number 0 belongs to a component closest to the root,
 * number count-1 belongs to a component farthest from the root
 * @throws IllegalArgumentException if [beginIndex] is negative,
* or [endIndex] is greater than existing number of components,
* or [beginIndex] is greater than [endIndex]
 */
public fun File.subPath(beginIndex: Int, endIndex: Int): File = filePathComponents().subPath(beginIndex, endIndex)
