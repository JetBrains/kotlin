import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.internal.jvm.Jvm

description = "Shaded Maven dependencies resolver"

val JDK_18: String by rootProject.extra
val jarBaseName = property("archivesBaseName") as String

val embedded by configurations

embedded.apply {
    exclude("org.slf4j", "slf4j-api")
    exclude("org.eclipse.aether", "aether-api")
    exclude("org.eclipse.aether", "aether-util")
    exclude("org.eclipse.aether", "aether-spi")
}

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    embedded(project(":kotlin-scripting-dependencies-maven")) { isTransitive = false }
    embedded(project(":kotlin-scripting-dependencies")) { isTransitive = false }

    embedded("org.eclipse.aether:aether-connector-basic:1.1.0")
    embedded("org.eclipse.aether:aether-transport-wagon:1.1.0")
    embedded("org.eclipse.aether:aether-transport-file:1.1.0")
    embedded("org.apache.maven:maven-core:3.8.1")
    embedded("org.apache.maven.wagon:wagon-http:3.4.3")
}

sourceSets {
    "main" {}
    "test" {}
}

publish()

noDefaultJar()
sourcesJar()
javadocJar()

val relocatedJar by task<ShadowJar> {
    configurations = listOf(embedded)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    destinationDirectory.set(File(buildDir, "libs"))
    archiveClassifier.set("before-proguard")

    transform(ComponentsXmlResourceTransformerPatched())

    if (kotlinBuildProperties.relocation) {
        packagesToRelocate.forEach {
            relocate(it, "$kotlinEmbeddableRootPackage.$it")
        }
    }
}

val proguard by task<CacheableProguardTask> {
    dependsOn(relocatedJar)
    configuration("dependencies-maven.pro")

    injars(mapOf("filter" to "!META-INF/versions/**,!kotlinx/coroutines/debug/**"), relocatedJar.get().outputs.files)

    outjars(fileFrom(buildDir, "libs", "$jarBaseName-$version-after-proguard.jar"))

    javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_1_8))

    libraryjars(
        files(
            javaLauncher.map {
                firstFromJavaHomeThatExists(
                    "jre/lib/rt.jar",
                    "../Classes/classes.jar",
                    jdkHome = it.metadata.installationPath.asFile
                )
            },
            javaLauncher.map {
                firstFromJavaHomeThatExists(
                    "jre/lib/jsse.jar",
                    "../Classes/jsse.jar",
                    jdkHome = it.metadata.installationPath.asFile
                )
            },
            javaLauncher.map {
                Jvm.forHome(it.metadata.installationPath.asFile).toolsJar
            }
        )
    )

}

val resultJar by task<Jar> {
    val pack = if (kotlinBuildProperties.proguard) proguard else relocatedJar
    dependsOn(pack)
    setupPublicJar(jarBaseName)
    from {
        zipTree(pack.get().singleOutputFile())
    }
}

addArtifact("runtime", resultJar)
addArtifact("runtimeElements", resultJar)
addArtifact("archives", resultJar)
