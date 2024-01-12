/*
 * Copyright 2016-2023 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
package kotlinx.validation.build.conventions

import java.time.Duration

plugins {
    base
}

// common config for all projects

if (project != rootProject) {
    project.version = rootProject.version
    project.group = rootProject.group
}

tasks.withType<AbstractArchiveTask>().configureEach {
    // https://docs.gradle.org/current/userguide/working_with_files.html#sec:reproducible_archives
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.withType<AbstractTestTask>().configureEach {
    timeout.set(Duration.ofMinutes(60))
}

tasks.withType<AbstractCopyTask>().configureEach {
    includeEmptyDirs = false
}
