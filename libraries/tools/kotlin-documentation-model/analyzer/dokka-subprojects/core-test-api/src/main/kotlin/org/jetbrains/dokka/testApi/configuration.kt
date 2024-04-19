/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.testApi

import org.jetbrains.dokka.DokkaConfiguration
import kotlin.test.assertEquals

public fun assertDokkaConfigurationEquals(
    expected: DokkaConfiguration,
    actual: DokkaConfiguration
) {
    assertEquals(expected.moduleName, actual.moduleName, "DokkaConfiguration.moduleName")
    assertEquals(expected.moduleVersion, actual.moduleVersion, "DokkaConfiguration.moduleVersion")
    assertEquals(expected.outputDir, actual.outputDir, "DokkaConfiguration.outputDir")
    assertEquals(expected.cacheRoot, actual.cacheRoot, "DokkaConfiguration.cacheRoot")
    assertEquals(expected.offlineMode, actual.offlineMode, "DokkaConfiguration.offlineMode")
    assertEquals(expected.pluginsClasspath, actual.pluginsClasspath, "DokkaConfiguration.pluginsClasspath")
    assertEquals(expected.pluginsConfiguration, actual.pluginsConfiguration, "DokkaConfiguration.pluginsConfiguration")
    assertEquals(expected.modules, actual.modules, "DokkaConfiguration.modules")
    assertEquals(expected.failOnWarning, actual.failOnWarning, "DokkaConfiguration.failOnWarning")
    assertEquals(
        expected.delayTemplateSubstitution,
        actual.delayTemplateSubstitution,
        "DokkaConfiguration.delayTemplateSubstitution"
    )
    assertEquals(
        expected.suppressObviousFunctions,
        actual.suppressObviousFunctions,
        "DokkaConfiguration.suppressObviousFunctions"
    )
    assertEquals(expected.includes, actual.includes, "DokkaConfiguration.includes")
    assertEquals(
        expected.suppressInheritedMembers,
        actual.suppressInheritedMembers,
        "DokkaConfiguration.suppressInheritedMembers"
    )
    assertEquals(expected.finalizeCoroutines, actual.finalizeCoroutines, "DokkaConfiguration.finalizeCoroutines")

    assertEquals(expected.sourceSets.size, actual.sourceSets.size, "DokkaConfiguration.sourceSets.size")
    expected.sourceSets.zip(actual.sourceSets) { expectedSourceSet, actualSourceSet ->
        assertDokkaSourceSetEquals(expectedSourceSet, actualSourceSet)
    }
}

public fun assertDokkaSourceSetEquals(
    expected: DokkaConfiguration.DokkaSourceSet,
    actual: DokkaConfiguration.DokkaSourceSet
) {
    assertEquals(expected.sourceSetID, actual.sourceSetID, "DokkaSourceSet.sourceSetID")
    assertEquals(expected.displayName, actual.displayName, "DokkaSourceSet.displayName")
    assertEquals(expected.classpath, actual.classpath, "DokkaSourceSet.classpath")
    assertEquals(expected.sourceRoots, actual.sourceRoots, "DokkaSourceSet.sourceRoots")
    assertEquals(expected.dependentSourceSets, actual.dependentSourceSets, "DokkaSourceSet.dependentSourceSets")
    assertEquals(expected.samples, actual.samples, "DokkaSourceSet.samples")
    assertEquals(expected.includes, actual.includes, "DokkaSourceSet.includes")
    @Suppress("DEPRECATION")
    assertEquals(expected.includeNonPublic, actual.includeNonPublic, "DokkaSourceSet.includeNonPublic")
    assertEquals(expected.reportUndocumented, actual.reportUndocumented, "DokkaSourceSet.reportUndocumented")
    assertEquals(expected.skipEmptyPackages, actual.skipEmptyPackages, "DokkaSourceSet.skipEmptyPackages")
    assertEquals(expected.skipDeprecated, actual.skipDeprecated, "DokkaSourceSet.skipDeprecated")
    assertEquals(expected.jdkVersion, actual.jdkVersion, "DokkaSourceSet.jdkVersion")
    assertEquals(expected.sourceLinks, actual.sourceLinks, "DokkaSourceSet.sourceLinks")
    assertEquals(expected.perPackageOptions, actual.perPackageOptions, "DokkaSourceSet.perPackageOptions")
    assertEquals(
        expected.externalDocumentationLinks,
        actual.externalDocumentationLinks,
        "DokkaSourceSet.externalDocumentationLinks"
    )
    assertEquals(expected.languageVersion, actual.languageVersion, "DokkaSourceSet.languageVersion")
    assertEquals(expected.apiVersion, actual.apiVersion, "DokkaSourceSet.apiVersion")
    assertEquals(expected.noStdlibLink, actual.noStdlibLink, "DokkaSourceSet.noStdlibLink")
    assertEquals(expected.noJdkLink, actual.noJdkLink, "DokkaSourceSet.noJdkLink")
    assertEquals(expected.suppressedFiles, actual.suppressedFiles, "DokkaSourceSet.suppressedFiles")
    assertEquals(expected.analysisPlatform, actual.analysisPlatform, "DokkaSourceSet.analysisPlatform")
    assertEquals(
        expected.documentedVisibilities,
        actual.documentedVisibilities,
        "DokkaSourceSet.documentedVisibilities"
    )
}
