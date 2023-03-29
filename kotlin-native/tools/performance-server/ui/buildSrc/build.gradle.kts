import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties
import java.io.FileReader

buildscript {
    java.util.Properties().also {
        it.load(java.io.FileReader(project.file("../../../../../gradle.properties")))
    }.forEach { k, v->
        val key = k as String
        val value = project.findProperty(key) ?: v
        extra[key] = value
    }

    extra["defaultSnapshotVersion"] = kotlinBuildProperties.defaultSnapshotVersion
    extra["bootstrapKotlinRepo"] = project.bootstrapKotlinRepo
    extra["bootstrapKotlinVersion"] = project.bootstrapKotlinVersion

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-build-gradle-plugin:${kotlinBuildProperties.buildGradlePluginVersion}")
    }
}

plugins {
    `kotlin-dsl`
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.sam.with.receiver")
}

repositories {
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies")
    gradlePluginPortal()
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

sourceSets["main"].kotlin {
    srcDir("../../../../performance/buildSrc/src/main/kotlin")
    srcDir("../../../benchmarks/shared/src/main/kotlin/report")
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.options
            .optIn.addAll("kotlin.RequiresOptIn", "kotlin.ExperimentalStdlibApi")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-build-gradle-plugin:${kotlinBuildProperties.buildGradlePluginVersion}")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${project.bootstrapKotlinVersion}")
    api("org.jetbrains.kotlin:kotlin-native-utils:${project.bootstrapKotlinVersion}")
    api("org.jetbrains.kotlin:kotlin-util-klib:${project.bootstrapKotlinVersion}")
    compileOnly(gradleApi())
}
