/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.api.configuration

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.analysis.test.api.util.mapToSet
import java.io.File

/**
 * Maps [TestDokkaConfiguration] to the actual [DokkaConfiguration] that will
 * be used to run Dokka; The resulting configuration must be valid from Dokka's perspective.
 *
 * @receiver test configuration to map to the real one; all file paths must be relative to the
 *           root of the project.
 * @param projectDir the actual project directory that will be used to create test files in;
 *                   the path must be absolute and must exist.
 */
fun TestDokkaConfiguration.toDokkaConfiguration(projectDir: File): DokkaConfiguration {
    require(projectDir.exists() && projectDir.isDirectory) {
        "Expected the \"projectDir\" File param to exist and be a directory"
    }

    val moduleName = this.moduleName
    val includes = this.includes.mapToSet { it.relativeTo(projectDir) }
    val sourceSets = this.sourceSets.map { it.toDokkaSourceSet(projectDir) }

    return object : DokkaConfiguration {

        /*
         * NOTE: The getters need to return data that can be compared by value.
         *       This means you can't recreate lists of interfaces on every invocation
         *       as their equals will return false, leading to difficult to trace bugs,
         *       especially when it comes to `SourceSetDependent<T>`
         */

        override val moduleName: String
            get() = moduleName

        override val includes: Set<File>
            get() = includes

        override val sourceSets: List<DokkaConfiguration.DokkaSourceSet>
            get() = sourceSets

        /*
         * The plugin API uses the properties below to initialize plugins found on classpath.
         * They are not settable directly in analysis tests, but must not throw any exceptions.
         */
        override val pluginsClasspath: List<File>
            get() = emptyList()
        override val pluginsConfiguration: List<DokkaConfiguration.PluginConfiguration>
            get() = emptyList()

        /*
         * The properties below are not used by the analysis modules,
         * and thus they don't need to be supported.
         *
         * If one of the properties below starts being used during
         * analysis (i.e starts throwing an exception), a corresponding
         * test property should be added along with the mapping.
         */
        override val moduleVersion: String
            get() = throw NotImplementedError("Not expected to be used by analysis modules")
        override val outputDir: File
            get() = throw NotImplementedError("Not expected to be used by analysis modules")
        override val cacheRoot: File
            get() = throw NotImplementedError("Not expected to be used by analysis modules")
        override val offlineMode: Boolean
            get() = throw NotImplementedError("Not expected to be used by analysis modules")
        override val failOnWarning: Boolean
            get() = throw NotImplementedError("Not expected to be used by analysis modules")
        override val modules: List<DokkaConfiguration.DokkaModuleDescription>
            get() = throw NotImplementedError("Not expected to be used by analysis modules")
        override val delayTemplateSubstitution: Boolean
            get() = throw NotImplementedError("Not expected to be used by analysis modules")
        override val suppressObviousFunctions: Boolean
            get() = throw NotImplementedError("Not expected to be used by analysis modules")
        override val suppressInheritedMembers: Boolean
            get() = throw NotImplementedError("Not expected to be used by analysis modules")
        override val finalizeCoroutines: Boolean
            get() = throw NotImplementedError("Not expected to be used by analysis modules")
    }
}

private fun TestDokkaSourceSet.toDokkaSourceSet(relativeToDir: File): DokkaConfiguration.DokkaSourceSet {
    val analysisPlatform = this.analysisPlatform
    val displayName = this.displayName
    val sourceSetID = this.sourceSetID
    val dependentSourceSets = this.dependentSourceSets
    val sourceRoots = this.sourceRoots.mapToSet { it.relativeTo(relativeToDir) }
    val classpath = this.classpath.map { File(it) }
    val includes = this.includes.mapToSet { it.relativeTo(relativeToDir) }
    val samples = this.samples.mapToSet { it.relativeTo(relativeToDir) }
    val languageVersion = this.languageVersion
    val apiVersion = this.apiVersion

    return object : DokkaConfiguration.DokkaSourceSet {

        /*
         * NOTE: The getters need to return data that can be compared by value.
         *       This means you can't recreate lists of interfaces on every invocation
         *       as their equals will return false, leading to difficult to trace bugs,
         *       especially when it comes to `SourceSetDependent<T>`
         */

        override val analysisPlatform: Platform
            get() = analysisPlatform

        override val displayName: String
            get() = displayName

        override val sourceSetID: DokkaSourceSetID
            get() = sourceSetID

        override val dependentSourceSets: Set<DokkaSourceSetID>
            get() = dependentSourceSets

        override val sourceRoots: Set<File>
            get() = sourceRoots

        override val classpath: List<File>
            get() = classpath

        override val includes: Set<File>
            get() = includes

        override val samples: Set<File>
            get() = samples

        override val languageVersion: String?
            get() = languageVersion

        override val apiVersion: String?
            get() = apiVersion

        override fun equals(other: Any?): Boolean {
            return sourceSetID == (other as? DokkaConfiguration.DokkaSourceSet)?.sourceSetID
        }

        override fun hashCode(): Int {
            return sourceSetID.hashCode()
        }

        /*
         * The properties below are not used by the analysis modules,
         * and thus they don't need to be supported.
         *
         * If one of the properties below starts being used during
         * analysis (i.e starts throwing an exception), a corresponding
         * test property should be added along with the mapping.
         */
        @Suppress("OVERRIDE_DEPRECATION")
        override val includeNonPublic: Boolean
            get() = throw NotImplementedError("Not expected to be used by analysis modules")
        override val reportUndocumented: Boolean
            get() = throw NotImplementedError("Not expected to be used by analysis modules")
        override val skipEmptyPackages: Boolean
            get() = throw NotImplementedError("Not expected to be used by analysis modules")
        override val skipDeprecated: Boolean
            get() = throw NotImplementedError("Not expected to be used by analysis modules")
        override val jdkVersion: Int
            get() = throw NotImplementedError("Not expected to be used by analysis modules")
        override val sourceLinks: Set<DokkaConfiguration.SourceLinkDefinition>
            get() = throw NotImplementedError("Not expected to be used by analysis modules")
        override val perPackageOptions: List<DokkaConfiguration.PackageOptions>
            get() = throw NotImplementedError("Not expected to be used by analysis modules")
        override val externalDocumentationLinks: Set<DokkaConfiguration.ExternalDocumentationLink>
            get() = throw NotImplementedError("Not expected to be used by analysis modules")
        override val noStdlibLink: Boolean
            get() = throw NotImplementedError("Not expected to be used by analysis modules")
        override val noJdkLink: Boolean
            get() = throw NotImplementedError("Not expected to be used by analysis modules")
        override val suppressedFiles: Set<File>
            get() = throw NotImplementedError("Not expected to be used by analysis modules")
        override val documentedVisibilities: Set<DokkaConfiguration.Visibility>
            get() = throw NotImplementedError("Not expected to be used by analysis modules")
    }
}

private fun String.relativeTo(dir: File): File {
    return dir.resolve(this.removePrefix("/"))
}
