package org.jetbrains.kotlin.tools.projectWizard.wizard.ui

import org.jetbrains.kotlin.tools.projectWizard.library.LibraryArtifact
import org.jetbrains.kotlin.tools.projectWizard.library.MavenArtifact
import org.jetbrains.kotlin.tools.projectWizard.library.NpmArtifact

fun LibraryArtifact.render() = when (this) {
    is MavenArtifact -> "$groupId:$artifactId"
    is NpmArtifact -> name
}

