package org.jetbrains.teamcity

import org.gradle.api.Project
import org.gradle.api.ProjectEvaluationListener
import org.gradle.api.ProjectState

class AfterEvaluateListener(private val action: (Project) -> Unit) : ProjectEvaluationListener {
    override fun beforeEvaluate(project: Project) {}

    override fun afterEvaluate(project: Project, state: ProjectState) {
        action(project)
    }
}
