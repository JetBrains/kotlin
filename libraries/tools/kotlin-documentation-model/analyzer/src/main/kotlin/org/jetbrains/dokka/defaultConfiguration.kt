/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import java.io.File

public data class DokkaConfigurationImpl(
    override val moduleName: String = DokkaDefaults.moduleName,
    override val moduleVersion: String? = DokkaDefaults.moduleVersion,
    override val outputDir: File = DokkaDefaults.outputDir,
    override val sourceSets: List<DokkaSourceSetImpl> = emptyList(),
    override val pluginsClasspath: List<File> = emptyList(),
    override val pluginsConfiguration: List<PluginConfigurationImpl> = DokkaDefaults.pluginsConfiguration,
    override val modules: List<DokkaModuleDescriptionImpl> = emptyList(),
    override val failOnWarning: Boolean = DokkaDefaults.failOnWarning,
    override val suppressObviousFunctions: Boolean = DokkaDefaults.suppressObviousFunctions,
    override val includes: Set<File> = emptySet(),
    override val suppressInheritedMembers: Boolean = DokkaDefaults.suppressInheritedMembers,
    override val finalizeCoroutines: Boolean = true,
) : DokkaConfiguration

public data class PluginConfigurationImpl(
    override val fqPluginName: String,
    override val serializationFormat: DokkaConfiguration.SerializationFormat,
    override val values: String
) : DokkaConfiguration.PluginConfiguration


public data class DokkaSourceSetImpl(
    override val displayName: String = DokkaDefaults.sourceSetDisplayName,
    override val sourceSetID: DokkaSourceSetID,
    override val classpath: List<File> = emptyList(),
    override val sourceRoots: Set<File> = emptySet(),
    override val dependentSourceSets: Set<DokkaSourceSetID> = emptySet(),
    override val samples: Set<File> = emptySet(),
    override val includes: Set<File> = emptySet(),
    override val reportUndocumented: Boolean = DokkaDefaults.reportUndocumented,
    override val skipEmptyPackages: Boolean = DokkaDefaults.skipEmptyPackages,
    override val skipDeprecated: Boolean = DokkaDefaults.skipDeprecated,
    override val jdkVersion: Int = DokkaDefaults.jdkVersion,
    override val perPackageOptions: List<PackageOptionsImpl> = mutableListOf(),
    override val languageVersion: String? = null,
    override val apiVersion: String? = null,
    override val suppressedFiles: Set<File> = emptySet(),
    override val analysisPlatform: Platform = DokkaDefaults.analysisPlatform,
    override val documentedVisibilities: Set<DokkaConfiguration.Visibility> = DokkaDefaults.documentedVisibilities,
    override val suppressAnnotatedWith: Set<String> = DokkaDefaults.suppressAnnotatedWith,
) : DokkaSourceSet {

    override fun equals(other: Any?): Boolean {
        return sourceSetID == (other as? DokkaSourceSet)?.sourceSetID
    }

    override fun hashCode(): Int {
        return sourceSetID.hashCode()
    }
}

public data class DokkaModuleDescriptionImpl(
    override val name: String,
    override val relativePathToOutputDirectory: File,
    override val includes: Set<File>,
    override val sourceOutputDirectory: File
) : DokkaConfiguration.DokkaModuleDescription

public data class PackageOptionsImpl(
    override val matchingRegex: String,
    override val reportUndocumented: Boolean?,
    override val skipDeprecated: Boolean,
    override val suppress: Boolean,
    override val documentedVisibilities: Set<DokkaConfiguration.Visibility>, // TODO add default to DokkaDefaults.documentedVisibilities
) : DokkaConfiguration.PackageOptions
