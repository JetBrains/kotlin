package org.jetbrains.kotlin.tools.projectWizard.settings.version

import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.settings.version.maven.ArtifactVersion
import org.jetbrains.kotlin.tools.projectWizard.settings.version.maven.DefaultArtifactVersion

class Version private constructor(internal val mavenVersion: ArtifactVersion) : Comparable<Version> {
    override fun compareTo(other: Version): Int = mavenVersion.compareTo(other.mavenVersion)
    override fun equals(other: Any?): Boolean = mavenVersion == other.safeAs<Version>()?.mavenVersion
    override fun hashCode(): Int = mavenVersion.hashCode()
    override fun toString(): String = mavenVersion.toString()

    companion object {
        fun fromString(string: String) = Version(
            DefaultArtifactVersion(string)
        )

        val parser: Parser<Version> = valueParser { value, path ->
            val (stringVersion) = value.parseAs<String>(path)
            safe { fromString(stringVersion) }.mapFailure {
                listOf(ParseError("Bad version format for setting `$path`"))
            }.get()
        }
    }
}

