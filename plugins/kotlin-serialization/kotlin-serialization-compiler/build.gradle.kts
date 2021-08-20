description = "Kotlin Serialization Compiler Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(intellijCoreDep()) { includeJars("intellij-core", "asm-all", rootProject = rootProject) }

    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:backend"))
    compileOnly(project(":compiler:ir.backend.common"))
    compileOnly(project(":js:js.frontend"))
    compileOnly(project(":js:js.translator"))
    compileOnly(project(":kotlin-util-klib-metadata"))

    runtimeOnly(kotlinStdlib())

    testCompile(projectTests(":compiler:tests-common"))
    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":compiler:tests-compiler-utils"))
    testApi(projectTests(":compiler:tests-common-new"))
    testImplementation(projectTests(":generators:test-generator"))
    testCompile(commonDep("junit:junit"))
    testApiJUnit5(vintageEngine = true)

    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.2.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.2")

    testRuntimeOnly(intellijCoreDep()) { includeJars("intellij-core") }
    testRuntimeOnly(intellijDep()) { includeJars("platform-concurrency") }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
    kotlinOptions {
        freeCompilerArgs += "-Xopt-in=org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI"
    }
}

runtimeJar()
sourcesJar()
javadocJar()
testsJar()

projectTest(parallel = true, jUnit5Enabled = true) {
    workingDir = rootDir
    useJUnitPlatform()
}

val generateTests by generator("org.jetbrains.kotlinx.serialization.TestGeneratorKt")