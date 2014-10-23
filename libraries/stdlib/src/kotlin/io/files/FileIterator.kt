package kotlin.io

import java.io.File
import java.util.NoSuchElementException

fun String.getRootName(): String {
    var first = this.indexOf(File.separatorChar, 0)
    if (first==0) {
        if (length() > 1 && this[1]==File.separatorChar) {
            // Network names like //my.host/home/something => //my.host/home/ should be root
            first = this.indexOf(File.separatorChar, 2)
            if (first>=0) {
                first = this.indexOf(File.separatorChar, first+1)
                if (first >= 0)
                    return this.substring(0, first+1)
            }
        }
        return this.substring(0, 1)
    }
    // C:\
    if (first > 0 && this[first-1]==':') {
        first++
        return this.substring(0, first)
    }
    // C:
    if (first==-1 && this.endsWith(':'))
        return this
    return ""

}

val File.rootName: String
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

/**
 * Iterates over elements of file, e.g. for /foo/bar/gav
 * we will have foo, bar and gav
 */
class FileIterator(private val f: File): Iterator<File> {

    private val path = f.separatorsToSystem()

    val root = path.getRootName()

    // Begin from the last root character
    private var separatorIndex = root.length()-1

    private var nextIndex = separatorIndex

    /**
     * Returns the next element for this file iterator
     * @throws NoSuchElementException if there is no next element
     */
    override fun next(): File {
        if (!hasNext()) {
            throw NoSuchElementException()
        }
        val prevIndex = separatorIndex
        separatorIndex = nextIndex
        return File(path.substring(prevIndex+1, nextIndex))
    }

    /**
     * Returns true if the next element is available for this file iterator,
     * or false otherwise
     */
    override fun hasNext(): Boolean {
        if (nextIndex == separatorIndex) {
            // Consider special "" case
            if (separatorIndex==-1 && path.length()==0) {
                nextIndex = 0
                return true
            }
            // /foo/bar/gav/, next points to last /
            if (nextIndex >= path.length()-1)
                return false
            nextIndex = path.indexOf(File.separatorChar, separatorIndex+1)
            // To remove all "" like in the middle of /home//something/
            if (nextIndex==separatorIndex+1) {
                separatorIndex = nextIndex
                return hasNext()
            }
        }
        if (nextIndex == -1)
            nextIndex = path.length()
        return true
    }
}

/**
 * Contains number of components in this abstract path name,
 * e.g. 3 in /home/user/tmp (home, user, tmp) or 0 in /.
 * Empty path name has one component which is the empty name itself.
 */
public val File.nameCount: Int
    get() {
        var i = 0
        for (elem in this) i++
        return i
    }

/**
 * Returns a relative pathname which is a subsequence of this pathname,
 * beginning from component [beginIndex], inclusive,
 * ending at component [endIndex], exclusive.
 * Number 0 belongs to a component closest to the root,
 * number count-1 belongs to a component farthest from the root
 * @throws IllegalArgumentException if [beginIndex] is negative,
* or [endIndex] is greater than existing number of components,
* or [beginIndex] is greater than or equals to [endIndex]
 */
public fun File.subPath(beginIndex: Int, endIndex: Int): File {
    if (beginIndex < 0 || beginIndex >= endIndex || endIndex > nameCount)
        throw IllegalArgumentException()
    var res = ""
    var i = 0
    for (elem in this) {
        if (i >= beginIndex && i < endIndex) {
            res += elem.toString()
            if (i != endIndex-1)
                res += File.separatorChar
        }
        i++
    }
    return File(res)

}
