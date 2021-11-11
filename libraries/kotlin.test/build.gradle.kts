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
    isCanBeResolved = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("java-api"))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
    }
}

val jvmRuntime by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("java-runtime"))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
    }
    extendsFrom(jvmApi)
}

val jsApiVariant by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("kotlin-api"))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
    }
}
val jsRuntimeVariant by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("kotlin-runtime"))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
    }
    extendsFrom(jsApiVariant)
}

val wasmApiVariant by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("kotlin-api"))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.wasm)
    }
}
val wasmRuntimeVariant by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("kotlin-runtime"))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.wasm)
    }
    extendsFrom(wasmApiVariant)
}

val nativeApiVariant by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("kotlin-api"))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
    }
}

val commonVariant by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("kotlin-api"))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.common)
    }
}

dependencies {
    jvmApi(project(":kotlin-stdlib"))
    jsApiVariant("$group:kotlin-test-js:$version")
    wasmApiVariant("$group:kotlin-test-wasm:$version")
    commonVariant("$group:kotlin-test-common:$version")
    commonVariant("$group:kotlin-test-annotations-common:$version")
}

artifacts {
    val jvmJar = tasks.getByPath(":kotlin-test:kotlin-test-jvm:jar")
    add(jvmApi.name, jvmJar)
    add(jvmRuntime.name, jvmJar)
}

val kotlinTestCommonSourcesJar = tasks.getByPath(":kotlin-test:kotlin-test-common:sourcesJar") as Jar
val kotlinTestJvmSourcesJar = tasks.getByPath(":kotlin-test:kotlin-test-jvm:sourcesJar") as Jar

val combinedSourcesJar by tasks.registering(Jar::class) {
    dependsOn(kotlinTestCommonSourcesJar, kotlinTestJvmSourcesJar)
    archiveClassifier.set("sources")
    into("common") {
        from(zipTree(kotlinTestCommonSourcesJar.archiveFile)) {
            exclude("META-INF/**")
        }
    }
    into("jvm") {
        from(zipTree(kotlinTestJvmSourcesJar.archiveFile)) {
            exclude("META-INF/**")
        }
    }
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
            isCanBeResolved = false
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
                apiElements("junit:junit:4.12")
            }
            "junit5" -> {
                apiElements("org.junit.jupiter:junit-jupiter-api:5.6.0")
                runtimeElements("org.junit.jupiter:junit-jupiter-engine:5.6.0")
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
        isCanBeResolved = false
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
        isCanBeResolved = false
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
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("kotlin-api"))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.common)
    }
}
val annotationsMetadata by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
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
        create("main", MavenPublication::class) {
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
        }
        jvmTestFrameworks.forEach { framework ->
            create(framework, MavenPublication::class) {
                artifactId = "kotlin-test-$framework"
                from(components[framework])
                artifact(tasks.getByPath(":kotlin-test:kotlin-test-$framework:sourcesJar") as Jar)
                artifact(emptyJavadocJar)
                configureKotlinPomAttributes(project, "Kotlin Test Support for $framework")
            }
        }
        create("js", MavenPublication::class) {
            artifactId = "kotlin-test-js"
            from(jsComponent)
            artifact(tasks.getByPath(":kotlin-test:kotlin-test-js:sourcesJar") as Jar)
            artifact(emptyJavadocJar)
            configureKotlinPomAttributes(project, "Kotlin Test for JS")
        }
        create("wasm", MavenPublication::class) {
            pom.packaging = "klib"
            artifactId = "kotlin-test-wasm"
            from(wasmComponent)
            configureKotlinPomAttributes(project, "Kotlin Test for WASM")
        }
        create("common", MavenPublication::class) {
            artifactId = "kotlin-test-common"
            from(commonMetadataComponent)
            artifact(tasks.getByPath(":kotlin-test:kotlin-test-common:sourcesJar") as Jar)
            artifact(emptyJavadocJar)
            configureKotlinPomAttributes(project, "Kotlin Test Common")
        }
        create("annotationsCommon", MavenPublication::class) {
            artifactId = "kotlin-test-annotations-common"
            from(annotationsMetadataComponent)
            artifact(tasks.getByPath(":kotlin-test:kotlin-test-annotations-common:sourcesJar") as Jar)
            artifact(emptyJavadocJar)
            configureKotlinPomAttributes(project, "Kotlin Test Common")
        }
        withType<MavenPublication> {
            suppressAllPomMetadataWarnings()
        }
    }
}

tasks.withType<GenerateModuleMetadata> {
    enabled = "common" !in (publication.get() as MavenPublication).artifactId
}