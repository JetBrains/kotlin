import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

sourceSets {
    "main" { none() }
    "test" { none() }
}

publish(sbom = false)
standardPublicJars()

// Libraries packed into the JAR have to be listed in the SBOM as well
configureSbom(gradleConfigurations = setOf("runtimeClasspath", "embedded"))

configurations {
    val artifactContent = dependencyScope("artifactContent")

    resolvable("artifactContentElements") {
        extendsFrom(artifactContent)
    }

    embedded {
        extendsFrom(artifactContent)
    }
}

val artifactExtension = extensions.create<AnalysisApiArtifactExtension>("analysisApiArtifact")

val verifyBinariesArtifact = tasks.register<VerifyArtifactContentTask>("verifyBinariesArtifact") {
    description = "Verifies that the binaries JAR has the expected content"
    artifact.set(tasks.named<Jar>("jar").flatMap { it.archiveFile })
    artifactName.set("Binaries JAR")
    expectNonEmpty.set(artifactExtension.expectNonEmptyBinaries)
}

val verifySourcesArtifact = tasks.register<VerifyArtifactContentTask>("verifySourcesArtifact") {
    description = "Verifies that the sources JAR has the expected content"
    artifact.set(tasks.named<Jar>("sourcesJar").flatMap { it.archiveFile })
    artifactName.set("Sources JAR")
    expectNonEmpty.set(artifactExtension.expectNonEmptySources)
}

val verifyJavadocArtifact = tasks.register<VerifyArtifactContentTask>("verifyJavadocArtifact") {
    description = "Verifies that the Javadoc JAR is empty"
    artifact.set(tasks.named<Jar>("javadocJar").flatMap { it.archiveFile })
    artifactName.set("Javadoc JAR")
    expectNonEmpty.set(false)
}

tasks.named("check").configure {
    dependsOn(verifyBinariesArtifact, verifySourcesArtifact, verifyJavadocArtifact)
}
