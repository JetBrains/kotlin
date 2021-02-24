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
    addVariantsFromConfiguration(nativeApiVariant) { mapToOptional() }
    addVariantsFromConfiguration(commonVariant) { mapToOptional() }
}


val baseCapability = "$group:kotlin-test-framework:$version"
val implCapability = "$group:kotlin-test-framework-impl:$version"

val jvmTestFrameworks = listOf("junit", "junit5", "testng")

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
            outgoing.capability("$group:kotlin-test-framework-$framework:$version") // C0
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
                apiElements("org.junit.jupiter:junit-jupiter-api:5.0.0")
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
                configureKotlinPomAttributes(project, "Kotlin Test Support for $framework")
            }
        }
        create("js", MavenPublication::class) {
            artifactId = "kotlin-test-js"
            from(jsComponent)
            artifact(tasks.getByPath(":kotlin-test:kotlin-test-js:sourcesJar") as Jar)
            configureKotlinPomAttributes(project, "Kotlin Test for JS")
        }
        create("common", MavenPublication::class) {
            artifactId = "kotlin-test-common"
            from(commonMetadataComponent)
            artifact(tasks.getByPath(":kotlin-test:kotlin-test-common:sourcesJar") as Jar)
            configureKotlinPomAttributes(project, "Kotlin Test Common")
        }
        create("annotationsCommon", MavenPublication::class) {
            artifactId = "kotlin-test-annotations-common"
            from(annotationsMetadataComponent)
            artifact(tasks.getByPath(":kotlin-test:kotlin-test-annotations-common:sourcesJar") as Jar)
            configureKotlinPomAttributes(project, "Kotlin Test Common")
        }
        withType<MavenPublication> {
            suppressAllPomMetadataWarnings()
            artifact(emptyJavadocJar)
        }
    }
}

tasks.withType<GenerateModuleMetadata> {
    enabled = "common" !in (publication.get() as MavenPublication).artifactId
}