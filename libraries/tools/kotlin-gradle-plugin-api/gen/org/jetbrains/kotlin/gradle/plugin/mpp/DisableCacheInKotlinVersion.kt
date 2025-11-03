// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.gradle.plugin.mpp

import kotlin.Comparable
import kotlin.Int
import kotlin.String

/**
 *
 * Provides type-safe constants for Kotlin versions to be used in the DSL for disabling the native cache.
 *
 * Disabling the native cache is not recommended and should only be used as a temporary workaround.
 * This class follows a rolling deprecation cycle to ensure that any cache-disabling configuration
 * is reviewed after a Kotlin update.
 *
 * Only the 3 most recent versions are included:
 * - **N (Latest):** The version constant is available.
 * - **N-1 (Deprecated):** The constant is marked with a deprecation warning.
 * - **N-2 (Error):** The constant is marked with a deprecation error.
 * - **N-3 (Dropped):** The constant is removed, causing a compilation failure.
 *
 * This forces a review of the cache-disabling configuration. If the problem is resolved,
 * please remove the DSL entry. If not, please update to the latest version constant.
 *
 * @since 2.3.20
 */
@KotlinNativeCacheApi
public sealed class DisableCacheInKotlinVersion private constructor(
  /**
   * The major version number.
   */
  public val major: Int,
  /**
   * The minor version number.
   */
  public val minor: Int,
  /**
   * The patch version number.
   */
  public val patch: Int,
) : Comparable<DisableCacheInKotlinVersion> {
  /**
   * Returns the string representation of this version (e.g., 'v2_3_0').
   */
  override fun toString(): String = "v${major}_${minor}_${patch}"

  /**
   * Compares this version to another version.
   */
  override fun compareTo(other: DisableCacheInKotlinVersion): Int = compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })

  /**
   * Represents the Kotlin version constant for 2.3.20.
   */
  public object `2_3_20` : DisableCacheInKotlinVersion(2, 3, 20)

  /**
   * Represents the Kotlin version constant for 2.3.255.
   */
  public object `2_3_255` : DisableCacheInKotlinVersion(2, 3, 255)
}
