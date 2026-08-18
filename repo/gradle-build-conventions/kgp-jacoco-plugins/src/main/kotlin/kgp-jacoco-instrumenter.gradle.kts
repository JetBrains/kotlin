/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.kotlin.dsl.withType

val versionCatalog = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
val jacocoCliDependency = versionCatalog.findLibrary("jacoco-cli").get()

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

if (kotlinBuildProperties.kgpTestCoverageEnabled.get()) {
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

    tasks.withType<Jar>()
        // different tasks used in kgp and kgp-api, but don't want to instrument gradle variants
        .matching { it.name == "jar" || it.name == "embeddableJar" }
        .configureEach {
            val jacocoCli = jacocoCliClasspathResolver.map { it.incoming.files }
            inputs.files(jacocoCli)
                .withNormalizer(ClasspathNormalizer::class)

            val execOps = serviceOf<ExecOperations>()
            val fs = serviceOf<FileSystemOperations>()

            val jacocoInstrumentOutputDir = temporaryDir.resolve("jacoco-instrument")

            doLast {
                val archiveFileName = archiveFileName.get()
                val actualOutputFile = archiveFile.get().asFile

                fs.delete { delete(jacocoInstrumentOutputDir) }
                jacocoInstrumentOutputDir.mkdirs()

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
