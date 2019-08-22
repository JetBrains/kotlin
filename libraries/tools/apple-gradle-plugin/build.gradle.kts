import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val pluginName = "applePlugin"
group = "org.jetbrains.gradle.apple"
version = "0.1"

plugins {
    kotlin("jvm")
    `kotlin-dsl`
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "5.1.0"
}

val assemble by tasks
val jar: Jar by tasks
val shadow: Configuration by configurations.getting
val shadowJar by tasks.getting(ShadowJar::class) {
    archiveClassifier.set(null as String?)
}

val clionVersion: String by rootProject.extra
val clionCocoaCommonPluginDir: File by rootProject.extra
val intellijVersion = rootProject.extra["versions.intellijSdk"] as String
val isSnapshotIntellij = intellijVersion.endsWith("SNAPSHOT")
val intellijRepo = "https://www.jetbrains.com/intellij-repository/" + if (isSnapshotIntellij) "snapshots" else "releases"
val intellijCoreDir = "$buildDir/intellij-core"
val intellijCoreZip by configurations.creating

repositories {
    maven(intellijRepo)
    maven("https://jetbrains.bintray.com/intellij-third-party-dependencies/")
    google()
}

val unzipIntellijCore by tasks.registering(Sync::class) {
    dependsOn(intellijCoreZip)
    from(intellijCoreZip.map { zipTree(it) })
    into(intellijCoreDir)
}

val intellijCoreLibs = arrayOf("intellij-core", "guava", "jdom", "log4j", "jna-platform", "jna", "annotations", "trove4j", "picocontainer")

dependencies {
    shadow(gradleApi())

    shadow("com.jetbrains.intellij.platform:core:$intellijVersion") // { isTransitive = false }
    shadow("com.jetbrains.intellij.platform:util:$intellijVersion") // { isTransitive = false }
    shadow("com.jetbrains.intellij.platform:util-rt:$intellijVersion") // { isTransitive = false }
    shadow("com.jetbrains.intellij.platform:util-class-loader:$intellijVersion") // { isTransitive = false }
    shadow("com.jetbrains.intellij.platform:util-ex:$intellijVersion") // { isTransitive = false }
    shadow("com.jetbrains.intellij.platform:extensions:$intellijVersion") // { isTransitive = false }
    //shadow("com.jetbrains.intellij.platform:resources.en:$intellijVersion") // { isTransitive = false }
    shadow("com.jetbrains.intellij.platform:core-impl:$intellijVersion") // { isTransitive = false }

    //intellijCoreZip(files("/Users/florian.kistner/Coding/192/community/out/idea-ce/artifacts/intellij-core-192.SNAPSHOT.zip")) //"com.jetbrains.intellij.idea:intellij-core:$intellijVersion")
    //compile(fileTree(intellijCoreDir) {
    //    include(*intellijCoreLibs.map { "$it*.jar" }.toTypedArray())
    //    builtBy(unzipIntellijCore)
    //})

    compile(fileTree(File(clionCocoaCommonPluginDir, "lib")) { include("*-core.jar") })
}

jar.enabled = false
assemble.dependsOn(shadowJar)

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.languageVersion = "1.3"
        kotlinOptions.apiVersion = "1.3"
        kotlinOptions.freeCompilerArgs += listOf("-Xskip-metadata-version-check", "-Xjvm-default=enable")
    }

    named<ValidateTaskProperties>("validateTaskProperties") {
        failOnWarning = true
    }
}

gradlePlugin {
    isAutomatedPublishing = false

    val applePlugin by plugins.registering {
        id = "$group.$pluginName"
        implementationClass = "ApplePlugin"
    }
}

publishing {
    repositories {
        maven {
            url = uri("$buildDir/repo")
        }
    }

    val applePlugin by publications.registering(MavenPublication::class) {
        artifactId = pluginName
        project.shadow.component(this)
        artifacts.single().classifier = null
    }

    val applePluginMarker by publications.registering(MavenPublication::class) {
        groupId = "$group.$pluginName"
        artifactId = "$groupId.gradle.plugin"
        pom.withXml {
            val plugin = applePlugin.get()
            asNode().appendNode("dependencies").appendNode("dependency").run {
                appendNode("groupId", plugin.groupId)
                appendNode("artifactId", plugin.artifactId)
                appendNode("version", plugin.version)
            }
        }
    }
}