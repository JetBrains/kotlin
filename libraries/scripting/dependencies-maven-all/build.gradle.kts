import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.ComponentsXmlResourceTransformer
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

val mavenPackagesToRelocate = listOf(
    "org.eclipse",
    "org.codehaus",
)

val relocatedJar by task<ShadowJar> {
    configurations = listOf(embedded)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    destinationDirectory.set(File(buildDir, "libs"))
    archiveClassifier.set("relocated")

    transform(ComponentsXmlResourceTransformer())

    if (kotlinBuildProperties.relocation) {
        (packagesToRelocate + mavenPackagesToRelocate).forEach {
            relocate(it, "$kotlinEmbeddableRootPackage.$it")
        }
    }
}

val normalizeComponentsXmlEndings by tasks.registering {
    dependsOn(relocatedJar)
    val outputDirectory = buildDir.resolve(name)
    val outputFile = outputDirectory.resolve(ComponentsXmlResourceTransformer.COMPONENTS_XML_PATH)
    outputs.file(outputFile)

    doFirst {
        val componentsXml = zipTree(relocatedJar.get().singleOutputFile()).matching {
            include { it.path == ComponentsXmlResourceTransformer.COMPONENTS_XML_PATH }
        }.single().readText()
        val processedComponentsXml = componentsXml.replace("\r\n", "\n")
        outputDirectory.mkdirs()
        outputFile.writeText(processedComponentsXml)
    }
}

val normalizedJar by task<Jar> {
    dependsOn(relocatedJar)
    dependsOn(normalizeComponentsXmlEndings)

    archiveClassifier.set("normalized")

    from {
        zipTree(relocatedJar.get().singleOutputFile()).matching {
            exclude(ComponentsXmlResourceTransformer.COMPONENTS_XML_PATH)
        }
    }

    into(ComponentsXmlResourceTransformer.COMPONENTS_XML_PATH.substringBeforeLast("/")) {
        from {
            normalizeComponentsXmlEndings.get().singleOutputFile()
        }
    }
}

val proguard by task<CacheableProguardTask> {
    dependsOn(normalizedJar)
    configuration("dependencies-maven.pro")

    injars(mapOf("filter" to "!META-INF/versions/**,!kotlinx/coroutines/debug/**"), normalizedJar.get().outputs.files)

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
    val pack = if (kotlinBuildProperties.proguard) proguard else normalizedJar
    dependsOn(pack)
    setupPublicJar(jarBaseName)
    from {
        zipTree(pack.get().singleOutputFile())
    }
}


addArtifact("runtime", resultJar)
addArtifact("runtimeElements", resultJar)
addArtifact("archives", resultJar)
