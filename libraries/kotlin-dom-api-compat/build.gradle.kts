import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute
import plugins.configureDefaultPublishing
import plugins.configureKotlinPomAttributes

plugins {
    `maven-publish`
    kotlin("multiplatform")
}

val jsStdlibSources = "${projectDir}/../stdlib/js/src"

val kotlinStdlibJs = configurations.dependencyScope("kotlinStdlibJs")
val kotlinStdlibJsResolvable = configurations.resolvable("kotlinStdlibJsResolvable") {
    extendsFrom(kotlinStdlibJs.get())
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_API))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
        attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
        // the workaround below for KT-65266 expects a packed artifact
        val klibPackagingAttribute = Attribute.of("org.jetbrains.kotlin.klib.packaging", String::class.java)
        attribute(klibPackagingAttribute, "packed")
    }
}

dependencies {
    kotlinStdlibJs(kotlinStdlib())
}

// Workaround for #KT-65266
val prepareFriendStdlibJs = tasks.register<Zip>("prepareFriendStdlibJs") {
    dependsOn(kotlinStdlibJsResolvable)
    from { zipTree(kotlinStdlibJsResolvable.get().singleFile).matching { exclude("META-INF/MANIFEST.MF") } }
    destinationDirectory = layout.buildDirectory.map { it.dir("libs") }
    archiveFileName = "friend-kotlin-stdlib-js.klib"
}

@Suppress("UNUSED_VARIABLE")
kotlin {
    explicitApi()
    js {
        nodejs()
        browser()
    }

    compilerOptions {
        allWarningsAsErrors.set(true)
        optIn.addAll(
            "kotlin.ExperimentalMultiplatform",
            "kotlin.contracts.ExperimentalContracts",
        )
        freeCompilerArgs.add("-Xallow-kotlin-package")
        val renderDiagnosticNames by extra(project.kotlinBuildProperties.renderDiagnosticNames)
        if (renderDiagnosticNames) {
            freeCompilerArgs.add("-Xrender-internal-diagnostic-names")
        }
    }

    sourceSets {
        val jsMain by getting {
            if (!kotlinBuildProperties.isInIdeaSync) {
                kotlin.srcDir("$jsStdlibSources/org.w3c")
                kotlin.srcDir("$jsStdlibSources/kotlinx")
                kotlin.srcDir("$jsStdlibSources/kotlin/browser")
                kotlin.srcDir("$jsStdlibSources/kotlin/dom")
            }
            dependencies {
                api(kotlinStdlib())
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile>().configureEach {
    dependsOn(prepareFriendStdlibJs)
    libraries.setFrom(prepareFriendStdlibJs)
    friendPaths.setFrom(libraries)
}

val emptyJavadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        named<MavenPublication>("js") {
            artifact(emptyJavadocJar)
            artifactId = project.name

            configureKotlinPomAttributes(
                project,
                explicitDescription = "Kotlin DOM API compatibility library",
                packaging = "klib",
            )
        }

        configureSbom(
            target = "Js",
            gradleConfigurations = setOf("jsRuntimeClasspath"),
            publication = named<MavenPublication>("js"),
        )
    }
}

// Disabling tasks for KMP/root publication as JS one should replace it
tasks.configureEach {
    if (name.contains("KotlinMultiplatformPublication")) {
        enabled = false
    }
}

configureDefaultPublishing()
