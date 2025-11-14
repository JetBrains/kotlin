import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

description = "Kotlin JVM metadata manipulation library"
group = "org.jetbrains.kotlin"

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
    id("org.jetbrains.dokka")
    id("project-tests-convention")
}


sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

val embedded by configurations
embedded.isTransitive = false
configurations.getByName("compileOnly").extendsFrom(embedded)
configurations.getByName("testApi").extendsFrom(embedded)

val proguardLibraryJars by configurations.creating {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
    }
}

dependencies {
    api(kotlinStdlib())
    embedded(project(":kotlin-metadata"))
    embedded(project(":core:metadata"))
    embedded(project(":core:metadata.jvm"))
    embedded(protobufLite())
    testImplementation(kotlinTest("junit5"))
    testImplementation(libs.intellij.asm)
    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }

    proguardLibraryJars(kotlinStdlib())
}

kotlin {
    explicitApi()
    compilerOptions {
        freeCompilerArgs.add("-Xallow-kotlin-package")
    }
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5)
}

publish()

val unshaded by task<Jar> {
    archiveClassifier.set("unshaded")
    from(mainSourceSet.output)
}
project.addArtifact("unshaded", unshaded, unshaded)

val relocatedJar by task<ShadowJar> {
    configurations = listOf(embedded)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    destinationDirectory.set(layout.buildDirectory.dir("libs"))
    archiveClassifier.set("shadow")

    from(mainSourceSet.output)
    exclude("**/*.proto")
    relocate("org.jetbrains.kotlin", "kotlin.metadata.internal")
}

val proguard by task<CacheableProguardTask> {
    dependsOn(relocatedJar)

    injars(mapOf("filter" to "!META-INF/versions/**"), relocatedJar.get().outputs.files)
    outjars(fileFrom(base.libsDirectory.asFile.get(), "${base.archivesName.get()}-$version-proguard.jar"))

    javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_1_8))

    libraryjars(mapOf("filter" to "!META-INF/versions/**"), proguardLibraryJars)
    libraryjars(
        project.files(
            javaLauncher.map {
                firstFromJavaHomeThatExists(
                    "jre/lib/rt.jar",
                    "../Classes/classes.jar",
                    jdkHome = it.metadata.installationPath.asFile
                )!!
            }
        )
    )

    configuration("metadata.pro")
}

val resultJar by task<Jar> {
    val pack = if (kotlinBuildProperties.proguard) proguard else relocatedJar
    dependsOn(pack)
    setupPublicJar(base.archivesName.get())
    from {
        zipTree(pack.get().singleOutputFile(layout))
    }

    manifest {
        attributes("Automatic-Module-Name" to "kotlin.metadata.jvm")
    }
}

setPublishableArtifact(resultJar)

tasks.apiBuild {
    dependsOn(tasks.jar)
    inputJar.value(resultJar.flatMap { it.archiveFile })
}

apiValidation {
    ignoredPackages.add("kotlin.metadata.internal")
    nonPublicMarkers.add("kotlin.metadata.internal.IgnoreInApiDump")
}

tasks.dokkaHtml.configure {
    outputDirectory.set(layout.buildDirectory.dir("dokka"))
    pluginsMapConfiguration.set(
        mapOf(
            "org.jetbrains.dokka.base.DokkaBase"
                    to """{ "templatesDir": "${projectDir.toString().replace('\\', '/')}/dokka-templates" }"""
        )
    )

    dokkaSourceSets.configureEach {
        includes.from(project.file("dokka/moduledoc.md").path)

        sourceRoots.from(project(":kotlin-metadata").getSources())

        skipDeprecated.set(true)
        reportUndocumented.set(true)
        failOnWarning.set(true)

        perPackageOption {
            matchingRegex.set("kotlin\\.metadata\\.internal(\$|\\.).*")
            suppress.set(true)
            reportUndocumented.set(false)
        }
    }
}

sourcesJar()

javadocJar()
