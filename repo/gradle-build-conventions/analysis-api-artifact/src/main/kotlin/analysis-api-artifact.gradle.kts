import org.gradle.api.publish.maven.tasks.GenerateMavenPom
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import plugins.mainPublicationName

standardPublicJars()

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

analysisApiPublishingLatch {
    val propertyName = "analysis.api.version"
    version = when (val rawVersion = rootProject.findProperty(propertyName)?.toString()) {
        null -> error("Analysis API version isn't configured: pass the '$propertyName' property")
        "kotlin.version" -> project.kotlinBuildProperties.kotlinVersion.get()
        else -> rawVersion
    }

    sourceSets {
        "main" { none() }
        "test" { none() }
    }

    publish()

    val verifyArtifactPom = tasks.register<VerifyArtifactPomTask>("verifyArtifactPom") {
        description = "Verifies that the generated POM matches the committed 'expected-pom.xml'"

        val generateMavenPomTaskName = "generatePomFileFor${mainPublicationName}Publication"
        pomFile.set(layout.file(tasks.named<GenerateMavenPom>(generateMavenPomTaskName).map { it.destination }))
        artifactVersion.set(version.toString())
        expectedPomFile.set(layout.projectDirectory.file("expected-pom.xml"))
    }

    tasks.named("check").configure {
        dependsOn(verifyArtifactPom)
    }
}
