import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.internal.artifacts.ArtifactAttributes
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType.IR
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.Companion.attribute
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.ProjectLocalConfigurations.ATTRIBUTE
import org.jetbrains.kotlin.gradle.plugin.ProjectLocalConfigurations.PUBLIC_VALUE
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages.KOTLIN_API
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages.KOTLIN_RUNTIME
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute.Companion.jsCompilerAttribute
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute.legacy
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin

plugins {
    kotlin("multiplatform")
    `maven-publish`
    signing
}

apply<plugins.LegacyKotlinJsComponentPlugin>()


var target: KotlinTarget? = null

kotlin {
    target = js(IR) {
        nodejs()
    }
}

fun Configuration.configureAttributesForLegacy(usage: String) {
    isCanBeResolved = false
    isCanBeConsumed = true
    attributes {
        attribute(
            USAGE_ATTRIBUTE,
            target!!.project.objects.named(
                Usage::class.java,
                usage
            )
        )
        attribute(attribute, target!!.platformType)
        attribute(jsCompilerAttribute, legacy)
        attribute(ATTRIBUTE, PUBLIC_VALUE)
    }
}

val legacyApiElements by configurations.creating {
    configureAttributesForLegacy(KOTLIN_API)
}

val legacyRuntimeElements by configurations.creating {
    configureAttributesForLegacy(KOTLIN_RUNTIME)
}

val jarWithoutIr = tasks.getByPath(":kotlin-stdlib-js:libraryJarWithoutIr")

val legacyArtifact = artifacts.add(Dependency.ARCHIVES_CONFIGURATION, jarWithoutIr) {
    this.builtBy(jarWithoutIr)
    this.type = ArtifactTypeDefinition.JAR_TYPE

    addJar(legacyApiElements, this)

    addJar(legacyRuntimeElements, this)
}

fun addJar(configuration: Configuration, jarArtifact: PublishArtifact) {
    val publications = configuration.outgoing

    // Configure an implicit variant
    publications.artifacts.add(jarArtifact)
    publications.attributes.attribute(ArtifactAttributes.ARTIFACT_FORMAT, ArtifactTypeDefinition.JAR_TYPE)
}

val adhocComponent = components.getByName("myAdhocComponent") as AdhocComponentWithVariants
adhocComponent.addVariantsFromConfiguration(legacyRuntimeElements) {
    mapToMavenScope("runtime")
}

adhocComponent.addVariantsFromConfiguration(legacyApiElements) {
    mapToMavenScope("compile")
}

adhocComponent.addVariantsFromConfiguration(configurations.getByName(target!!.apiElementsConfigurationName)) {
    mapToMavenScope("compile")
}

adhocComponent.addVariantsFromConfiguration(configurations.getByName(target!!.runtimeElementsConfigurationName)) {
    mapToMavenScope("runtime")
}

val sourcesJar = artifacts.add(Dependency.ARCHIVES_CONFIGURATION, tasks.getByPath(":kotlin-stdlib-js:sourcesJar"))
val javadocJar = artifacts.add(Dependency.ARCHIVES_CONFIGURATION, tasks.getByPath(":kotlin-stdlib-js:javadocJar"))

publishing {
    publications {
        create<MavenPublication>("jsNew") {
            from(adhocComponent)
            artifact(sourcesJar)
            artifact(javadocJar)
        }
    }
}


// Disable multiplatform predefined publications
listOf("js", "kotlinMultiplatform", "metadata")
    .forEach { name ->
        tasks.named("generatePomFileFor${name.capitalize()}Publication") {
            enabled = false
        }
        tasks.named("generateMetadataFileFor${name.capitalize()}Publication") {
            enabled = false
        }
        tasks.named("publish${name.capitalize()}PublicationToMavenLocal") {
            enabled = false
        }
    }

publishing {
    publications.getByName("jsNew") {
        this as MavenPublication
        pom {
            val stdlibJs = findProject(":kotlin-stdlib-js")!!.name
            name.set("${project.group}:$stdlibJs")
            artifactId = stdlibJs
            // optionally artifactId can be defined here
            description.set(project.description)
            url.set("https://kotlinlang.org/")
            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            scm {
                url.set("https://github.com/JetBrains/kotlin")
                connection.set("scm:git:https://github.com/JetBrains/kotlin.git")
                developerConnection.set("scm:git:https://github.com/JetBrains/kotlin.git")
            }
            developers {
                developer {
                    name.set("Kotlin Team")
                    organization.set("JetBrains")
                    organizationUrl.set("https://www.jetbrains.com")
                }
            }
        }
    }
}

val isSonatypeRelease: Boolean? by project.extra

if (!project.hasProperty("prebuiltJar")) {
    signing {
        setRequired((project.properties["signingRequired"] as Boolean?) ?: isSonatypeRelease)
        sign(publishing.publications["jsNew"])
    }

    tasks.named("signJsNewPublication") {
        enabled = signing.isRequired
    }
}

val prepareTask = rootProject.tasks.getByName("preparePublication")

publishing {
    repositories {
        maven {
            setUrl(project.provider {
                val repoUrl = prepareTask.extra["repoUrl"]!! as String?
                if (repoUrl?.startsWith("file:") != true) {
                    credentials {
                        username = prepareTask.extra["username"] as? String
                        password = prepareTask.extra["password"] as? String
                    }
                }
                repoUrl
            })
        }
    }
}

tasks.named("publish") {
    dependsOn(prepareTask)
}

tasks.register("install") {
    dependsOn("publishJsNewPublicationToMavenLocal")
}

// Using custom node version because regression https://bugs.chromium.org/p/v8/issues/detail?id=9546
// causes test.numbers.DoubleMathTest.powers to fail
rootProject.plugins.withType<NodeJsRootPlugin> {
    rootProject.extensions.getByType(NodeJsRootExtension::class.java).apply {
        nodeVersion = "12.16.1"
    }
}

val unimplementedNativeBuiltIns =
    (file("$rootDir/core/builtins/native/kotlin/").list().toSet() - file("$rootDir/libraries/stdlib/js-ir/builtins/").list())
        .map { "core/builtins/native/kotlin/$it" }

// Required to compile native builtins with the rest of runtime
val builtInsHeader = """@file:Suppress(
    "NON_ABSTRACT_FUNCTION_WITH_NO_BODY",
    "MUST_BE_INITIALIZED_OR_BE_ABSTRACT",
    "EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE",
    "PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED",
    "WRONG_MODIFIER_TARGET"
)
"""

val commonMainSources by task<Sync> {
    val sources = listOf(
        "libraries/stdlib/common/src/",
        "libraries/stdlib/src/kotlin/",
        "libraries/stdlib/unsigned/"
    )

    sources.forEach { path ->
        from("$rootDir/$path") {
            into(path.dropLastWhile { it != '/' })
        }
    }

    into("$buildDir/commonMainSources")
}

val jsMainSources by task<Sync> {
    val sources = listOf(
        "core/builtins/src/kotlin/",
        "libraries/stdlib/js/src/",
        "libraries/stdlib/js/runtime/",
        "libraries/stdlib/js-ir/builtins/",
        "libraries/stdlib/js-ir/src/",
        "libraries/stdlib/js-ir/runtime/",

        // TODO get rid - move to test module
        "js/js.translator/testData/_commonFiles/"
    ) + unimplementedNativeBuiltIns

    val excluded = listOf(
        // stdlib/js/src/generated is used exclusively for current `js-v1` backend.
        "libraries/stdlib/js/src/generated/**",

        // JS-specific optimized version of emptyArray() already defined
        "core/builtins/src/kotlin/ArrayIntrinsics.kt"
    )

    sources.forEach { path ->
        from("$rootDir/$path") {
            into(path.dropLastWhile { it != '/' })
            excluded.filter { it.startsWith(path) }.forEach {
                exclude(it.substring(path.length))
            }
        }
    }

    into("$buildDir/jsMainSources")

    doLast {
        unimplementedNativeBuiltIns.forEach { path ->
            val file = File("$buildDir/jsMainSources/$path")
            val sourceCode = builtInsHeader + file.readText()
            file.writeText(sourceCode)
        }
    }
}

val commonTestSources by task<Sync> {
    val sources = listOf(
        "libraries/stdlib/test/",
        "libraries/stdlib/common/test/"
    )

    sources.forEach { path ->
        from("$rootDir/$path") {
            into(path.dropLastWhile { it != '/' })
        }
    }

    into("$buildDir/commonTestSources")
}

val jsTestSources by task<Sync> {
    from("$rootDir/libraries/stdlib/js/test/")
    into("$buildDir/jsTestSources")
}

val fullRuntimeDir: File = buildDir.resolve("fullRuntime/klib")

kotlin {
    sourceSets {
        val commonMain by getting {
            kotlin.srcDir(commonMainSources.get().destinationDir)
        }
        val jsMain by getting {
            kotlin.srcDir(jsMainSources.get().destinationDir)
        }
        val commonTest by getting {
            dependencies {
                api(project(":kotlin-test:kotlin-test-js-ir"))
            }
            kotlin.srcDir(commonTestSources.get().destinationDir)
        }
        val jsTest by getting {
            dependencies {
                api(project(":kotlin-test:kotlin-test-js-ir"))
            }
            kotlin.srcDir(jsTestSources.get().destinationDir)
        }
    }
}



tasks.withType<KotlinCompile<*>>().configureEach {
    kotlinOptions.freeCompilerArgs += listOf(
        "-Xallow-kotlin-package",
        "-Xallow-result-return-type",
        "-Xuse-experimental=kotlin.Experimental",
        "-Xuse-experimental=kotlin.ExperimentalMultiplatform",
        "-Xuse-experimental=kotlin.contracts.ExperimentalContracts",
        "-Xinline-classes",
        "-Xopt-in=kotlin.RequiresOptIn",
        "-Xopt-in=kotlin.ExperimentalUnsignedTypes",
        "-Xopt-in=kotlin.ExperimentalStdlibApi"
    )
}

tasks.named("compileKotlinJs") {
    dependsOn(commonMainSources)
    dependsOn(jsMainSources)
}

tasks.named("compileTestKotlinJs") {
    dependsOn(commonTestSources)
    dependsOn(jsTestSources)
}


