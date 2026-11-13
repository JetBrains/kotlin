import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

description = "Kotlin JVM metadata manipulation library"

// Do not reuse the published GA coordinates (org.jetbrains.kotlin:kotlin-metadata-jvm) here: the fat artifact with
// those coordinates is assembled by :prepare:kotlin-metadata-jvm, and two projects with the same group:name would
// make Gradle resolve project(":kotlin-metadata-jvm") dependencies to the prepare module (see KTI-3695 cleanup)
group = "org.jetbrains.kotlin.internal"

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("org.jetbrains.dokka")
}


sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

dependencies {
    api(kotlinStdlib())
    api(project(":kotlin-metadata"))
    implementation(project(":core:metadata"))
    implementation(project(":core:metadata.jvm"))
    implementation(protobufLite())

    testImplementation(kotlinTest("junit5"))
    testImplementation(libs.intellij.asm)
    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
}

kotlin {
    explicitApi()
    compilerOptions {
        freeCompilerArgs.add("-Xallow-kotlin-package")

        // Keep the *.kotlin_module file name (and therefore the module identity the compiler sees) stable
        // regardless of the Gradle group assigned to this project above
        moduleName.set("org.jetbrains.kotlin_kotlin-metadata-jvm")
    }

    tasks.named<KotlinCompile>("compileTestKotlin").configure {
        compilerOptions.freeCompilerArgs.addAll("-Xcompanion-blocks", "-Xcompanion-blocks-and-extensions")
    }
}

projectTests {
    testTask()
}

val unshaded = tasks.register<Jar>("unshaded") {
    archiveClassifier.set("unshaded")
    from(mainSourceSet.output)
}
project.addArtifact("unshaded", unshaded, unshaded)

dokka {
    dokkaGeneratorIsolation = ProcessIsolation {
        // enable support for kotlin package - required with K2 analysis
        systemProperties.put("org.jetbrains.dokka.analysis.allowKotlinPackage", "true")
    }

    dokkaPublications.html {
        outputDirectory.set(layout.buildDirectory.dir("dokka"))
        failOnWarning.set(true)
    }
    pluginsConfiguration.html {
        templatesDir.set(projectDir.resolve("dokka-templates"))
    }

    dokkaSourceSets.configureEach {
        includes.from(project.file("dokka/moduledoc.md").path)

        val kotlinMetadataSources = configurations.detachedConfiguration(dependencies.project(":kotlin-metadata")).apply {
            attributes {
                attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category.VERIFICATION))
                attribute(VerificationType.VERIFICATION_TYPE_ATTRIBUTE, project.objects.named(VerificationType.MAIN_SOURCES))
            }
            isTransitive = false
        }
        sourceRoots.from(kotlinMetadataSources)

        skipDeprecated.set(true)
        reportUndocumented.set(true)

        perPackageOption {
            matchingRegex.set("kotlin\\.metadata\\.internal(\$|\\.).*")
            suppress.set(true)
            reportUndocumented.set(false)
        }
    }
}

sourcesJar()

javadocJar()
