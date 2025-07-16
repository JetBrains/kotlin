import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
}

repositories {
    if (!kotlinBuildProperties.isTeamcityBuild) {
        androidXMavenLocal(androidXMavenLocalPath)
    }
    androidxSnapshotRepo(composeRuntimeSnapshot.versions.snapshot.id.get())
    composeGoogleMaven(libs.versions.compose.stable.get())
}

dependencies {
    implementation(project(":kotlin-stdlib"))
    implementation("org.ow2.asm:asm-tree:9.7")
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        apiVersion.set(KotlinVersion.KOTLIN_2_0)
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
    }
}


base {
    archivesName = "compose-mapping-generator"
}

publish {
    artifactId = "compose-mapping-generator"
    pom {
        name.set("AndroidX Compose Mapping Generator")
        developers {
            developer {
                name.set("The Android Open Source Project")
            }
        }
    }
}

runtimeJar()
sourcesJar()
javadocJar()