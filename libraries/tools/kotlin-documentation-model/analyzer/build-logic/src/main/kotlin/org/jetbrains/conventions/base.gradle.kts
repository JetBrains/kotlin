package org.jetbrains.conventions

import org.jetbrains.DokkaBuildProperties

/**
 * A convention plugin that sets up common config and sensible defaults for all subprojects.
 *
 * It provides the [DokkaBuildProperties] extension, for accessing common build properties.
 */

plugins {
    base
}

val dokkaBuildProperties: DokkaBuildProperties = extensions.create(DokkaBuildProperties.EXTENSION_NAME)


if (project != rootProject) {
    project.group = rootProject.group
    project.version = rootProject.version
}
