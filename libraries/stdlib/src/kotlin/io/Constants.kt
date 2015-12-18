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
@Deprecated("This constant will become private soon, use its value instead.", ReplaceWith("4096"))
public val defaultBlockSize: Int = 4096
/**
 * Returns the minimum block size for forEachBlock().
 */
@Deprecated("This constant will become private soon, use its value instead.", ReplaceWith("512"))
public val minimumBlockSize: Int = 512