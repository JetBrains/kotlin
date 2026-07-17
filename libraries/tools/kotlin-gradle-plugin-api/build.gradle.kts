import gradle.GradlePluginVariant

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    id("gradle-plugin-dependency-configuration")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
    id("gradle-plugin-api-reference")
    id("generated-sources")
}

pluginApiReference {
    enableForAllGradlePluginVariants()

    failOnWarning = true

    additionalDokkaConfiguration {
        dokkaSourceSets.configureEach {
            if (name != "common") {
                suppress = true
                return@configureEach
            }

            reportUndocumented = true
            includes.from("api-reference-description.md")
        }
    }

    embeddedProject(project.dependencies.project(":kotlin-gradle-compiler-types"))
}

dependencies {
    commonApi(platform(project(":kotlin-gradle-plugins-bom")))
    commonApi(project(":kotlin-gradle-plugin-annotations"))
    commonApi(project(":native:kotlin-native-utils")) { // TODO: consider removing in KT-70247
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-util-klib")
    }
    commonApi(project(":kotlin-tooling-core"))
    commonApi(project(":compiler:build-tools:kotlin-build-tools-api"))

    commonCompileOnly(project(":kotlin-gradle-compiler-types"))

    embedded(project(":kotlin-gradle-compiler-types")) { isTransitive = false }
}

apiValidation {
    nonPublicMarkers += "org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi"
}

tasks {
    apiBuild {
        inputJar.value(jar.flatMap { it.archiveFile })
    }
}

registerKotlinSourceForVersionRange(
    GradlePluginVariant.GRADLE_MIN,
    GradlePluginVariant.GRADLE_88,
)

generatedSourcesTask(
    taskName = "generateKotlinVersionConstant",
    generatorProject = ":gradle:generators:native-cache-kotlin-version",
    generatorMainClass = "org.jetbrains.kotlin.gradle.generators.native.cache.version.MainKt",
    generatedSourceSetKind = GeneratedSourceSetKind.JvmCommon,
    argsProvider = { generationRoot ->
        listOf(
            generationRoot.toString(),
            version.toString(),
            layout.projectDirectory.file("native-cache-kotlin-versions.txt").toString(),
        )
    }
).configure {
    // 1. Trigger apiDump after generation
    finalizedBy("apiDump")
}

// 2. Resolve implicit dependency conflict
// apiDump writes the file, apiCheck reads it. If both run, Dump must run first.
tasks.named("apiCheck").configure {
    mustRunAfter("apiDump")
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.apache.commons" && requested.name == "commons-lang3") {
            useVersion(libs.versions.commons.lang.get())
            because("CVE-2025-48924")
        }
    }
}

tasks.withType<Jar>().configureEach {
    if (name.endsWith("Jar") || name == "jar") {
        // FIXME: Entry org/jetbrains/kotlin/gradle/dsl/KotlinDependencies.class is a duplicate
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}
