import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.kotlin.dsl.apiBuild
import org.gradle.kotlin.dsl.invoke

description = "Kotlin JVM metadata manipulation library"
group = "org.jetbrains.kotlin"

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

val embedded = configurations.embedded.get()
embedded.isTransitive = false
configurations.compileOnly.get().extendsFrom(embedded)

val proguardLibraryJars = configurations.create("proguardLibraryJars") {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
    }
}

dependencies {
    api(kotlinStdlib())
    embedded(project(":kotlin-metadata-jvm")) { isTransitive = false }
    embedded(project(":kotlin-metadata"))
    embedded(project(":core:metadata"))
    embedded(project(":core:metadata.jvm"))
    embedded(protobufLite())

    proguardLibraryJars(kotlinStdlib())
}

publish()

val relocatedJar = tasks.register<ShadowJar>("relocatedJar") {
    configurations = listOf(embedded)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    destinationDirectory.set(layout.buildDirectory.dir("libs"))
    archiveClassifier.set("shadow")

    exclude("**/*.proto")
    relocate("org.jetbrains.kotlin", "kotlin.metadata.internal")
}

val proguard = tasks.register<CacheableProguardTask>("proguard") {
    dependsOn(relocatedJar)

    injars(mapOf("filter" to "!META-INF/versions/**"), relocatedJar.get().outputs.files)
    outjars(fileFrom(base.libsDirectory.asFile.get(), "${base.archivesName.get()}-$version-proguard.jar"))

    javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_1_8))

    libraryjars(mapOf("filter" to "!META-INF/versions/**"), proguardLibraryJars)
    libraryjars(
        project.files(
            javaLauncher.map {
                firstFromJavaHomeThatExists(
                    "jre/lib/rt.jar",
                    "../Classes/classes.jar",
                    jdkHome = it.metadata.installationPath.asFile
                )!!
            }
        )
    )

    configuration("metadata.pro")
}

val resultJar = tasks.register<Jar>("resultJar") {
    val pack = if (kotlinBuildProperties.proguard) proguard else relocatedJar
    dependsOn(pack)
    setupPublicJar(base.archivesName.get())
    from {
        zipTree(pack.get().singleOutputFile(layout))
    }

    manifest {
        attributes("Automatic-Module-Name" to "kotlin.metadata.jvm")
    }
}

setPublishableArtifact(resultJar)

tasks.apiBuild {
    dependsOn(resultJar)
    inputJar.value(resultJar.flatMap { it.archiveFile })
}

apiValidation {
    ignoredPackages.add("kotlin.metadata.internal")
    nonPublicMarkers.add("kotlin.metadata.internal.IgnoreInApiDump")
}

sourcesJar()

javadocJar()
