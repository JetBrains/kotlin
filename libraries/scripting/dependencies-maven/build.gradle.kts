import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
}

project.updateJvmTarget("1.8")

dependencies {
    implementation(kotlinStdlib())
    api(project(":kotlin-scripting-dependencies"))

    implementation("org.apache.maven:maven-core:3.8.8")
    implementation("org.apache.maven.wagon:wagon-http:3.5.3")
    implementation("org.apache.maven.resolver:maven-resolver-connector-basic:1.9.22")
    implementation("org.apache.maven.resolver:maven-resolver-transport-file:1.9.22")
    implementation("org.apache.maven.resolver:maven-resolver-transport-wagon:1.9.22")
    implementation("org.apache.maven.resolver:maven-resolver-impl:1.9.22")
    implementation(libs.apache.commons.io)

    testImplementation(projectTests(":kotlin-scripting-dependencies"))
    testImplementation(libs.junit4)
    testRuntimeOnly("org.slf4j:slf4j-nop:1.7.36")
    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testImplementation(libs.kotlinx.coroutines.core)

    constraints {
        api(libs.apache.commons.lang)
    }
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "com.google.guava" && requested.name == "guava") {
            useVersion("32.0.1-android")
            because("CVE-2023-2976")
        }
        if (requested.group == "commons-codec" && requested.name == "commons-codec") {
            useVersion("1.19.0")
            because("WS-2019-0379")
        }
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.freeCompilerArgs.add("-Xallow-kotlin-package")
}

publish()

runtimeJar()
sourcesJar()
javadocJar()
