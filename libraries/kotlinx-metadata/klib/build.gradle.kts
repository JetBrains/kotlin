import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

description = "Kotlin Library (KLIB) metadata manipulation library"

plugins {
    id("common-configuration")
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

val embedded = configurations.embedded.get()
embedded.isTransitive = false
configurations.compileOnly.get().extendsFrom(embedded)
configurations.testApi.get().extendsFrom(embedded)

dependencies {
    api(kotlinStdlib())
    api(project(":kotlin-metadata"))

    implementation(project(":core:compiler.common"))
    implementation(project(":core:names"))
    implementation(project(":core:deserialization"))
    implementation(project(":core:deserialization.common"))
    implementation(project(":compiler:serialization.common"))
    implementation(project(":compiler:serialization"))
    implementation(project(":kotlin-util-klib-metadata"))
    implementation(project(":kotlin-util-klib"))
    implementation(project(":kotlin-util-io"))
    implementation(protobufLite())

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

val relocatedJar = tasks.register<ShadowJar>("relocatedJar") {
    archiveClassifier.set("shadow")
    configurations.add(project.configurations["embedded"])
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(mainSourceSet.output)
    exclude("**/*.proto")
    relocate("org.jetbrains.kotlin", "kotlin.metadata.internal")
}

if (deployVersion != null) {
    publish()
    (components.findByName("kotlinLibrary") as? AdhocComponentWithVariants)?.withVariantsFromConfiguration(configurations.getByName("runtimeElements")) {
        skip()
    }
    val runtimeJar = tasks.register<Jar>("runtimeJar") {
        dependsOn(relocatedJar)
        from {
            zipTree(relocatedJar.get().outputs.files.singleFile)
        }
        setupPublicJar(project.extensions.getByType<BasePluginExtension>().archivesName.get())
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
    tasks.named("assemble").configure { dependsOn(runtimeJar) }
    configurations.named("publishedRuntime") {
        setExtendsFrom(emptyList())
        outgoing.artifacts.clear()
        project.addArtifact(this.name, runtimeJar, runtimeJar)
    }
    configurations.named("publishedCompile") {
        setExtendsFrom(emptyList())
        outgoing.artifacts.clear()
        project.addArtifact(this.name, runtimeJar, runtimeJar)
    }
    dependencies {
        "publishedCompile"(kotlinStdlib())
    }
}

tasks.test {
    useJUnitPlatform()
}

sourcesJar()

javadocJar()

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xallow-kotlin-package")
    }
}
