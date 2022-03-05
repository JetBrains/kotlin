/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tooling.core

import java.io.Serializable
import java.util.*

fun KotlinToolingVersion(kotlinVersionString: String): KotlinToolingVersion {
    fun throwInvalid(): Nothing {
        throw IllegalArgumentException("Invalid Kotlin version: $kotlinVersionString")
    }

    val baseVersion = kotlinVersionString.split("-", limit = 2)[0]
    val classifier = kotlinVersionString.split("-", limit = 2).getOrNull(1)

    val baseVersionSplit = baseVersion.split(".")
    if (!(baseVersionSplit.size == 2 || baseVersionSplit.size == 3)) throwInvalid()

    return KotlinToolingVersion(
        major = baseVersionSplit[0].toIntOrNull() ?: throwInvalid(),
        minor = baseVersionSplit[1].toIntOrNull() ?: throwInvalid(),
        patch = baseVersionSplit.getOrNull(2)?.let { it.toIntOrNull() ?: throwInvalid() } ?: 0,
        classifier = classifier
    )
}

fun KotlinToolingVersion(kotlinVersion: KotlinVersion, classifier: String? = null): KotlinToolingVersion {
    return KotlinToolingVersion(kotlinVersion.major, kotlinVersion.minor, kotlinVersion.patch, classifier)
}

class KotlinToolingVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val classifier: String?
) : Comparable<KotlinToolingVersion>, Serializable {

    enum class Maturity {
        SNAPSHOT, DEV, MILESTONE, ALPHA, BETA, RC, STABLE
    }

    val maturity: Maturity
        get() {
            val classifier = this.classifier?.toLowerCase(Locale.ROOT)
            return when {
                classifier == null || classifier.matches(Regex("""(release-)?\d+""")) -> Maturity.STABLE
                classifier.matches(Regex("""(rc)(\d*)?-?\d*""")) -> Maturity.RC
                classifier.matches(Regex("""beta(\d*)?-?\d*""")) -> Maturity.BETA
                classifier.matches(Regex("""alpha(\d*)?-?\d*""")) -> Maturity.ALPHA
                classifier.matches(Regex("""m\d+(-\d*)?""")) -> Maturity.MILESTONE
                classifier.matches(Regex("""dev-?\d*""")) -> Maturity.DEV
                classifier == "snapshot" -> Maturity.SNAPSHOT
                else -> throw IllegalArgumentException("Can't infer maturity of KotlinVersion $this")
            }
        }

    override fun compareTo(other: KotlinToolingVersion): Int {
        if (this == other) return 0
        (this.major - other.major).takeIf { it != 0 }?.let { return it }
        (this.minor - other.minor).takeIf { it != 0 }?.let { return it }
        (this.patch - other.patch).takeIf { it != 0 }?.let { return it }
        (this.maturity.ordinal - other.maturity.ordinal).takeIf { it != 0 }?.let { return it }

        if (this.classifier == null && other.classifier != null) {
            /* eg. 1.6.20 > 1.6.20-200 */
            return 1
        }

        if (this.classifier != null && other.classifier == null) {
            /* e.g. 1.6.20-200 < 1.6.20 */
            return -1
        }

        val thisClassifierNumber = this.classifierNumber
        val otherClassifierNumber = other.classifierNumber
        if (thisClassifierNumber != null && otherClassifierNumber != null) {
            (thisClassifierNumber - otherClassifierNumber).takeIf { it != 0 }?.let { return it }
        }

        if (thisClassifierNumber != null && otherClassifierNumber == null) {
            /* e.g. 1.6.20-rc1 > 1.6.20-rc */
            return 1
        }

        if (thisClassifierNumber == null && otherClassifierNumber != null) {
            /* e.g. 1.6.20-rc < 1.6.20-rc1 */
            return -1
        }

        val thisBuildNumber = this.buildNumber
        val otherBuildNumber = other.buildNumber
        if (thisBuildNumber != null && otherBuildNumber != null) {
            (thisBuildNumber - otherBuildNumber).takeIf { it != 0 }?.let { return it }
        }

        if (thisBuildNumber == null && otherBuildNumber != null) {
            /* e.g. 1.6.20-M1 > 1.6.20-M1-200 */
            return 1
        }

        if (thisBuildNumber != null && otherBuildNumber == null) {
            /* e.g. 1.6.20-M1-200 < 1.6.20-M1 */
            return -1
        }

        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KotlinToolingVersion) return false
        if (this.major != other.major) return false
        if (this.minor != other.minor) return false
        if (this.patch != other.patch) return false
        if (this.classifier?.toLowerCase(Locale.ROOT) != other.classifier?.toLowerCase(Locale.ROOT)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = major
        result = 31 * result + minor
        result = 31 * result + patch
        result = 31 * result + (classifier?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "$major.$minor.$patch" + if (classifier != null) "-$classifier" else ""
    }
}

fun KotlinToolingVersion.toKotlinVersion(): KotlinVersion =
    KotlinVersion(major, minor, patch)

fun KotlinVersion.toKotlinToolingVersion(classifier: String? = null): KotlinToolingVersion =
    KotlinToolingVersion(this, classifier)

val KotlinToolingVersion.isSnapshot: Boolean
    get() = this.maturity == KotlinToolingVersion.Maturity.SNAPSHOT

val KotlinToolingVersion.isDev: Boolean
    get() = this.maturity == KotlinToolingVersion.Maturity.DEV

val KotlinToolingVersion.isMilestone: Boolean
    get() = this.maturity == KotlinToolingVersion.Maturity.MILESTONE

val KotlinToolingVersion.isAlpha: Boolean
    get() = this.maturity == KotlinToolingVersion.Maturity.ALPHA

val KotlinToolingVersion.isBeta: Boolean
    get() = this.maturity == KotlinToolingVersion.Maturity.BETA

val KotlinToolingVersion.isRC: Boolean
    get() = this.maturity == KotlinToolingVersion.Maturity.RC

val KotlinToolingVersion.isStable: Boolean
    get() = this.maturity == KotlinToolingVersion.Maturity.STABLE

val KotlinToolingVersion.isPreRelease: Boolean get() = !isStable

val KotlinToolingVersion.buildNumber: Int?
    get() {
        if (classifier == null) return null

        /*
        Handle classifiers that only consist of version + build number. This is used for stable releases
        like:
        1.6.20-1
        1.6.20-22
        1.6.
         */
        val buildNumberOnlyClassifierRegex = Regex("\\d+")
        if (buildNumberOnlyClassifierRegex.matches(classifier)) {
            return classifier.toIntOrNull()
        }

        val classifierRegex = Regex("""(.+?)(\d*)?-?(\d*)?""")
        val classifierMatch = classifierRegex.matchEntire(classifier) ?: return null
        return classifierMatch.groupValues.getOrNull(3)?.toIntOrNull()
    }

val KotlinToolingVersion.classifierNumber: Int?
    get() {
        if (classifier == null) return null

        /*
        Classifiers with only a buildNumber assigned
         */
        val buildNumberOnlyClassifierRegex = Regex("\\d+")
        if (buildNumberOnlyClassifierRegex.matches(classifier)) {
            return null
        }


        val classifierRegex = Regex("""(.+?)(\d*)?-?(\d*)?""")
        val classifierMatch = classifierRegex.matchEntire(classifier) ?: return null
        return classifierMatch.groupValues.getOrNull(2)?.toIntOrNull()
    }

operator fun String.compareTo(version: KotlinToolingVersion): Int {
    return KotlinToolingVersion(this).compareTo(version)
}

operator fun KotlinToolingVersion.compareTo(kotlinVersionString: String): Int {
    return this.compareTo(KotlinToolingVersion(kotlinVersionString))
}
