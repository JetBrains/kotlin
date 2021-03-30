/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
import org.gradle.api.Project

fun Project.kotlinInit(cacheRedirectorEnabled: Boolean) {
    extensions.extraProperties["defaultSnapshotVersion"] = kotlinBuildProperties.defaultSnapshotVersion
    kotlinBootstrapFrom(BootstrapOption.SpaceBootstrap(kotlinBuildProperties.kotlinBootstrapVersion!!, cacheRedirectorEnabled))

    extensions.extraProperties["kotlinVersion"] = findProperty("kotlinVersion")
    extensions.extraProperties["konanVersion"] = findProperty("konanVersion")
}
