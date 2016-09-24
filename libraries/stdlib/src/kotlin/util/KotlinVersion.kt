package kotlin

/**
 * Represents a version of the Kotlin standard library
 */
public class KotlinVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<KotlinVersion> {
    public constructor(major: Int, minor: Int) : this(major, minor, 0)

    private val version = versionOf(major, minor, patch)

    private fun versionOf(major: Int, minor: Int, patch: Int): Int {
        require(major in 0..99 && minor in 0..99 && patch in 0..99) { "Version components are out of range: $major.$minor.$patch" }
        return major * 100 * 100 + minor * 100 + patch
    }

    /**
     * Returns the integer representation of this version.
     *
     * Integer representations of two [KotlinVersion] instances can be compared,
     * so that if `v1.toInt() > v2.toInt()` then `v1 > v2`.
     *
     * This value should not be persisted, as it can change between program runs.
     */
    public fun toInt(): Int = version

    /**
     * Returns the string representation of this version
     */
    override fun toString(): String = "$major.$minor.$patch"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherVersion = (other as? KotlinVersion) ?: return false
        return this.version == otherVersion.version
    }

    override fun hashCode(): Int = version

    override fun compareTo(other: KotlinVersion): Int = version - other.version

    public fun isAtLeast(major: Int, minor: Int): Boolean =
            // or this.version >= versionOf(major, minor, 0)
            this.major > major || (this.major == major &&
                    this.minor >= minor)

    public fun isAtLeast(major: Int, minor: Int, patch: Int): Boolean =
            // or this.version >= versionOf(major, minor, patch)
            this.major > major || (this.major == major &&
                    (this.minor > minor || this.minor == minor &&
                            this.patch >= patch))

    public fun isAtLeast(version: KotlinVersion): Boolean = this >= version

    companion object {
        /**
         * Returns the current version of the Kotlin standard library
         */
        // TODO: get from metadata or hardcode automatically during build
        @kotlin.jvm.JvmField
        public val CURRENT: KotlinVersion = KotlinVersion(1, 1, 0)

        // should we have 'parse'?

    }
}