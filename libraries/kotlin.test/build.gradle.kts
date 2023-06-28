import org.gradle.kotlin.dsl.support.serviceOf
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import plugins.configureDefaultPublishing
import plugins.configureKotlinPomAttributes
import groovy.util.Node
import groovy.util.NodeList

plugins {
    `kotlin-multiplatform` apply false
    base
    `maven-publish`
    signing
}

open class ComponentsFactoryAccess
@javax.inject.Inject
constructor(val factory: SoftwareComponentFactory)

val componentFactory = objects.newInstance<ComponentsFactoryAccess>().factory


val jvmApi by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = true
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("java-api"))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
    }
}

val jvmRuntime by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = true
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("java-runtime"))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
    }
    extendsFrom(jvmApi)
}

val jsApiVariant by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("kotlin-api"))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
    }
}
val jsRuntimeVariant by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("kotlin-runtime"))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
    }
    extendsFrom(jsApiVariant)
}

val wasmApiVariant by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("kotlin-api"))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.wasm)
    }
}
val wasmRuntimeVariant by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("kotlin-runtime"))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.wasm)
    }
    extendsFrom(wasmApiVariant)
}

val nativeApiVariant by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("kotlin-api"))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
    }
}

val commonVariant by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("kotlin-api"))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.common)
    }
}

fun Configuration.sourcesConsumingConfiguration() {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
        attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.SOURCES))
    }
}

val kotlinTestCommonSources by configurations.creating {
    sourcesConsumingConfiguration()
}

val kotlinTestJvmSources by configurations.creating {
    sourcesConsumingConfiguration()
}

dependencies {
    jvmApi(project(":kotlin-stdlib"))
    jsApiVariant("$group:kotlin-test-js:$version")
    wasmApiVariant("$group:kotlin-test-wasm:$version")
    commonVariant(project(":kotlin-test:kotlin-test-common"))
    commonVariant(project(":kotlin-test:kotlin-test-annotations-common"))
    kotlinTestCommonSources(project(":kotlin-test:kotlin-test-common"))
    kotlinTestJvmSources(project(":kotlin-test:kotlin-test-jvm"))
}

artifacts {
    val jvmJar = tasks.getByPath(":kotlin-test:kotlin-test-jvm:jar")
    add(jvmApi.name, jvmJar)
    add(jvmRuntime.name, jvmJar)
}

val combinedSourcesJar by tasks.registering(Jar::class) {
    dependsOn(kotlinTestCommonSources)
    dependsOn(kotlinTestJvmSources)
    archiveClassifier.set("sources")
    val archiveOperations = serviceOf<ArchiveOperations>()
    into("common") {
        from({ archiveOperations.zipTree(kotlinTestCommonSources.singleFile) }) {
            exclude("META-INF/**")
        }
    }
    into("jvm") {
        from({ archiveOperations.zipTree(kotlinTestJvmSources.singleFile) }) {
            exclude("META-INF/**")
        }
    }
}

val combinedJvmSourcesJar by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}

artifacts {
    add(combinedJvmSourcesJar.name, combinedSourcesJar)
}

val rootComponent = componentFactory.adhoc("root").apply {
    addVariantsFromConfiguration(jvmApi) {
        mapToMavenScope("compile")
    }
    addVariantsFromConfiguration(jvmRuntime) {
        mapToMavenScope("runtime")
    }
    addVariantsFromConfiguration(jsApiVariant) { mapToOptional() }
    addVariantsFromConfiguration(jsRuntimeVariant) { mapToOptional() }
    addVariantsFromConfiguration(wasmApiVariant) { mapToOptional() }
    addVariantsFromConfiguration(wasmRuntimeVariant) { mapToOptional() }
    addVariantsFromConfiguration(nativeApiVariant) { mapToOptional() }
    addVariantsFromConfiguration(commonVariant) { mapToOptional() }
}


val kotlinTestCapability = "$group:kotlin-test:$version" // add to variants with explicit capabilities when the default one is needed, too
val baseCapability = "$group:kotlin-test-framework:$version"
val implCapability = "$group:kotlin-test-framework-impl:$version"

val jvmTestFrameworks = listOf("junit", "junit5", "testng")

val frameworkCapabilities = mutableSetOf<String>()

jvmTestFrameworks.forEach { framework ->
    val (apiVariant, runtimeVariant) = listOf("api", "runtime").map { usage ->
        configurations.create("${framework}${usage.capitalize()}Variant") {
            isCanBeConsumed = true
            isCanBeResolved = false
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named("java-$usage"))
                attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
            }
            outgoing.capability(baseCapability)  // C0
            outgoing.capability(
                "$group:kotlin-test-framework-$framework:$version".also { frameworkCapabilities.add(it) }
            ) // C0
        }
    }
    runtimeVariant.extendsFrom(apiVariant)
    dependencies {
        apiVariant("$group:kotlin-test-$framework:$version")
    }
    rootComponent.addVariantsFromConfiguration(apiVariant) { mapToOptional() }
    rootComponent.addVariantsFromConfiguration(runtimeVariant) { mapToOptional() }

    val (apiElements, runtimeElements) = listOf("api", "runtime").map { usage ->
        configurations.create("${framework}${usage.capitalize()}") {
            isCanBeConsumed = true
            isCanBeResolved = true
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named("java-$usage"))
            }
            outgoing.capability(implCapability) // CC
            outgoing.capability("$group:kotlin-test-$framework:$version")  // CC
        }
    }
    runtimeElements.extendsFrom(apiElements)
    dependencies {
        apiElements("$group:kotlin-test:$version")
        when(framework) {
            "junit" -> {
                apiElements("junit:junit:4.13.2")
            }
            "junit5" -> {
                apiElements("org.junit.jupiter:junit-jupiter-api:5.6.3")
                runtimeElements("org.junit.jupiter:junit-jupiter-engine:5.6.3")
            }
            "testng" -> {
                apiElements("org.testng:testng:6.13.1")
            }
        }
    }

    artifacts {
        val jar = tasks.getByPath(":kotlin-test:kotlin-test-$framework:jar")
        add(apiElements.name, jar)
        add(runtimeElements.name, jar)
    }

    componentFactory.adhoc(framework).apply {
        addVariantsFromConfiguration(apiElements) {
            mapToMavenScope("compile")
        }
        addVariantsFromConfiguration(runtimeElements) {
            mapToMavenScope("runtime")
        }
    }.let { components.add(it) }
}

/**
 * When a consumer's dependency requires a specific test framework (like with auto framework selection), their configurations requesting
 * "common" artifacts (such as `*DependenciesMetadata` in MPP) should choose this variant anyway. Otherwise, choosing this variant
 * (from a "pure", capability-less dependency on `kotlin-test` appearing transitively in the dependency graph) along with some
 * capability-providing *platform* variant leads to incompatible variants being chosen together, causing dependency resolution errors,
 * see KTIJ-6098
 */
commonVariant.apply {
    frameworkCapabilities.forEach(outgoing::capability)
    outgoing.capability(kotlinTestCapability)
}

val (jsApi, jsRuntime) = listOf("api", "runtime").map { usage ->
    configurations.create("js${usage.capitalize()}") {
        isCanBeConsumed = true
        isCanBeResolved = true
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named("kotlin-$usage"))
            attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
        }
    }
}
jsRuntime.extendsFrom(jsApi)

dependencies {
    jsApi(project(":kotlin-stdlib-js"))
}

artifacts {
    val jsJar = tasks.getByPath(":kotlin-test:kotlin-test-js:libraryJarWithIr")
    add(jsApi.name, jsJar)
    add(jsRuntime.name, jsJar)
}

val jsComponent = componentFactory.adhoc("js").apply {
    addVariantsFromConfiguration(jsApi) {
        mapToMavenScope("compile")
    }
    addVariantsFromConfiguration(jsRuntime) {
        mapToMavenScope("runtime")
    }
}

val (wasmApi, wasmRuntime) = listOf("api", "runtime").map { usage ->
    configurations.create("wasm${usage.capitalize()}") {
        isCanBeConsumed = true
        isCanBeResolved = true
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named("kotlin-$usage"))
            attribute(KotlinPlatformType.attribute, KotlinPlatformType.wasm)
        }
    }
}
wasmRuntime.extendsFrom(wasmApi)

dependencies {
    wasmApi(project(":kotlin-stdlib-wasm"))
}

artifacts {
    val wasmKlib = tasks.getByPath(":kotlin-test:kotlin-test-wasm:wasmJar")
    add(wasmApi.name, wasmKlib) {
        extension = "klib"
    }
    add(wasmRuntime.name, wasmKlib) {
        extension = "klib"
    }
}

val wasmComponent = componentFactory.adhoc("wasm").apply {
    addVariantsFromConfiguration(wasmApi) {
        mapToMavenScope("compile")
    }
    addVariantsFromConfiguration(wasmRuntime) {
        mapToMavenScope("runtime")
    }
}

val commonMetadata by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("kotlin-api"))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.common)
    }
}
val annotationsMetadata by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("kotlin-api"))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.common)
    }
}
dependencies {
    commonMetadata(project(":kotlin-stdlib-common"))
    annotationsMetadata(project(":kotlin-stdlib-common"))
}
artifacts {
    add(commonMetadata.name, tasks.getByPath(":kotlin-test:kotlin-test-common:jar"))
    add(annotationsMetadata.name, tasks.getByPath(":kotlin-test:kotlin-test-annotations-common:jar"))
}
val commonMetadataComponent = componentFactory.adhoc("common").apply {
    addVariantsFromConfiguration(commonMetadata) {
        mapToMavenScope("compile")
    }
}
val annotationsMetadataComponent = componentFactory.adhoc("annotations-common").apply {
    addVariantsFromConfiguration(annotationsMetadata) {
        mapToMavenScope("compile")
    }
}

val emptyJavadocJar by tasks.creating(Jar::class) {
    archiveClassifier.set("javadoc")
}

configureDefaultPublishing()

publishing {
    publications {
        val mainPublication = register("main", MavenPublication::class) {
            from(rootComponent)
            artifact(combinedSourcesJar)
            artifact(emptyJavadocJar)
            // Remove all optional dependencies from the root pom
            pom.withXml {
                val dependenciesNode = (asNode().get("dependencies") as NodeList).filterIsInstance<Node>().single()
                val optionalDependencies = (dependenciesNode.get("dependency") as NodeList).filterIsInstance<Node>().filter {
                    ((it.get("optional") as NodeList).singleOrNull() as Node?)?.text() == "true"
                }
                optionalDependencies.forEach { dependenciesNode.remove(it) }
            }
            configureKotlinPomAttributes(project, "Kotlin Test Multiplatform library")
            suppressAllPomMetadataWarnings()
        }
        configureSbom(
            "Main", "kotlin-test",
            setOf(jvmRuntime.name, commonVariant.name), mainPublication
        )
        jvmTestFrameworks.forEach { framework ->
            val publication = register(framework, MavenPublication::class) {
                artifactId = "kotlin-test-$framework"
                from(components[framework])
                artifact(tasks.getByPath(":kotlin-test:kotlin-test-$framework:sourcesJar") as Jar)
                artifact(tasks.getByPath(":kotlin-test:kotlin-test-$framework:javadocJar") as Jar)
                configureKotlinPomAttributes(project, "Kotlin Test Support for $framework")
                suppressAllPomMetadataWarnings()
            }
            configureSbom(
                framework.capitalize(), "kotlin-test-$framework",
                setOf("${framework}Api", commonVariant.name), publication
            )
        }
        val kotlinTestJsPublication = register("js", MavenPublication::class) {
            artifactId = "kotlin-test-js"
            from(jsComponent)
            artifact(tasks.getByPath(":kotlin-test:kotlin-test-js:sourcesJar") as Jar)
            artifact(tasks.getByPath(":kotlin-test:kotlin-test-js:javadocJar") as Jar)
            configureKotlinPomAttributes(project, "Kotlin Test for JS")
        }
        configureSbom(
            "Js", "kotlin-test-js",
            setOf(jsRuntime.name, commonVariant.name), kotlinTestJsPublication
        )
        val kotlinTestWasmPublication = register("wasm", MavenPublication::class) {
            artifactId = "kotlin-test-wasm"
            from(wasmComponent)
            artifact(tasks.getByPath(":kotlin-test:kotlin-test-wasm:sourcesJar") as Jar)
            artifact(tasks.getByPath(":kotlin-test:kotlin-test-wasm:emptyJavadocJar") as Jar)
            configureKotlinPomAttributes(project, "Kotlin Test for WASM", packaging = "klib")
        }
        configureSbom(
            "Wasm", "kotlin-test-wasm",
            setOf(wasmRuntime.name, commonVariant.name), kotlinTestWasmPublication
        )
        val kotlinTestCommonPublication = register("common", MavenPublication::class) {
            artifactId = "kotlin-test-common"
            from(commonMetadataComponent)
            artifact(tasks.getByPath(":kotlin-test:kotlin-test-common:sourcesJar") as Jar)
            artifact(tasks.getByPath(":kotlin-test:kotlin-test-common:javadocJar") as Jar)
            configureKotlinPomAttributes(project, "Kotlin Test Common")
        }
        configureSbom(
            "Common", "kotlin-test-common",
            setOf(commonMetadata.name), kotlinTestCommonPublication
        )
        val annotationsCommonPublication = register("annotationsCommon", MavenPublication::class) {
            artifactId = "kotlin-test-annotations-common"
            from(annotationsMetadataComponent)
            artifact(tasks.getByPath(":kotlin-test:kotlin-test-annotations-common:sourcesJar") as Jar)
            artifact(tasks.getByPath(":kotlin-test:kotlin-test-annotations-common:javadocJar") as Jar)
            configureKotlinPomAttributes(project, "Kotlin Test Common")
        }
        configureSbom(
            "AnnotationsCommon", "kotlin-test-annotations-common",
            setOf(annotationsMetadata.name), annotationsCommonPublication
        )
    }
}

tasks.withType<GenerateModuleMetadata> {
    enabled = "common" !in (publication.get() as MavenPublication).artifactId
}