import org.gradle.api.publish.internal.PublicationInternal
import plugins.KotlinBuildPublishingPlugin.Companion.ADHOC_COMPONENT_NAME
import plugins.configureKotlinPomAttributes
import plugins.signLibraryPublication

description = "Annotation Processor for Kotlin (for using with embeddable compiler)"

plugins {
    `java-library`
}

dependencies {
    embedded(project(":kotlin-annotation-processing")) { isTransitive = false }
}

publish()

// Special compat publication for Kapt/Gradle until we will have minimal
// supported IDEA/Kotlin plugin version 1.9.0
val publications: PublicationContainer = extensions.getByType<PublishingExtension>().publications
val gradleCompatPublication = publications.register<MavenPublication>("gradleCompat") {
    artifactId = "kotlin-annotation-processing-gradle"
    from(components[ADHOC_COMPONENT_NAME])

    // Workaround for https://github.com/gradle/gradle/issues/12324
    (this as PublicationInternal<*>).isAlias = true
    configureKotlinPomAttributes(project)
}
if (signLibraryPublication) {
    tasks.named("publishGradleCompatPublicationToMavenLocal").configure {
        dependsOn("signMainPublication")
    }
    tasks.named("publishGradleCompatPublicationToMavenRepository").configure {
        dependsOn("signMainPublication")
    }
    tasks.named("publishMainPublicationToMavenLocal").configure {
        dependsOn("signGradleCompatPublication")
    }
    tasks.named("publishMainPublicationToMavenRepository").configure {
        dependsOn("signGradleCompatPublication")
    }
}

val targetName = "${gradleCompatPublication.name.capitalize()}Publication"
configureSbom(
    target = targetName,
    documentName = "kotlin-annotation-processing-gradle",
    publication = gradleCompatPublication
)

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())
sourcesJar()
javadocJar()
