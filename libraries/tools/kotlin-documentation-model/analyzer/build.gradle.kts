/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("dokkabuild.base")
    idea
}

val publishedIncludedBuilds = listOf("runner-cli", "dokka-gradle-plugin", "runner-maven-plugin")
val gradlePluginIncludedBuilds = listOf("dokka-gradle-plugin")

addDependencyOnSameTasksOfIncludedBuilds("assemble", "build", "clean", "check")

registerParentGroupTasks(
    "publishing", taskNames = listOf(
        "publishAllPublicationsToMavenCentralRepository",
        "publishAllPublicationsToProjectLocalRepository",
        "publishAllPublicationsToSnapshotRepository",
        "publishAllPublicationsToSpaceDevRepository",
        "publishAllPublicationsToSpaceTestRepository",
        "publishToMavenLocal",
    )
) {
    it.name in publishedIncludedBuilds
}

registerParentGroupTasks(
    "gradle plugin", taskNames = listOf(
        "publishPlugins",
        "validatePlugins",
    )
) {
    it.name in gradlePluginIncludedBuilds
}

registerParentGroupTasks(
    "bcv", taskNames = listOf(
        "apiDump",
        "apiCheck",
        "apiBuild",
    )
) {
    it.name in publishedIncludedBuilds
}

registerParentGroupTasks(
    "verification", taskNames = listOf(
        "test",
    )
)

tasks.register("integrationTest") {
    group = "verification"
    description = "Runs integration tests of this project. Might take a while and require additional setup."

    dependsOn(includedBuildTasks("integrationTest") {
        it.name == "dokka-integration-tests"
    })
}

fun addDependencyOnSameTasksOfIncludedBuilds(vararg taskNames: String) {
    taskNames.forEach { taskName ->
        tasks.named(taskName) {
            dependsOn(includedBuildTasks(taskName))
        }
    }
}

fun registerParentGroupTasks(
    groupName: String,
    taskNames: List<String>,
    includedBuildFilter: (IncludedBuild) -> Boolean = { true }
) = taskNames.forEach { taskName ->
    tasks.register(taskName) {
        group = groupName
        description = "A parent task that calls tasks with the same name in all subprojects and included builds"

        dependsOn(subprojectTasks(taskName), includedBuildTasks(taskName, includedBuildFilter))
    }
}

fun subprojectTasks(taskName: String): List<String> =
    subprojects
        .filter { it.getTasksByName(taskName, false).isNotEmpty() }
        .map { ":${it.path}:$taskName" }


fun includedBuildTasks(taskName: String, filter: (IncludedBuild) -> Boolean = { true }): List<TaskReference> =
    gradle.includedBuilds
        .filter { it.name != "build-logic" && it.name != "build-settings-logic" }
        .filter(filter)
        .mapNotNull { it.task(":$taskName") }


tasks.wrapper {
    doLast {
        // Manually update the distribution URL to use cache-redirector.
        // (Workaround for https://github.com/gradle/gradle/issues/17515)
        propertiesFile.writeText(
            propertiesFile.readText()
                .replace(
                    "https\\://services.gradle.org/",
                    "https\\://cache-redirector.jetbrains.com/services.gradle.org/",
                )
        )
    }
}

idea {
    module {
        // Mark directories as excluded so that they don't appear in IntelliJ's global search.
        excludeDirs.addAll(
            files(
                ".idea",
                ".husky",
                ".kotlin",
                "dokka-runners/dokka-gradle-plugin/.kotlin",
                "dokka-runners/runner-cli/.kotlin",
                "dokka-runners/runner-maven-plugin/.kotlin",
                "dokka-runners/dokka-gradle-plugin/src/testFunctional/resources/KotlinDslAccessorsTest/",

                "dokka-integration-tests/gradle/src/testExampleProjects/expectedData",
            )
        )
    }
}
