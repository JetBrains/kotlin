import org.gradle.api.Project
import org.gradle.kotlin.dsl.*

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

fun Project.preparePublication() {
    tasks.register("preparePublication") {
        assert(project.version != "unspecified")

        val isRelease: Boolean by extra(!project.version.toString().contains("-SNAPSHOT"))

        val repo: String? = properties["kotlin.build.deploy-repo"]?.toString() ?: properties["deploy-repo"]?.toString()
        val repoProvider = when (repo) {
            "sonatype-nexus-staging" -> "sonatype"
            "sonatype-nexus-snapshots" -> "sonatype"
            else -> repo
        }

        val deployRepoUrl = (properties["kotlin.build.deploy-url"] ?: properties["deploy-url"])?.toString()?.takeIf { it.isNotBlank() }

        val sonatypeSnapshotsUrl = "https://oss.sonatype.org/content/repositories/snapshots/".takeIf { repo == "sonatype-nexus-snapshots" }

        val repoUrl: String by extra(
            (deployRepoUrl ?: sonatypeSnapshotsUrl ?: "file://${
                rootProject.layout.buildDirectory.dir("repo").get().asFile
            }").toString()
        )

        val username: String? by extra(
            properties["kotlin.build.deploy-username"]?.toString() ?: properties["kotlin.${repoProvider}.user"]?.toString()
        )
        val password: String? by extra(
            properties["kotlin.build.deploy-password"]?.toString() ?: properties["kotlin.${repoProvider}.password"]?.toString()
        )

        doLast {
            logger.warn(
                "Deploy. url: $repoUrl, provider: $repoProvider, username: $username, password: ${"***".takeIf { password != null }}"
            )
        }
    }
}
