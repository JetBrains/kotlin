
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.6"

dependencies {
    compile(kotlinStdlib())
    compile(project(":kotlin-scripting-dependencies"))
    compile("org.jetbrains.kotlin:jcabi-aether:1.0-dev-3")
    compile("org.sonatype.aether:aether-api:1.13.1")
    testCompile("org.apache.maven:maven-settings-builder:3.0.5")
    testCompile(projectTests(":kotlin-scripting-dependencies"))
    testCompile(commonDep("junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
    kotlinOptions.freeCompilerArgs += "-Xallow-kotlin-package"
}

publish()

runtimeJar()
sourcesJar()
javadocJar()
