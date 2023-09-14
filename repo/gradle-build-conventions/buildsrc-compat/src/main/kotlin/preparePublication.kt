import org.gradle.api.Project
import org.gradle.kotlin.dsl.*

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

fun Project.preparePublication() {
    tasks.register("preparePublication") {
        assert(project.version != "unspecified")

        val repositoryProviders = mapOf<String?, String?>(
            "sonatype-nexus-staging" to "sonatype",
            "sonatype-nexus-snapshots" to "sonatype"
        )
        val isRelease: Boolean by extra(!project.version.toString().contains("-SNAPSHOT"))

        val repo: String? = properties["kotlin.build.deploy-repo"]?.toString() ?: properties["deploy-repo"]?.toString()
        val repoProvider = repositoryProviders.getOrDefault(repo, repo)
        val isSonatypePublish: Boolean by extra(repoProvider == "sonatype")
        val isSonatypeRelease: Boolean by extra(isSonatypePublish && isRelease)

        val deployRepoUrl = (properties["kotlin.build.deploy-url"] ?: properties["deploy-url"])?.toString()?.takeIf { it.isNotBlank() }

        val sonatypeSnapshotsUrl = if (isSonatypePublish && !isRelease) {
            "https://oss.sonatype.org/content/repositories/snapshots/"
        } else {
            null
        }
        val deployUrlFromParameters = deployRepoUrl ?: sonatypeSnapshotsUrl

        val repoUrl: String by extra((deployUrlFromParameters ?: "file://${rootProject.buildDir}/repo").toString())
        logger.info("Deployment repository preliminary url: $repoUrl ($repoProvider)")

        val username: String? by extra(
            properties["kotlin.build.deploy-username"]?.toString() ?: properties["kotlin.${repoProvider}.user"]?.toString()
        )
        val password: String? by extra(
            properties["kotlin.build.deploy-password"]?.toString() ?: properties["kotlin.${repoProvider}.password"]?.toString()
        )

        doLast {
            logger.warn("Deployment repository url: $repoUrl")
        }
    }
}
