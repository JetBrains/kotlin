import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.ComponentsXmlResourceTransformer
import org.gradle.internal.jvm.Jvm
import org.gradle.kotlin.dsl.support.serviceOf

description = "Shaded Maven dependencies resolver"

val jarBaseName = property("archivesBaseName") as String

val embedded by configurations

embedded.apply {
    exclude("org.slf4j", "slf4j-api")
}

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val proguardLibraryJars by configurations.creating {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
    }
}

dependencies {
    api(project(":kotlin-scripting-dependencies"))
    proguardLibraryJars(project(":kotlin-scripting-dependencies"))

    embedded(project(":kotlin-scripting-dependencies-maven")) { isTransitive = false }

    embedded("org.apache.maven.resolver:maven-resolver-connector-basic:1.9.2")
    embedded("org.apache.maven.resolver:maven-resolver-transport-file:1.9.2")
    embedded("org.apache.maven.resolver:maven-resolver-transport-wagon:1.9.2")
    embedded("org.apache.maven.resolver:maven-resolver-impl:1.9.2")
    embedded("org.apache.maven:maven-core:3.8.7")
    embedded("org.apache.maven.wagon:wagon-http:3.5.3")
    embedded("commons-io:commons-io:2.11.0")

    testImplementation(libs.junit4)
    testRuntimeOnly("org.slf4j:slf4j-nop:1.7.36")
    testImplementation(project(":kotlin-scripting-dependencies-maven-all"))
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

publish()

noDefaultJar()
sourcesJar()
javadocJar()

val mavenPackagesToRelocate = listOf(
    "org.eclipse",
    "org.codehaus",
    "org.jsoup",
    "afu",
    "org.aopalliance",
    "org.checkerframework",
    "org.sonatype"
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
    val relocatedJarFile = project.provider { relocatedJar.get().singleOutputFile() }
    val archiveOperations = serviceOf<ArchiveOperations>()
    outputs.file(outputFile)

    doFirst {
        val componentsXml = archiveOperations.zipTree(relocatedJarFile.get()).matching {
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

    libraryjars(mapOf("filter" to "!META-INF/versions/**"), proguardLibraryJars)
    libraryjars(
        files(
            javaLauncher.map {
                firstFromJavaHomeThatExists(
                    "jre/lib/rt.jar",
                    "../Classes/classes.jar",
                    jdkHome = it.metadata.installationPath.asFile
                )!!
            },
            javaLauncher.map {
                firstFromJavaHomeThatExists(
                    "jre/lib/jsse.jar",
                    "../Classes/jsse.jar",
                    jdkHome = it.metadata.installationPath.asFile
                )!!
            },
            javaLauncher.map {
                Jvm.forHome(it.metadata.installationPath.asFile).toolsJar!!
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


setPublishableArtifact(resultJar)
