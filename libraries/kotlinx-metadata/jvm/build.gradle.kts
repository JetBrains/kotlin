import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

description = "Kotlin JVM metadata manipulation library"

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
    id("org.jetbrains.dokka")
}

/*
 * To publish this library use `:kotlinx-metadata-jvm:publish` task and specify the following parameters
 *
 *      - `-PdeployVersion=1.2.nn`: the version of the standard library dependency to put into .pom
 *      - `-PkotlinxMetadataDeployVersion=0.0.n`: the version of the library itself
 *      - `-PdeployRepoUrl=repository_url`: (optional) the url of repository to deploy to;
 *          if not specified, the local directory repository `build/repo` will be used
 *      - `-PdeployRepoUsername=username`: (optional) the username to authenticate in the deployment repository
 *      - `-PdeployRepoPassword=password`: (optional) the password to authenticate in the deployment repository
 */
group = "org.jetbrains.kotlinx"
val deployVersion = findProperty("kotlinxMetadataDeployVersion") as String?
version = deployVersion ?: "0.1-SNAPSHOT"

//kotlin {
//    explicitApiWarning()
//}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

val shadows by configurations.creating {
    isTransitive = false
}
val proguardDeps by configurations.creating

configurations.getByName("compileOnly").extendsFrom(shadows)
configurations.getByName("testApi").extendsFrom(shadows)

dependencies {
    api(kotlinStdlib())
    proguardDeps(kotlinStdlib())

    shadows(project(":kotlinx-metadata"))
    shadows(project(":core:metadata"))
    shadows(project(":core:metadata.jvm"))
    shadows(protobufLite())
    testImplementation(project(":kotlin-test:kotlin-test-junit"))
    testImplementation(commonDependency("junit:junit"))
    testImplementation(commonDependency("org.jetbrains.intellij.deps:asm-all"))
    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
}

if (deployVersion != null) {
    publish()
}

val shadowJarTask = runtimeJar(tasks.register<ShadowJar>("shadowJar")) {
    callGroovy("manifestAttributes", manifest, project)
    manifest.attributes["Implementation-Version"] = archiveVersion

    from(mainSourceSet.output)
    exclude("**/*.proto")
    configurations = listOf(shadows)
    relocate("org.jetbrains.kotlin", "kotlinx.metadata.internal")
}

val test by tasks
test.dependsOn("shadowJar")

tasks.apiBuild {
    inputJar.value(shadowJarTask.flatMap { it.archiveFile })
}

apiValidation {
    ignoredPackages.add("kotlinx.metadata.internal")
    nonPublicMarkers.addAll(
        listOf(
            "kotlinx.metadata.internal.IgnoreInApiDump",
            "kotlinx.metadata.jvm.internal.IgnoreInApiDump"
        )
    )
}

tasks.dokkaHtml.configure {
    outputDirectory.set(buildDir.resolve("dokka"))

    dokkaSourceSets.configureEach {
        sourceRoots.from(project(":kotlinx-metadata").getSources())

        skipDeprecated.set(true)

        perPackageOption {
            matchingRegex.set("kotlinx\\.metadata\\.internal(\$|\\.).*")
            suppress.set(true)
            reportUndocumented.set(false)
        }
    }
}


val proguard by task<CacheableProguardTask> {
    dependsOn(shadowJarTask)

    injars(shadowJarTask.flatMap { it.archiveFile })
    outjars(fileFrom(base.libsDirectory.asFile.get(), "${base.archivesName.get()}-$version-proguard.jar"))

    javaLauncher.set(project.getToolchainLauncherFor(chooseJdk_1_8ForJpsBuild(JdkMajorVersion.JDK_1_8)))
    libraryjars(mapOf("filter" to "!META-INF/versions/**"), proguardDeps)
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

    configuration("$projectDir/metadata-jvm.pro")
}

val proguardJar by task<Jar> {
    dependsOn(proguard)
    setupPublicJar(base.archivesName.get() + "-proguard",)
    from {
        zipTree(proguard.get().singleOutputFile())
    }
}

//tasks.named("install").configure {
//    dependsOn(proguardJar)
//}
//setPublishableArtifact(proguardJar)

sourcesJar {
    for (dependency in shadows.dependencies) {
        if (dependency is ProjectDependency) {
            val javaPlugin = dependency.dependencyProject.convention.findPlugin(JavaPluginConvention::class.java)
            if (javaPlugin != null) {
                from(javaPlugin.sourceSets["main"].allSource)
            }
        }
    }
}

javadocJar()
