import org.gradle.api.Project
import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.api.artifacts.VersionCatalogsExtension

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

fun DependencyResolveDetails.checkAndOverrideBouncyCastleVersion(project: Project) {
    val libs = (project.extensions.getByName("versionCatalogs") as VersionCatalogsExtension).named("libs")
    listOf(
        libs.findLibrary("bouncycastle.bcpg.jdk18on"),
        libs.findLibrary("bouncycastle.bcpkix.jdk18on"),
        libs.findLibrary("bouncycastle.bcprov.jdk18on"),
    ).forEach {
        if (requested.module == it.get().get().module) {
            useVersion(it.get().get().version!!)
            because("CVE-2024-34447, CVE-2024-30172, CVE-2024-30171, CVE-2024-29857")
        }
    }
}
