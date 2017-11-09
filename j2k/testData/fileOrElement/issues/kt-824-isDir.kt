package test

import java.io.File

/**
 * User: ignatov
 */
object Test {
    fun isDir(parent: File?): Boolean {
        if (parent == null || !parent.exists()) {
            return false
        }
        val result = true
        return if (parent.isDirectory) {
            true
        } else
            false
    }
}