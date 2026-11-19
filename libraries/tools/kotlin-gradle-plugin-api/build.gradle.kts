import gradle.GradlePluginVariant

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    id("gradle-plugin-dependency-configuration")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
    id("gradle-plugin-api-reference")
    id("generated-sources")
    id("kgp-jacoco-on-the-fly")
    id("kgp-jacoco-instrumenter")
    id("asm-deprecating-transformer")
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
    // kotlin-native-utils is bundled into this jar instead of published as a dependency (TODO: remove in KT-70247),
    // so that HostManager can be deprecated for build scripts below. Its own dependency stays a dependency.
    commonCompileOnly(project(":native:kotlin-native-utils"))
    commonApi(project(":kotlin-util-io"))
    commonApi(project(":kotlin-tooling-core"))
    commonApi(project(":build-tools:libraries:native-host"))
    commonApi(project(":compiler:build-tools:kotlin-build-tools-api"))

    commonCompileOnly(project(":kotlin-gradle-compiler-types"))

    embedded(project(":kotlin-gradle-compiler-types")) { isTransitive = false }
    embedded(project(":native:kotlin-native-utils")) { isTransitive = false }
}

// HostManager is internal to Kotlin/Native, but build scripts reach it through this module. Deprecating the bundled
// copy warns them without changing the class for Kotlin/Native itself.
asmDeprecation {
    val embeddedConfigurations = listOf(configurations.getByName("embedded")) + GradlePluginVariant.values().map { variant ->
        configurations.getByName("${sourceSets.getByName(variant.sourceSetName).name}Embedded")
    }
    deprecateClassesByPattern(
        inputConfigurations = embeddedConfigurations,
        pattern = "org.jetbrains.kotlin.konan.target.HostManager*",
        deprecationMessage = """
            HostManager is internal to Kotlin/Native and will be hidden from build scripts in a future Kotlin release.
            Use org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHost instead: KotlinNativeHost.current(), or kotlin.nativeHost
            in a multiplatform build script, and kotlin.hostNativeTarget() to declare the host target.
            See https://youtrack.jetbrains.com/issue/KT-74608 for details.
        """.trimIndent(),
    )
}

GradlePluginVariant.values().forEach { variant ->
    val jarTaskName = sourceSets.getByName(variant.sourceSetName).jarTaskName
    val taskSuffix = jarTaskName.replaceFirstChar { it.uppercase() }
    asmDeprecation {
        tasks.registerDumpDeprecationsTask(jarTaskName, taskSuffix)
        val checkTask = tasks.registerCheckDeprecationsTask(
            jarTaskName,
            taskSuffix,
            expectedFileDoesNotExistMessage = "Run ./gradlew ${project.path}:dumpDeprecationsFor$taskSuffix to create the expected deprecations.",
            checkFailureMessage = """
                The classes deprecated in the jar of ${project.path} changed. Only HostManager from kotlin-native-utils should be deprecated.
                Run ./gradlew ${project.path}:dumpDeprecationsFor$taskSuffix to see the difference. Context: https://youtrack.jetbrains.com/issue/KT-74608
            """.trimIndent(),
        )
        tasks.named("check") { dependsOn(checkTask) }
    }
}

apiValidation {
    nonPublicMarkers += "org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi"
    // Bundled kotlin-native-utils classes are that module's API, not this one's
    ignoredPackages += listOf("org.jetbrains.kotlin.konan", "org.jetbrains.kotlin.backend.konan")
}

tasks {
    apiBuild {
        inputJar.value(jar.flatMap { it.archiveFile })
    }
}

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
    },
    versionDependent = true,
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
