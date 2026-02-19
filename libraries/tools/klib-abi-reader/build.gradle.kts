import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.internal.jvm.Jvm

description = "KLIB ABI reader"

plugins {
    kotlin("jvm")
}

val jarBaseName = the<BasePluginExtension>().archivesName

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
    runtimeOnly(kotlinStdlib())
    embedded(project(":kotlin-util-klib-abi")) {
        isTransitive = true
        exclude("org.jetbrains.kotlin", "kotlin-stdlib")
    }
    embedded(libs.guava)
    embedded(libs.intellij.fastutil)

    proguardLibraryJars(kotlinStdlib())
    proguardLibraryJars(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
}

publish()

noDefaultJar()

val relocatedJar by task<ShadowJar> {
    configurations = listOf(embedded)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    destinationDirectory.set(layout.buildDirectory.dir("libs"))
    archiveClassifier.set("before-proguard")

    if (kotlinBuildProperties.relocation) {
        packagesToRelocate.forEach {
            relocate(it, "$kotlinEmbeddableRootPackage.$it")
        }
    }
}

val proguard by task<CacheableProguardTask> {
    dependsOn(relocatedJar)
    configuration("klib-abi-reader.pro")

    injars(mapOf("filter" to "!META-INF/versions/**"), relocatedJar.get().outputs.files)

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
