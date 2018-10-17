import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

description = "Kotlin JVM metadata manipulation library"

plugins {
    kotlin("jvm")
    id("jps-compatible")
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

jvmTarget = "1.6"
javaHome = rootProject.extra["JDK_16"] as String

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

val shadows by configurations.creating {
    isTransitive = false
}
configurations.getByName("compileOnly").extendsFrom(shadows)
configurations.getByName("testCompile").extendsFrom(shadows)

dependencies {
    compile(project(":kotlin-stdlib"))
    shadows(project(":kotlinx-metadata"))
    shadows(project(":core:metadata"))
    shadows(project(":core:metadata.jvm"))
    shadows(protobufLite())
    testCompile(commonDep("junit:junit"))
    testCompile(intellijDep()) { includeJars("asm-all", rootProject = rootProject) }
    testCompileOnly(project(":kotlin-reflect-api"))
    testRuntime(project(":kotlin-reflect"))
}

noDefaultJar()

task<ShadowJar>("shadowJar") {
    callGroovy("manifestAttributes", manifest, project)
    manifest.attributes["Implementation-Version"] = version

    from(mainSourceSet.output)
    exclude("**/*.proto")
    configurations = listOf(shadows)
    relocate("org.jetbrains.kotlin", "kotlinx.metadata.internal")

    val artifactRef = outputs.files.singleFile
    runtimeJarArtifactBy(this, artifactRef)
    addArtifact("runtime", this, artifactRef)
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

if (deployVersion != null) {
    publish()
}

