@file:Suppress("UNUSED_VARIABLE")

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import org.gradle.api.publish.internal.PublicationInternal
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.GenerateProjectStructureMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink
import org.jetbrains.kotlin.library.KOTLINTEST_MODULE_NAME
import plugins.configureDefaultPublishing
import plugins.configureKotlinPomAttributes
import plugins.publishing.configureMultiModuleMavenPublishing

plugins {
    kotlin("multiplatform")
    `maven-publish`
    signing
    id("nodejs-cache-redirector-configuration")
    id("binaryen-configuration")
}

description = "Kotlin Test Library"
base.archivesName = "kotlin-test"

configureJvmToolchain(JdkMajorVersion.JDK_1_8)

val kotlinTestCapability = "$group:${base.archivesName.get()}:$version" // add to variants with explicit capabilities when the default one is needed, too
val baseCapability = "$group:kotlin-test-framework:$version"
val implCapability = "$group:kotlin-test-framework-impl:$version"

enum class JvmTestFramework {
    JUnit,
    JUnit5,
    TestNG;

    fun lowercase() = name.lowercase()
}
val jvmTestFrameworks = JvmTestFramework.values().toList()

kotlin {

    explicitApi()

    jvm {
        compilations {
            all {
                compileTaskProvider.configure {
                    compilerOptions {
                        freeCompilerArgs.empty() // avoid common options set from the root project
                        freeCompilerArgs.addAll("-Xdont-warn-on-error-suppression")
                    }
                }
            }
            val main by getting
            val test by getting
            configureJava9Compilation(
                "kotlin.test",
                listOf(main.output.allOutputs),
                main.configurations.compileDependencyConfiguration,
                project.sourceSets.create("jvmJava9") {
                    java.srcDir("jvm/src/java9/java")
                }.name,
            )
            jvmTestFrameworks.forEach { framework ->
                val frameworkMain = create("$framework") {
                    associateWith(main)
                    compileTaskProvider.configure {
                        compilerOptions {
                            freeCompilerArgs.add("-Xexplicit-api=strict")
                        }
                    }
                }
                create("${framework}Test") {
                    associateWith(frameworkMain)
                }
                val frameworkJava9SourceSet = project.sourceSets.create("jvm${framework}Java9") {
                    java.srcDir("${framework.lowercase()}/src/java9/java")
                }
                configureJava9Compilation(
                    "kotlin.test.${framework.lowercase()}",
                    listOf(frameworkMain.output.allOutputs),
                    frameworkMain.configurations.compileDependencyConfiguration,
                    frameworkJava9SourceSet.name,
                )
                val java9CompileOnly = configurations[frameworkJava9SourceSet.compileOnlyConfigurationName]
                project.dependencies {
                    java9CompileOnly(project)
                }
            }
            test.associateWith(getByName("JUnit"))
        }
    }
    js {
        if (!kotlinBuildProperties.isTeamcityBuild) {
            browser {}
        }
        nodejs {}
        compilations["main"].compileTaskProvider.configure {
            compilerOptions.freeCompilerArgs.add("-Xir-module-name=$KOTLINTEST_MODULE_NAME")
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        nodejs {
            testTask {
                enabled = false
            }
        }
        compilations["main"].compileTaskProvider.configure {
            compilerOptions.freeCompilerArgs.add("-Xir-module-name=$KOTLINTEST_MODULE_NAME")
        }
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs {
            testTask {
                enabled = false
            }
        }
        compilations["main"].compileTaskProvider.configure {
            compilerOptions.freeCompilerArgs.add("-Xir-module-name=$KOTLINTEST_MODULE_NAME")
        }
    }

    targets.all {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    optIn.add("kotlin.contracts.ExperimentalContracts")
                    freeCompilerArgs.addAll(
                        "-Xallow-kotlin-package",
                        "-Xexpect-actual-classes",
                    )
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(kotlinStdlib())
            }
        }
        val annotationsCommonMain by creating {
            dependsOn(commonMain)
            kotlin.srcDir("annotations-common/src/main/kotlin")
        }
        val assertionsCommonMain by creating {
            dependsOn(commonMain)
            kotlin.srcDir("common/src/main/kotlin")
        }
        val commonTest by getting {
            kotlin.srcDir("annotations-common/src/test/kotlin")
            kotlin.srcDir("common/src/test/kotlin")
        }
        val jvmMain by getting {
            dependsOn(assertionsCommonMain)
            kotlin.srcDir("jvm/src/main/kotlin")
        }
        val jvmTest by getting {
            kotlin.srcDir("jvm/src/test/kotlin")
        }
        val jvmJUnit by getting {
            dependsOn(annotationsCommonMain)
            kotlin.srcDir("junit/src/main/kotlin")
            resources.srcDir("junit/src/main/resources")
            dependencies {
                api("junit:junit:4.13.2")
            }
        }
        val jvmJUnitTest by getting {
            dependsOn(commonTest)
            kotlin.srcDir("junit/src/test/kotlin")
        }
        val jvmJUnit5 by getting {
            dependsOn(annotationsCommonMain)
            kotlin.srcDir("junit5/src/main/kotlin")
            resources.srcDir("junit5/src/main/resources")
            dependencies {
                compileOnly("org.junit.jupiter:junit-jupiter-api:5.0.0")
            }
        }
        val jvmJUnit5Test by getting {
            dependsOn(commonTest)
            kotlin.srcDir("junit5/src/test/kotlin")
            dependencies {
                runtimeOnly(libs.junit.jupiter.engine)
                runtimeOnly(libs.junit.platform.launcher)
            }
        }
        val jvmTestNG by getting {
            dependsOn(annotationsCommonMain)
            kotlin.srcDir("testng/src/main/kotlin")
            resources.srcDir("testng/src/main/resources")
            dependencies {
                api("org.testng:testng:7.0.0")
            }
        }
        val jvmTestNGTest by getting {
            dependsOn(commonTest)
            kotlin.srcDir("testng/src/test/kotlin")
            dependencies {
                implementation("org.testng:testng:7.5.1")
            }
        }
        val jsMain by getting {
            dependsOn(assertionsCommonMain)
            dependsOn(annotationsCommonMain)
            kotlin.srcDir("js/src/main/kotlin")
        }
        val jsTest by getting {
            kotlin.srcDir("js/src/test/kotlin")
        }
        val wasmCommonMain by creating {
            dependsOn(assertionsCommonMain)
            dependsOn(annotationsCommonMain)
            kotlin.srcDir("wasm/src/main/kotlin")
        }
        val wasmJsMain by getting {
            dependsOn(wasmCommonMain)
            kotlin.srcDir("wasm/js/src/main/kotlin")
        }
        val wasmJsTest by getting {
            kotlin.srcDir("wasm/js/src/test/kotlin")
        }
        val wasmWasiMain by getting {
            dependsOn(wasmCommonMain)
            kotlin.srcDir("wasm/wasi/src/main/kotlin")
        }
    }
}

tasks {
    named("compileTestDevelopmentExecutableKotlinWasmJs", KotlinJsIrLink::class) {
        enabled = false
    }
    named("compileTestDevelopmentExecutableKotlinWasmWasi", KotlinJsIrLink::class) {
        enabled = false
    }
    named("compileTestProductionExecutableKotlinWasmJs", KotlinJsIrLink::class) {
        enabled = false
    }
    named("compileTestProductionExecutableKotlinWasmWasi", KotlinJsIrLink::class) {
        enabled = false
    }

    val allMetadataJar by existing(Jar::class) {
        archiveClassifier = "all"
    }
    val jvmJar by existing(Jar::class) {
        archiveAppendix = null
        from(project.sourceSets["jvmJava9"].output)
        manifestAttributes(manifest, "Test", multiRelease = true)
    }
    val jvmSourcesJar by existing(Jar::class) {
        archiveAppendix = null
        kotlin.sourceSets["annotationsCommonMain"].let { sourceSet ->
            into(sourceSet.name) {
                from(sourceSet.kotlin)
            }
        }
    }
    val jvmJarTasks = jvmTestFrameworks.map { framework ->
        named("jvm${framework}Jar", Jar::class) {
            archiveBaseName = base.archivesName
            archiveAppendix = framework.lowercase()
            from(project.sourceSets["jvm${framework}Java9"].output)
            manifestAttributes(manifest, "Test", multiRelease = true)
            manifest.attributes("Implementation-Title" to "${archiveBaseName.get()}-${archiveAppendix.get()}")
        }
    }
    val jvmSourcesJarTasks = jvmTestFrameworks.map { framework ->
        register("jvm${framework}SourcesJar", Jar::class) {
            archiveAppendix = framework.lowercase()
            archiveClassifier = "sources"
            kotlin.jvm().compilations[framework.name].allKotlinSourceSets.forEach {
                from(it.kotlin.sourceDirectories) { into(it.name) }
                from(it.resources.sourceDirectories) { into(it.name) }
            }
        }
    }
    val jsJar by existing(Jar::class) {
        manifestAttributes(manifest, "Test")
        manifest.attributes("Implementation-Title" to "${archiveBaseName.get()}-${archiveAppendix.get()}")
    }
    val wasmJsJar by existing(Jar::class) {
        manifestAttributes(manifest, "Test")
        manifest.attributes("Implementation-Title" to "${archiveBaseName.get()}-${archiveAppendix.get()}")
    }
    val wasmWasiJar by existing(Jar::class) {
        manifestAttributes(manifest, "Test")
        manifest.attributes("Implementation-Title" to "${archiveBaseName.get()}-${archiveAppendix.get()}")
    }
    val assemble by existing {
        dependsOn(jvmJarTasks)
    }

    val jvmTestTasks = jvmTestFrameworks.flatMap { framework ->
        listOf(false, true).map { excludeAsserterContributor ->
            register("jvm${framework}${if (excludeAsserterContributor) "NoAsserter" else ""}Test", Test::class) {
                group = "verification"
                val testCompilation = kotlin.jvm().compilations["${framework}Test"]
                classpath = testCompilation.runtimeDependencyFiles + testCompilation.output.allOutputs
                if (excludeAsserterContributor) {
                    val mainCompilation = kotlin.jvm().compilations["$framework"]
                    classpath -= mainCompilation.output.allOutputs
                    filter.excludePatterns += "*ContributorTest"
                }
                testClassesDirs = testCompilation.output.classesDirs
                when (framework) {
                    JvmTestFramework.JUnit -> useJUnit()
                    JvmTestFramework.JUnit5 -> useJUnitPlatform()
                    JvmTestFramework.TestNG -> useTestNG()
                }
            }
        }
    }
    val allTests by existing {
        dependsOn(jvmTestTasks)
    }

    val generateProjectStructureMetadata by existing(GenerateProjectStructureMetadata::class) {
        val outputTestFile = file("kotlin-project-structure-metadata.beforePatch.json")
        val patchedFile = file("kotlin-project-structure-metadata.json")

        inputs.file(patchedFile)
        inputs.file(outputTestFile)

        doLast {
            /*
            Check that the generated 'outputFile' by default matches our expectations stored in the .beforePatch file
            This will fail if the kotlin-project-structure-metadata.json file would change unnoticed (w/o updating our patched file)
             */
            run {
                val outputFileText = resultFile.readText().trim()
                val expectedFileContent = outputTestFile.readText().trim()
                if (outputFileText != expectedFileContent)
                    error(
                        "${resultFile.path} file content does not match expected content\n\n" +
                                "expected:\n\n$expectedFileContent\n\nactual:\n\n$outputFileText"
                    )
            }

            patchedFile.copyTo(resultFile, overwrite = true)
        }
    }
}

configurations {
    val metadataApiElements by getting
    metadataApiElements.outgoing.capability(kotlinTestCapability)

    for (framework in jvmTestFrameworks) {
        val frameworkCapability = "$group:kotlin-test-framework-${framework.lowercase()}:$version"
        metadataApiElements.outgoing.capability(frameworkCapability)

        val runtimeDeps = create("jvm${framework}RuntimeDependencies") {
            isCanBeResolved = true
            isCanBeConsumed = false
        }
        val (apiElements, runtimeElements, sourcesElements) = listOf(KotlinUsages.KOTLIN_API, KotlinUsages.KOTLIN_RUNTIME, KotlinUsages.KOTLIN_SOURCES).map { usage ->
            val name = "jvm$framework${usage.substringAfter("kotlin-").replaceFirstChar { it.uppercase() }}Elements"
            create(name) {
                isCanBeResolved = false
                isCanBeConsumed = true
                outgoing.capability(baseCapability)
                outgoing.capability(frameworkCapability)
                attributes {
                    attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named(TargetJvmEnvironment.STANDARD_JVM))
                    attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
                    attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
                    when (usage) {
                        KotlinUsages.KOTLIN_SOURCES -> {
                            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
                            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
                            attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.SOURCES))
                            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
                        }
                        KotlinUsages.KOTLIN_API -> {
                            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_API))
                            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
                            extendsFrom(getByName("jvm${framework}Api"))
                        }
                        KotlinUsages.KOTLIN_RUNTIME -> {
                            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
                            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
                            runtimeDeps.extendsFrom(getByName("jvm${framework}Api"))
                            runtimeDeps.extendsFrom(getByName("jvm${framework}Implementation"))
                            runtimeDeps.extendsFrom(getByName("jvm${framework}RuntimeOnly"))
                            extendsFrom(runtimeDeps)
                        }
                        else -> error(usage)
                    }
                }
            }
        }
        dependencies {
            apiElements(project)
            runtimeDeps(project)
            when (framework) {
                JvmTestFramework.JUnit -> {}
                JvmTestFramework.JUnit5 -> {
                    apiElements("org.junit.jupiter:junit-jupiter-api:5.10.1")
                    runtimeDeps("org.junit.jupiter:junit-jupiter-engine:5.10.1")
                    runtimeDeps("org.junit.platform:junit-platform-launcher:1.10.1")
                }
                JvmTestFramework.TestNG -> {
                    apiElements("org.testng:testng:7.5.1")
                }
            }
        }
        artifacts {
            add(apiElements.name, tasks.named<Jar>("jvm${framework}Jar"))
            add(runtimeElements.name, tasks.named<Jar>("jvm${framework}Jar"))
            add(sourcesElements.name, tasks.named<Jar>("jvm${framework}SourcesJar"))
        }
    }

    for (configurationName in listOf("kotlinTestCommon", "kotlinTestAnnotationsCommon")) {
        val legacyConfigurationDeps = create("${configurationName}Dependencies") {
            isCanBeResolved = true
            isCanBeConsumed = false
        }
        val legacyConfiguration = create("${configurationName}Elements") {
            isCanBeResolved = false
            isCanBeConsumed = false
            attributes {
                attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
            }
            extendsFrom(legacyConfigurationDeps)
        }
        dependencies {
            legacyConfigurationDeps(project)
        }
    }

    val jvmMainApi by getting
    val nativeApiElements by creating
    for (artifactName in listOf("kotlin-test-common", "kotlin-test-annotations-common")) {
        dependencies {
            constraints {
                val artifactCoordinates = "$group:$artifactName:$version"
                // there is no dependency anymore from kotlin-test to kotlin-test-common and -annotations-common,
                // but use this constraint to align it if another library brings it transitively
                jvmMainApi(artifactCoordinates)
                metadataApiElements(artifactCoordinates)
                nativeApiElements(artifactCoordinates)
            }
        }
    }
}


configureDefaultPublishing()

fun emptyJavadocJar(appendix: String? = null): TaskProvider<Jar> =
    tasks.register<Jar>("emptyJavadocJar${appendix.orEmpty().capitalize()}") {
        archiveAppendix = appendix
        archiveClassifier = "javadoc"
    }

publishing {
    val artifactBaseName = base.archivesName.get()
    configureMultiModuleMavenPublishing {
        val rootModule = module("rootModule") {
            mavenPublication {
                artifactId = artifactBaseName
                configureKotlinPomAttributes(project, "Kotlin Test Multiplatform library")
                artifact(emptyJavadocJar())
            }
            variant("metadataApiElements") { suppressPomMetadataWarnings() }
            variant("jvmApiElements")
            variant("jvmRuntimeElements") {
                configureVariantDetails { mapToMavenScope("runtime") }
            }
            variant("jvmSourcesElements")
            variant("nativeApiElements") {
                attributes {
                    attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
                    attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named("non-jvm"))
                    attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_API))
                    attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
                }
            }
        }
        val frameworkModules = jvmTestFrameworks.map { framework ->
            module("${framework.lowercase()}Module") {
                mavenPublication {
                    artifactId = "$artifactBaseName-${framework.lowercase()}"
                    configureKotlinPomAttributes(project, "Kotlin Test library support for ${framework}")
                    artifact(emptyJavadocJar(framework.lowercase()))
                }
                variant("jvm${framework}ApiElements") { suppressPomMetadataWarnings() }
                variant("jvm${framework}RuntimeElements") {
                    suppressPomMetadataWarnings()
                    configureVariantDetails { mapToMavenScope("runtime") }
                }
                variant("jvm${framework}SourcesElements") { suppressPomMetadataWarnings() }
            }
        }

        val js = module("jsModule") {
            mavenPublication {
                artifactId = "$artifactBaseName-js"
                configureKotlinPomAttributes(project, "Kotlin Test library for JS", packaging = "klib")
            }
            variant("jsApiElements")
            variant("jsRuntimeElements")
            variant("jsSourcesElements")
        }

        val wasmJs = module("wasmJsModule") {
            mavenPublication {
                artifactId = "$artifactBaseName-wasm-js"
                configureKotlinPomAttributes(project, "Kotlin Test library for experimental WebAssembly JS platform", packaging = "klib")
            }
            variant("wasmJsApiElements")
            variant("wasmJsRuntimeElements")
            variant("wasmJsSourcesElements")
        }
        val wasmWasi = module("wasmWasiModule") {
            mavenPublication {
                artifactId = "$artifactBaseName-wasm-wasi"
                configureKotlinPomAttributes(project, "Kotlin Test library for experimental WebAssembly WASI platform", packaging = "klib")
            }
            variant("wasmWasiApiElements")
            variant("wasmWasiRuntimeElements")
            variant("wasmWasiSourcesElements")
        }

        module("testCommonModule") {
            mavenPublication {
                artifactId = "$artifactBaseName-common"
                configureKotlinPomAttributes(project, "Legacy artifact of Kotlin Test library. Use kotlin-test instead", packaging = "pom")
                (this as PublicationInternal<*>).isAlias = true
            }
            variant("kotlinTestCommonElements")
        }
        module("testAnnotationsCommonModule") {
            mavenPublication {
                artifactId = "$artifactBaseName-annotations-common"
                configureKotlinPomAttributes(project, "Legacy artifact of Kotlin Test library. Use kotlin-test instead", packaging = "pom")
                (this as PublicationInternal<*>).isAlias = true
            }
            variant("kotlinTestAnnotationsCommonElements")
        }

        // Makes all variants from accompanying artifacts visible through `available-at`
        rootModule.include(js, *frameworkModules.toTypedArray(), wasmJs, wasmWasi)
    }

    publications {
        (listOf(
            listOf("rootModule", "Main", "kotlin-test", "jvmRuntimeClasspath"),
            listOf("jsModule", "Js", "kotlin-test-js", "jsRuntimeClasspath"),
            listOf("wasmJsModule", "Wasm-Js", "kotlin-test-wasm-js", "wasmJsRuntimeClasspath"),
            listOf("wasmWasiModule", "Wasm-Wasi", "kotlin-test-wasm-wasi", "wasmWasiRuntimeClasspath"),
            listOf("testCommonModule", "Common", "kotlin-test-common", "kotlinTestCommonDependencies"),
            listOf("testAnnotationsCommonModule", "AnnotationsCommon", "kotlin-test-annotations-common", "kotlinTestAnnotationsCommonDependencies"),
        ) + jvmTestFrameworks.map { framework ->
            listOf("${framework.lowercase()}Module", "$framework", "kotlin-test-${framework.lowercase()}", "jvm${framework}RuntimeDependencies")
        }).forEach { (module, sbomTarget, sbomDocument, classpath) ->
            configureSbom(sbomTarget, sbomDocument, setOf(classpath), named<MavenPublication>(module))
        }
    }
}


tasks.withType<GenerateModuleMetadata> {
    val publication = publication.get() as MavenPublication
    // alter capabilities of leaf JVM framework artifacts published by "available-at" coordinates
    if (jvmTestFrameworks.map { it.lowercase() }.any { publication.artifactId.endsWith(it) }) {
        fun capability(group: String, name: String, version: String) =
            mapOf("group" to group, "name" to name, "version" to version)

        val defaultCapability = publication.let { capability(it.groupId, it.artifactId, it.version) }
        val implCapability = implCapability.split(":").let { (g, n, v) -> capability(g, n, v) }
        val capabilities = listOf(defaultCapability, implCapability)

        doLast {
            val output = outputFile.get().asFile
            val gson = GsonBuilder().setPrettyPrinting().create()
            val moduleJson = output.bufferedReader().use { gson.fromJson(it, JsonObject::class.java) }
            val variants = moduleJson.getAsJsonArray("variants")
            variants.forEach { variant ->
                variant as JsonObject
                variant.remove("capabilities")
                variant.add("capabilities", gson.toJsonTree(capabilities))
            }
            output.bufferedWriter().use { writer -> gson.toJson(moduleJson, writer) }
        }
    }
}
