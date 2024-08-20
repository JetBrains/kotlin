import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
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
    //kotlin("multiplatform")
}

repositories {
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies")
    gradlePluginPortal()
}

tasks.validatePlugins.configure {
    enabled = false
}

sourceSets["main"].withConvention(org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet::class) {
    kotlin.filter.exclude("**/FileCheckTest.kt")
    // TODO: Consider moving required stuff from kotlin-native/build-tools/ to buildSrc/ here.
    kotlin.filter.exclude("**/bitcode/**")
    kotlin.filter.exclude("**/cpp/**")
    kotlin.filter.exclude("**/testing/**")

    kotlin.srcDir("../../../../build-tools/src/main/kotlin")
    kotlin.srcDir("../../../../performance/buildSrc/src/main/kotlin")
    kotlin.srcDir("../../../../shared/src/library/kotlin")
    kotlin.srcDir("../../../../shared/src/main/kotlin")
    kotlin.srcDir("../../../benchmarks/shared/src/main/kotlin/report")
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.optIn.addAll(
        listOf(
                "kotlin.RequiresOptIn",
                "kotlin.ExperimentalStdlibApi"
        )
    )
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-build-gradle-plugin:${kotlinBuildProperties.buildGradlePluginVersion}")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${project.bootstrapKotlinVersion}")
    api("org.jetbrains.kotlin:kotlin-native-utils:${project.bootstrapKotlinVersion}")
    api("org.jetbrains.kotlin:kotlin-util-klib:${project.bootstrapKotlinVersion}")
    compileOnly(gradleApi())
    val kotlinVersion = project.bootstrapKotlinVersion
    val ktorVersion = "1.2.1"
    val slackApiVersion = "1.2.0"
    val shadowVersion = "8.3.0"
    val metadataVersion = "0.0.1-dev-10"

    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("com.ullink.slack:simpleslackapi:$slackApiVersion")

    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")

    api("org.jetbrains.kotlin:kotlin-native-utils:$kotlinVersion")

    // Located in <repo root>/shared and always provided by the composite build.
    //api("org.jetbrains.kotlin:kotlin-native-shared:$konanVersion")
    implementation("com.gradleup.shadow:shadow-gradle-plugin:$shadowVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-metadata-klib:$metadataVersion")
}
