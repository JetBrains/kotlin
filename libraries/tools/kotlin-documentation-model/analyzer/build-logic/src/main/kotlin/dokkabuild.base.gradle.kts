/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import dokkabuild.DokkaBuildProperties
import org.gradle.language.base.plugins.LifecycleBasePlugin.VERIFICATION_GROUP

/**
 * A convention plugin that sets up common config and sensible defaults for all subprojects.
 *
 * It provides the [DokkaBuildProperties] extension, for accessing common build properties.
 */

plugins {
    base
}

extensions.create<DokkaBuildProperties>(
    DokkaBuildProperties.EXTENSION_NAME,
    provider { project.version.toString() },
)

tasks.withType<AbstractArchiveTask>().configureEach {
    // https://docs.gradle.org/current/userguide/working_with_files.html#sec:reproducible_archives
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
