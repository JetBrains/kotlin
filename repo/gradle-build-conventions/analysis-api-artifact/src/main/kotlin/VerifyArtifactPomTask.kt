/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Verifies that the generated Maven POM ([pomFile]) matches the committed golden POM ([expectedPomFile]).
 *
 * Before comparing, every occurrence of the publication's own [artifactVersion] is replaced with a fixed
 * placeholder, so the golden POM doesn't have to be updated whenever the publication version changes. The
 * versions of third-party dependencies differ from [artifactVersion] and are therefore kept verbatim (tracked).
 *
 * If [expectedPomFile] is missing or out of date, it is (re)generated and the task fails — the same convention
 * used by the other "expected file" checks in this repository.
 */
@DisableCachingByDefault(because = "The verification is cheap and not worth caching")
abstract class VerifyArtifactPomTask : DefaultTask() {
    init {
        group = "verification"
    }

    /**
     * The generated POM to verify (`build/publications/<name>/pom-default.xml`).
     */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val pomFile: RegularFileProperty

    /**
     * The publication version. Replaced with a placeholder so the golden POM stays version-independent.
     */
    @get:Input
    abstract val artifactVersion: Property<String>

    /**
     * The committed golden POM. (Re)generated when it is missing or doesn't match, in which case the task fails.
     */
    @get:OutputFile
    abstract val expectedPomFile: RegularFileProperty

    @TaskAction
    fun verify() {
        val version = artifactVersion.get()
        val actualPom = pomFile.get().asFile.readText()
            .replace("<version>$version</version>", "<version>$VERSION_PLACEHOLDER</version>")

        val expectedFile = expectedPomFile.get().asFile

        if (!expectedFile.exists()) {
            expectedFile.writeText(actualPom)
            throw GradleException("'${expectedFile.name}' did not exist and has been generated. Please review and commit it.")
        }

        if (expectedFile.readText().lines() != actualPom.lines()) {
            expectedFile.writeText(actualPom)
            throw GradleException("'${expectedFile.name}' is out of date and has been updated. Please review and commit the changes.")
        }
    }

    private companion object {
        const val VERSION_PLACEHOLDER = "@ARTIFACT_VERSION@"
    }
}
