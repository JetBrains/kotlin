import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties
import java.io.FileReader

extra["versions.native-platform"] = "0.14"

buildscript {
    java.util.Properties().also {
        it.load(java.io.FileReader(project.file("../../../../../gradle.properties")))
    }.forEach { k, v->
        val key = k as String
        val value = project.findProperty(key) ?: v
        extra[key] = value
    }

    val cacheRedirectorEnabled = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() ?: false

    extra["defaultSnapshotVersion"] = kotlinBuildProperties.defaultSnapshotVersion
    kotlinBootstrapFrom(BootstrapOption.SpaceBootstrap(kotlinBuildProperties.kotlinBootstrapVersion!!, cacheRedirectorEnabled))
    extra["bootstrapKotlinRepo"] = project.bootstrapKotlinRepo
    extra["bootstrapKotlinVersion"] = project.bootstrapKotlinVersion

    repositories {
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies")
        jcenter()
        project.bootstrapKotlinRepo?.let {
            maven(url = it)
        }
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-build-gradle-plugin:0.0.29")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${project.bootstrapKotlinVersion}")
    }
}

apply{
    plugin("kotlin")
    plugin("kotlin-sam-with-receiver")
}
plugins {
    `kotlin-dsl`
    //kotlin("multiplatform") version "${project.bootstrapKotlinVersion}"
}

val cacheRedirectorEnabled = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true
repositories {
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies")
    jcenter()
    gradlePluginPortal()
    extra["bootstrapKotlinRepo"]?.let {
        maven(url = it)
    }
}

tasks.validatePlugins.configure {
    enabled = false
}


sourceSets["main"].withConvention(org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet::class) {
    kotlin.srcDir("../../../../build-tools/src/main/kotlin")
    kotlin.srcDir("../../../../performance/buildSrc/src/main/kotlin")
    kotlin.srcDir("../../../../shared/src/library/kotlin")
    kotlin.srcDir("../../../../shared/src/main/kotlin")
    kotlin.srcDir("../../../benchmarks/shared/src/main/kotlin/report")
    kotlin.srcDir("../../../../../native/utils/src")
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs +=
        listOf("-Xopt-in=kotlin.RequiresOptIn",
               "-Xskip-runtime-version-check",
               "-Xopt-in=kotlin.ExperimentalStdlibApi")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-build-gradle-plugin:0.0.29")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${project.bootstrapKotlinVersion}")
    api("org.jetbrains.kotlin:kotlin-native-utils:${project.bootstrapKotlinVersion}")
    api("org.jetbrains.kotlin:kotlin-util-klib:${project.bootstrapKotlinVersion}")
    compileOnly(gradleApi())
    val kotlinVersion = project.bootstrapKotlinVersion
    val ktorVersion = "1.2.1"
    val slackApiVersion = "1.2.0"
    val shadowVersion = "5.1.0"
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
    implementation("com.github.jengelman.gradle.plugins:shadow:$shadowVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-metadata-klib:$metadataVersion")
}
