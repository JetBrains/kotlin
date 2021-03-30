plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.6"

dependencies {
    compile(kotlinStdlib())
    compile(project(":kotlin-scripting-dependencies"))
    compile("org.eclipse.aether:aether-api:1.1.0")
    compile("org.eclipse.aether:aether-impl:1.1.0")
    compile("org.eclipse.aether:aether-util:1.1.0")
    compile("org.eclipse.aether:aether-connector-basic:1.1.0")
    compile("org.eclipse.aether:aether-transport-wagon:1.1.0")
    compile("org.eclipse.aether:aether-transport-http:1.1.0")
    compile("org.eclipse.aether:aether-transport-file:1.1.0")
    compile("org.apache.maven:maven-core:3.6.3")
    compile("org.apache.maven:maven-settings-builder:3.6.3")
    compile("org.apache.maven:maven-aether-provider:3.3.9")
    compile("org.apache.maven.wagon:wagon-provider-api:3.3.4")
    testCompile(projectTests(":kotlin-scripting-dependencies"))
    testCompile(commonDep("junit"))
    testRuntimeOnly("org.slf4j:slf4j-nop:1.7.30")
    testImplementation(kotlin("reflect"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
    kotlinOptions.freeCompilerArgs += listOf(
        "-Xallow-kotlin-package", "-Xsuppress-deprecated-jvm-target-warning"
    )
}

publish()

runtimeJar()
sourcesJar()
javadocJar()
