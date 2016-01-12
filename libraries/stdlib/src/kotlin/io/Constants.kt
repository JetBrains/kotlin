@file:JvmVersion
@file:JvmMultifileClass
@file:JvmName("FilesKt")
package kotlin.io

/**
 * Returns the default buffer size when working with buffered streams.
*/
public val DEFAULT_BUFFER_SIZE: Int = 64 * 1024

@Deprecated("Use DEFAULT_BUFFER_SIZE constant instead.", ReplaceWith("kotlin.io.DEFAULT_BUFFER_SIZE"))
public val defaultBufferSize: Int = DEFAULT_BUFFER_SIZE

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