import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.gradle.api.GradleException
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

        val repo: String? = properties["deployRepo"]?.toString() ?: properties["deploy-repo"]?.toString()
        val repoProvider = repositoryProviders.getOrDefault(repo, repo)
        val isSonatypePublish: Boolean by extra(repoProvider == "sonatype")
        val isSonatypeRelease: Boolean by extra(isSonatypePublish && isRelease)

        val deployRepoUrl = properties["deployRepoUrl"]?.toString() ?: properties["deploy-url"]?.toString()
        val deployFolder = properties["deployRepoFolder"]?.toString()
            ?.let { "file://${rootProject.buildDir}/$it" }
        val sonatypeSnapshotsUrl = if (isSonatypePublish && !isRelease) {
            "https://oss.sonatype.org/content/repositories/snapshots/"
        } else {
            null
        }
        val deployUrlFromParameters = deployRepoUrl ?: deployFolder ?: sonatypeSnapshotsUrl

        val isDeployStagingRepoGenerationRequired: Boolean by extra(isSonatypeRelease && deployUrlFromParameters == null)

        var repoUrl: String by extra((deployUrlFromParameters ?: "file://${rootProject.buildDir}/repo").toString())
        logger.info("Deployment repository preliminary url: $repoUrl ($repoProvider)")

        val username: String? by extra(
            properties["deployRepoUsername"]?.toString() ?: properties["kotlin.${repoProvider}.user"]?.toString()
        )
        val password: String? by extra(
            properties["deployRepoPassword"]?.toString() ?: properties["kotlin.${repoProvider}.password"]?.toString()
        )

        if (isDeployStagingRepoGenerationRequired) {
            doFirst {
                HttpClient().use { client ->
                    runBlocking {
                        val sonatypeUsername = requireNotNull(username) {
                            "Username to authenticate on sonatype staging was not provided!"
                        }
                        val sonatypePassword = requireNotNull(password) {
                            "Password to authenticate on sonatype staging was not provided!"
                        }

                        val response = client.post("https://oss.sonatype.org/service/local/staging/profiles/169b36e205a64e/start") {
                            basicAuth(sonatypeUsername, sonatypePassword)
                            contentType(ContentType.Application.Xml)
                            accept(ContentType.Application.Xml)
                            setBody("<promoteRequest><data><description>Repository for publishing $version</description></data></promoteRequest>")
                        }

                        if (response.status.value in 200..299) {
                            val responseText = response.bodyAsText()
                            val repoId = responseText
                                .substringAfter("<stagingRepositoryId>")
                                .substringBefore("</stagingRepositoryId>")
                            repoUrl = "https://oss.sonatype.org/service/local/staging/deployByRepositoryId/$repoId/"
                            logger.warn("##teamcity[setParameter name='system.deploy-url' value='$repoUrl']")
                        } else {
                            throw GradleException("Failed to connect to sonatype API: ${response.status.description}")
                        }
                    }
                }
            }
        }

        doLast {
            logger.warn("Deployment repository url: $repoUrl")
        }
    }
}
