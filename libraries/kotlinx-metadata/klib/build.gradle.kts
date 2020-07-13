import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

description = "Kotlin Library (KLIB) metadata manipulation library"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

group = "org.jetbrains.kotlinx"

val deployVersion = findProperty("kotlinxMetadataKlibDeployVersion") as String?
version = deployVersion ?: "0.0.1-SNAPSHOT"

sourceSets {
    "main" { projectDefault() }
}

val shadows by configurations.creating {
    isTransitive = false
}
configurations.getByName("compileOnly").extendsFrom(shadows)
configurations.getByName("testCompile").extendsFrom(shadows)

dependencies {
    compile(kotlinStdlib())
    shadows(project(":kotlinx-metadata"))
    // TODO: Get rid of this heavyweight dependency.
    shadows(project(":core:descriptors"))
    shadows(project(":core:metadata"))
    shadows(project(":core:deserialization"))
    shadows(project(":compiler:serialization"))
    shadows(project(":kotlin-util-klib-metadata"))
    shadows(project(":kotlin-util-klib"))
    shadows(protobufLite())
}

if (deployVersion != null) {
    publish()
}

noDefaultJar()

val shadowJar = tasks.register<ShadowJar>("shadowJar") {
    callGroovy("manifestAttributes", manifest, project)
    manifest.attributes["Implementation-Version"] = version

    from(mainSourceSet.output)
    exclude("**/*.proto")
    configurations = listOf(shadows)
    relocate("org.jetbrains.kotlin", "kotlinx.metadata.internal")
}

runtimeJarArtifactBy(shadowJar, shadowJar)

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