description = "Kotlin Library (KLIB) metadata manipulation library"
group = "org.jetbrains.kotlinx"

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    `java-library`
}

val deployVersion = findProperty("kotlinxMetadataKlibDeployVersion") as String?
version = deployVersion ?: "0.0.1-SNAPSHOT"

val embedded = configurations.embedded.get()
embedded.isTransitive = false
configurations.compileOnly.get().extendsFrom(embedded)

dependencies {
    api(kotlinStdlib())
    embedded(project(":kotlinx-metadata-klib")) { isTransitive = false }
    embedded(project(":kotlin-metadata"))
    embedded(project(":core:compiler.common"))
    embedded(project(":core:names"))
    embedded(project(":core:deserialization.common"))
    embedded(project(":compiler:serialization.common"))
    embedded(project(":kotlin-util-klib-metadata"))
    embedded(project(":kotlin-util-klib"))
    embedded(project(":kotlin-util-io"))
    embedded(protobufLite())
}

if (deployVersion != null) {
    publish()
}

runtimeJarWithRelocation {
    exclude("**/*.proto")
    relocate("org.jetbrains.kotlin", "kotlin.metadata.internal")
}

sourcesJar()
javadocJar()
