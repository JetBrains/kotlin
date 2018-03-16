import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

description = "Kotlin JVM metadata manipulation library"

plugins {
    kotlin("jvm")
}

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
    testRuntime(projectDist(":kotlin-reflect"))
}

noDefaultJar()

val shadowJar = task<ShadowJar>("shadowJar") {
    callGroovy("manifestAttributes", manifest, project)
    manifest.attributes["Implementation-Version"] = version

    from(the<JavaPluginConvention>().sourceSets["main"].output)
    exclude("**/*.proto")
    configurations = listOf(shadows)

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

// Change this version before publishing
version = "0.1-SNAPSHOT"
// publish()

projectTest {
    workingDir = rootDir
}
