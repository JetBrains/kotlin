import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.kotlin.dsl.support.serviceOf

plugins {
    kotlin("jvm")
}

idePluginDependency {
    if (!kotlinBuildProperties.isKotlinNativeEnabled) return@idePluginDependency

    description = "Stripped down variant of Kotlin Backend Native for IDE (AppCode KMM)"
    val jarBaseName = property("archivesBaseName") as String

    val proguardLibraryJars by configurations.creating {
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_API))
        }
    }
    val embedded by configurations
    val backendNativeSourcesConfiguration by configurations.creating {
        isVisible = false
        isCanBeResolved = true
        isCanBeConsumed = false
        attributes {
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
            attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.SOURCES))
        }
    }

    dependencies {
        embedded(project(":kotlin-native:backend.native")) { isTransitive = false }

        proguardLibraryJars(project(":native:kotlin-native-utils")) { isTransitive = false }
        proguardLibraryJars(project(":kotlin-native:backend.native", "kotlin_stdlib_jar"))
        proguardLibraryJars(project(":kotlin-native:backend.native", "kotlin_reflect_jar"))
        proguardLibraryJars(project(":kotlin-native:backend.native", "cli_bcApiElements"))
        backendNativeSourcesConfiguration(project(":kotlin-native:backend.native"))
    }

    noDefaultJar()

    val shadowJar by task<ShadowJar> {
        configurations = listOf(embedded)
        duplicatesStrategy = DuplicatesStrategy.FAIL
        destinationDirectory.set(File(buildDir, "libs"))
        archiveClassifier.set("shadow")
    }

    val proguard by task<CacheableProguardTask> {
        dependsOn(shadowJar)

        configuration(fileFrom(projectDir, "backend-native-for-ide.pro"))
        injars(mapOf("filter" to "!META-INF/versions/**"), shadowJar.get().outputs.files)
        outjars(fileFrom(buildDir, "libs", "$jarBaseName-$version-after-proguard.jar"))

        javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_1_8))

        libraryjars(mapOf("filter" to "!META-INF/versions/**"), proguardLibraryJars)
        libraryjars(
            files(
                javaLauncher.map {
                    firstFromJavaHomeThatExists(
                        "jre/lib/rt.jar",
                        "../Classes/classes.jar",
                        jdkHome = it.metadata.installationPath.asFile
                    )!!
                },
                javaLauncher.map {
                    firstFromJavaHomeThatExists(
                        "jre/lib/jsse.jar",
                        "../Classes/jsse.jar",
                        jdkHome = it.metadata.installationPath.asFile
                    )!!
                },
                javaLauncher.map {
                    org.gradle.internal.jvm.Jvm.forHome(it.metadata.installationPath.asFile).toolsJar!!
                }
            )
        )
    }

    val resultJar by task<Jar> {
        dependsOn(proguard)
        manifest.attributes.apply {
            put("Implementation-Vendor", "JetBrains")
            put("Implementation-Title", jarBaseName)
            put("Implementation-Version", version)
        }
        from {
            zipTree(proguard.get().singleOutputFile())
        }
    }

    setPublishableArtifact(resultJar)

    publish()

    sourcesJar {
        val archiveOperations = serviceOf<ArchiveOperations>()
        from(backendNativeSourcesConfiguration.map { archiveOperations.zipTree(it) })
        dependsOn(backendNativeSourcesConfiguration)
    }
}
