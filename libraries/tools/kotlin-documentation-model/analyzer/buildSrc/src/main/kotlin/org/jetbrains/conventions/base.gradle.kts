package org.jetbrains.conventions

plugins {
    base
}

// common Gradle configuration that should be applied to all projects

if (project != rootProject) {
    project.group = rootProject.group
    project.version = rootProject.version
}
