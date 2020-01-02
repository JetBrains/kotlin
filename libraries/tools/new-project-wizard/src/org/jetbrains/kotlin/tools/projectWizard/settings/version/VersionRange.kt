package org.jetbrains.kotlin.tools.projectWizard.settings.version

import org.jetbrains.kotlin.tools.projectWizard.core.safeAs
import org.jetbrains.kotlin.tools.projectWizard.settings.version.maven.Restriction

class VersionRange private constructor(private val mavenRange: Restriction) {
    constructor(
        lowerBound: Version?,
        isLowerBoundInclusive: Boolean,
        upperBound: Version?,
        isUpperBoundInclusive: Boolean
    ) : this(
        Restriction(
            lowerBound?.mavenVersion,
            isLowerBoundInclusive,
            upperBound?.mavenVersion,
            isUpperBoundInclusive
        )
    )

    operator fun contains(version: Version) = mavenRange.containsVersion(version.mavenVersion)

    override fun equals(other: Any?): Boolean = mavenRange == other.safeAs<VersionRange>()?.mavenRange
    override fun hashCode(): Int = mavenRange.hashCode()
    override fun toString(): String = mavenRange.toString()

    companion object {
        val ALL = VersionRange(null, true, null, true)
    }
}

operator fun Version.rangeTo(upper: Version): VersionRange =
    VersionRange(this, true, upper, true)