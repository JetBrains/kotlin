import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.attributes.DocsType.SOURCES
import org.gradle.api.plugins.JavaPlugin.SOURCES_ELEMENTS_CONFIGURATION_NAME
import org.gradle.api.plugins.internal.JvmPluginsHelper.configureDocumentationVariantWithArtifact
import org.gradle.internal.jvm.Jvm
import java.util.*


description = "Stripped down variant of Kotlin Backend Native for Mobile"

plugins {
    kotlin("jvm")
    id("jps-compatible")
    `maven-publish`
}

version = "1.6.10-1" // CHANGE VERSION HERE

val relocate = false
val mobilePackage = when (relocate) {
    true -> "$kotlinEmbeddableRootPackage.mobile.${version.toString().replace(".", "_")}"
    false -> kotlinEmbeddableRootPackage
}

val jarBaseName = property("archivesBaseName") as String

val proguardLibraryJars by configurations.creating {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_API))
    }
}

val relocatedJarContents by configurations.creating
val embedded by configurations

dependencies {
    embedded(project(":kotlin-native:backend.native")) { isTransitive = false }
    //embedded(project(":prepare:ide-plugin-dependencies:kotlin-compiler-for-ide")) { isTransitive = false }

    proguardLibraryJars(kotlinStdlib())
    proguardLibraryJars(project(":kotlin-native:backend.native", "cli_bc"))

    relocatedJarContents(embedded)
}

noDefaultJar()

val relocatedJar by task<ShadowJar> {
    configurations = listOf(relocatedJarContents)
    duplicatesStrategy = DuplicatesStrategy.FAIL
    destinationDirectory.set(File(buildDir, "libs"))
    archiveClassifier.set("before-proguard")

    if (relocate) relocate(kotlinEmbeddableRootPackage, mobilePackage)
}

val proguard by task<CacheableProguardTask> {
    val packageName = mobilePackage //kotlinEmbeddableRootPackage //
    dependsOn(relocatedJar)

    buildDir.mkdirs()
    val config = fileFrom(buildDir, "backend-native-mobile.pro")
    config.writeText("${fileFrom(projectDir, "backend-native-mobile.pro").readText()}\n" +
            "-keep public class !$packageName.backend.konan.objcexport.ObjCExport,$packageName.backend.konan.objcexport.** { public *; }\n" +
            "-keep public class $packageName.backend.konan.ObjCInteropKt { public *; }\n" +
            "-keep public class $packageName.backend.konan.ObjCOverridabilityCondition { public *; }\n" +
            "-keep public class $packageName.cli.bc.K2NativeCompilerArguments* { public *; }\n") //$mobilePackage
    configuration(config)

    injars(mapOf("filter" to "!META-INF/versions/**"), relocatedJar.get().outputs.files)

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
                        )
                    },
                    javaLauncher.map {
                        firstFromJavaHomeThatExists(
                                "jre/lib/jsse.jar",
                                "../Classes/jsse.jar",
                                jdkHome = it.metadata.installationPath.asFile
                        )
                    },
                    javaLauncher.map {
                        Jvm.forHome(it.metadata.installationPath.asFile).toolsJar
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

// includes more sources than left by proguard
configureDocumentationVariantWithArtifact(
        SOURCES_ELEMENTS_CONFIGURATION_NAME, null, SOURCES, listOf(), "sourcesJar",
        project(":kotlin-native:backend.native").sourceSets["cli_bc"].allSource +
                project(":kotlin-native:backend.native").sourceSets["compiler"].allSource,
        components["java"] as AdhocComponentWithVariants, configurations, tasks, objects
)

addArtifact("runtime", resultJar)
addArtifact("runtimeElements", resultJar)
addArtifact("archives", resultJar)

publishing {
    repositories {
        maven { url = uri("$buildDir/repo") }

        maven {
            name = "idePluginDependencies"
            url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies")

            credentials {
                val file = rootProject.file("local.properties")
                if (file.exists()) {
                    /* To publish for Mobile IDE (you need Kotlin Project Member role):
                      1. Go to https://kotlin.jetbrains.space/p/kotlin/packages/maven/kotlin-ide-plugin-dependencies
                      2. Click Connect and generate a personal token
                      3. Add credentials to `kotlin-native/local.properties` as `idePluginDependenciesRepo{Username,Password}`
                      4. Set appropriate version below
                      5. Run `publishMavenMobilePublicationToIdePluginDependenciesRepository` gradle task */
                    val localProperties = Properties().apply { file.inputStream().use { load(it) } }
                    username = localProperties.getProperty("idePluginDependenciesRepoUsername")
                    password = localProperties.getProperty("idePluginDependenciesRepoPassword")
                }
            }
        }
    }

    publications {
        create<MavenPublication>("mavenMobile") {
            groupId = "org.jetbrains.kotlin"
            artifactId = "backend.native.mobile"

            from(components["java"])
        }
    }
}
