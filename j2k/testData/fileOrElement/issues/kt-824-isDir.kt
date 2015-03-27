package test

import java.io.File

/**
 * User: ignatov
 */
public object Test {
    public fun isDir(parent: File?): Boolean {
        if (parent == null || !parent.exists()) {
            return false
        }
        val result = true
        if (parent.isDirectory()) {
            return true
        } else
            return false
    }
}