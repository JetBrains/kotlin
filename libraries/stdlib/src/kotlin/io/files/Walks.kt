package kotlin.io

import java.io.File

/**
 * Recursively process this file and all children with the given block.
 * Note that if this file doesn't exist, then the block will be executed on it anyway.
 */
public fun File.recurse(block: (File) -> Unit): Unit {
    block(this)
    listFiles()?.forEach { it.recurse(block) }
}
