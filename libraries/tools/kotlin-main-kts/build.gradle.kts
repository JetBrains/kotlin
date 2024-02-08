import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.internal.jvm.Jvm

description = "Kotlin \"main\" script definition"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val jarBaseName = the<BasePluginExtension>().archivesName

val localPackagesToRelocate =
    listOf(
        "kotlinx.coroutines"
    )

val proguardLibraryJars by configurations.creating {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
    }
}

val embedded by configurations

val relocatedJarContents by configurations.creating {
    extendsFrom(embedded)
}

dependencies {
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":kotlin-scripting-jvm-host-unshaded"))
    compileOnly(project(":kotlin-scripting-dependencies-maven"))
    runtimeOnly(project(":kotlin-scripting-compiler-embeddable"))
    runtimeOnly(kotlinStdlib())
    runtimeOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    embedded(project(":kotlin-scripting-common")) { isTransitive = false }
    embedded(project(":kotlin-scripting-jvm")) { isTransitive = false }
    embedded(project(":kotlin-scripting-jvm-host-unshaded")) { isTransitive = false }
    embedded(project(":kotlin-scripting-dependencies")) { isTransitive = false }
    embedded(project(":kotlin-scripting-dependencies-maven-all")) { isTransitive = false }
    embedded("org.slf4j:slf4j-api:1.7.36")
    embedded("org.slf4j:slf4j-simple:1.7.36")
    embedded(commonDependency("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm")) {
        isTransitive = false
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        }
    }

    proguardLibraryJars(kotlinStdlib())
    proguardLibraryJars(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    proguardLibraryJars(project(":kotlin-compiler"))

    relocatedJarContents(mainSourceSet.output)
}

sourceSets {
    "main" { projectDefault() }
    "test" { }
}

publish()

noDefaultJar()

val relocatedJar by task<ShadowJar> {
    configurations = listOf(relocatedJarContents)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    destinationDirectory.set(layout.buildDirectory.dir("libs"))
    archiveClassifier.set("before-proguard")

    // don't add this files to resources classpath to avoid IDE exceptions on kotlin project
    from("jar-resources")

    if (kotlinBuildProperties.relocation) {
        (packagesToRelocate + localPackagesToRelocate).forEach {
            relocate(it, "$kotlinEmbeddableRootPackage.$it")
        }
    }
}

val proguard by task<CacheableProguardTask> {
    dependsOn(relocatedJar)
    configuration("main-kts.pro")

    injars(mapOf("filter" to "!META-INF/versions/**,!kotlinx/coroutines/debug/**"), relocatedJar.get().outputs.files)

    outjars(layout.buildDirectory.file(jarBaseName.map { "libs/$it-$version-after-proguard.jar" }))

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
    val pack = if (kotlinBuildProperties.proguard) proguard else relocatedJar
    dependsOn(pack)
    setupPublicJar(jarBaseName)
    from {
        zipTree(pack.map { it.singleOutputFile(layout) })
    }
}

setPublishableArtifact(resultJar)
sourcesJar()
javadocJar()
