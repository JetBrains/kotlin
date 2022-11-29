import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

description = "Kotlin JVM metadata manipulation library"

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
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
configurations.getByName("compileOnly").extendsFrom(shadows)
configurations.getByName("testApi").extendsFrom(shadows)

dependencies {
    api(kotlinStdlib())
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
    nonPublicMarkers.addAll(listOf(
        "kotlinx.metadata.internal.IgnoreInApiDump",
        "kotlinx.metadata.jvm.internal.IgnoreInApiDump"
    ))
}

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
