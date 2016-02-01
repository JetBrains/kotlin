@file:JvmVersion
@file:JvmName("ConstantsKt")
package kotlin.io

/**
 * Returns the default buffer size when working with buffered streams.
*/

public const val DEFAULT_BUFFER_SIZE: Int = 64 * 1024

@Deprecated("Use DEFAULT_BUFFER_SIZE constant instead.", ReplaceWith("kotlin.io.DEFAULT_BUFFER_SIZE"), level = DeprecationLevel.ERROR)
public val defaultBufferSize: Int = DEFAULT_BUFFER_SIZE

/**
 * Returns the default block size for forEachBlock().
 */
internal const val defaultBlockSize: Int = 4096
/**
 * Returns the minimum block size for forEachBlock().
 */
internal const val minimumBlockSize: Int = 512