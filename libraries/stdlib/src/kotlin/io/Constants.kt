@file:JvmVersion
@file:JvmMultifileClass
@file:JvmName("FilesKt")
package kotlin.io

/**
 * Returns the default buffer size when working with buffered streams.
 */
public val defaultBufferSize: Int = 64 * 1024
/**
 * Returns the default block size for forEachBlock().
 */
public val defaultBlockSize: Int = 4096
/**
 * Returns the minimum block size for forEachBlock().
 */
public val minimumBlockSize: Int = 512