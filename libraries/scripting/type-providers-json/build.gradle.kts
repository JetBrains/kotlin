
plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("com.github.johnrengelman.shadow")
}

dependencies {
    compileOnly(kotlinStdlib())
    implementation(project(":kotlin-scripting-type-providers"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.14.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.10.+")

    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":kotlin-scripting-common"))
    compileOnly(project(":kotlin-scripting-jvm"))
    compileOnly(project(":kotlin-scripting-jvm-host-unshaded"))
    embedded(project(":kotlin-scripting-dependencies")) { isTransitive = false }

    testCompile(commonDep("junit"))
    testCompile(project(":compiler:util"))
    testCompile(project(":compiler:cli"))
    testCompile(project(":compiler:cli-common"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectTests(":kotlin-scripting-type-providers"))

    testCompile(project(":compiler:frontend"))
    testCompile(project(":compiler:plugin-api"))
    testCompile(project(":compiler:frontend.java"))
    testCompile(project(":compiler:backend.js"))

    testImplementation(intellijCoreDep()) { includeJars("intellij-core") }
    testRuntimeOnly(intellijDep()) { includeJars("jps-model") }
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
testsJar()

projectTest(parallel = true) {
    dependsOn(":dist")
    workingDir = rootDir
    systemProperty("kotlin.test.script.classpath", testSourceSet.output.classesDirs.joinToString(File.pathSeparator))
}