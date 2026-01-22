/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.jacoco
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.kotlin.dsl.withType
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension

plugins {
    jacoco
}

val versionCatalog = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
val jacocoToolVersion = versionCatalog.findVersion("jacoco")
val jacocoCliDependency = versionCatalog.findLibrary("jacoco-cli").get()

jacoco {
    toolVersion = jacocoToolVersion.get().requiredVersion
}

val kgpTestCoverageEnabled: Boolean =
    providers.gradleProperty("kgp.jacoco.enabled").orNull?.toBoolean() ?: false

val jacocoCliClasspath = configurations.dependencyScope("jacocoCliClasspath")

dependencies {
    jacocoCliClasspath(jacocoCliDependency.get())
}

val jacocoCliClasspathResolver = configurations.resolvable(jacocoCliClasspath.name + "Resolver") {
    extendsFrom(jacocoCliClasspath)
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
    }
}

tasks.withType<Test>().configureEach {
    extensions.configure<JacocoTaskExtension> {
        isEnabled = kgpTestCoverageEnabled
    }
    // Don't fail the build on test failures when collecting coverage: JaCoCo writes the .exec on
    // shutdown regardless of test outcome, and the dependent coverage report task is more useful
    // than the failure signal in this mode.
    if (kgpTestCoverageEnabled) ignoreFailures = true
}

if (kgpTestCoverageEnabled) {
    // `mainSourceElements` (consumed by jacoco-report-aggregation for source discovery) doesn't
    // include the `common` source set by default — add it so it shows up in reports.
    plugins.withId("java") {
        configurations.findByName("mainSourceElements")?.let { mainSourceElements ->
            sourceSets.findByName("common")?.allSource?.srcDirs?.forEach { srcDir ->
                mainSourceElements.outgoing.artifact(srcDir) {
                    type = ArtifactTypeDefinition.DIRECTORY_TYPE
                }
            }
        }
    }

    tasks.withType<Jar>() {
        val actualOutputFile = destinationDirectory.file(archiveFileName)

        val jacocoCli = jacocoCliClasspathResolver.map { it.incoming.files }
        inputs.files(jacocoCli)
            .withNormalizer(ClasspathNormalizer::class)

        val execOps = serviceOf<ExecOperations>()
        val fs = serviceOf<FileSystemOperations>()

        val jacocoInstrumentOutputDir = temporaryDir.resolve("jacoco-instrument")

        doLast {
            val archiveFileName = archiveFileName.get()
            val actualOutputFile = actualOutputFile.get().asFile

            fs.delete { delete(jacocoInstrumentOutputDir) }
            jacocoInstrumentOutputDir.mkdirs()

            actualOutputFile.copyTo(jacocoInstrumentOutputDir.resolve("$archiveFileName.original.jar"))

            execOps.javaexec {
                mainClass.set("org.jacoco.cli.internal.Main")
                classpath(jacocoCli)
                args(
                    "instrument",
                    actualOutputFile.absolutePath,
                    "--dest",
                    jacocoInstrumentOutputDir,
                )
            }

            fs.copy {
                from(jacocoInstrumentOutputDir.resolve(archiveFileName))
                into(destinationDirectory)
            }
        }
    }
}
