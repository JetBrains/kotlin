description = "Kotlin Library (KLIB) metadata manipulation library"

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

group = "org.jetbrains.kotlinx"

val deployVersion = findProperty("kotlinxMetadataKlibDeployVersion") as String?
version = deployVersion ?: "0.0.1-SNAPSHOT"

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

optInToK1Deprecation()

val embedded = configurations.getByName("embedded")
embedded.isTransitive = false
configurations.getByName("compileOnly").extendsFrom(embedded)
configurations.getByName("testApi").extendsFrom(embedded)

dependencies {
    api(kotlinStdlib())
    embedded(project(":kotlin-metadata"))
    embedded(project(":core:compiler.common"))
    embedded(project(":core:names"))
    embedded(project(":core:deserialization"))
    embedded(project(":core:deserialization.common"))
    embedded(project(":compiler:serialization.common"))
    embedded(project(":compiler:serialization"))
    embedded(project(":kotlin-util-klib-metadata"))
    embedded(project(":kotlin-util-klib"))
    embedded(project(":kotlin-util-io"))
    embedded(protobufLite())
    testImplementation(kotlinTest("junit5"))
}

if (deployVersion != null) {
    publish()
}

tasks.test {
    useJUnitPlatform()
}

runtimeJarWithRelocation {
    from(mainSourceSet.output)
    exclude("**/*.proto")
    relocate("org.jetbrains.kotlin", "kotlin.metadata.internal")
}

sourcesJar()

javadocJar()

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xallow-kotlin-package")
    }
}
