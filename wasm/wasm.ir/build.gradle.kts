plugins {
    kotlin("jvm")
    id("jps-compatible")
    kotlin("plugin.serialization") version "1.4.10"
}

dependencies {
    implementation(kotlinStdlib())
    testImplementation(commonDep("junit:junit"))
    testCompileOnly(project(":kotlin-test:kotlin-test-jvm"))
    testCompileOnly(project(":kotlin-test:kotlin-test-junit"))
    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.0")
}


val testSuiteRevision = "18f8340"
val testSuiteDir = File(buildDir, "testsuite")
val testSuiteZip = File(testSuiteDir, testSuiteRevision + ".zip")

val downloadTestSuite by task<de.undercouch.gradle.tasks.download.Download> {
    src("https://github.com/WebAssembly/testsuite/zipball/$testSuiteRevision")
    dest(testSuiteZip)
    overwrite(false)
}

val unzipTestSuite by task<Copy> {
    dependsOn(downloadTestSuite)
    from(zipTree(downloadTestSuite.get().dest))
    into(testSuiteDir)
}

val wabtDir = File(buildDir, "wabt")
val wabtVersion = "1.0.19"

val downloadWabt by task<de.undercouch.gradle.tasks.download.Download> {
    val gradleOs = org.gradle.internal.os.OperatingSystem.current()
    val os = when {
        gradleOs.isMacOsX -> "macos"
        gradleOs.isWindows -> "windows"
        gradleOs.isLinux -> "ubuntu"
        else -> error("Unsupported OS: $gradleOs")
    }
    val fileName = "wabt-$wabtVersion-$os.tar.gz"
    src("https://github.com/WebAssembly/wabt/releases/download/$wabtVersion/$fileName")
    dest(File(wabtDir, fileName))
    overwrite(false)
}

val unzipWabt by task<Copy> {
    dependsOn(downloadWabt)
    from(tarTree(resources.gzip(downloadWabt.get().dest)))
    into(wabtDir)
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += listOf(
        "-Xopt-in=kotlin.ExperimentalUnsignedTypes",
        "-Xskip-prerelease-check"
    )
}

projectTest("test", true) {
    dependsOn(unzipWabt)
    dependsOn(unzipTestSuite)
    systemProperty("wabt.bin.path", "$wabtDir/wabt-$wabtVersion/bin")
    systemProperty("wasm.testsuite.path", "$testSuiteDir/WebAssembly-testsuite-$testSuiteRevision")
    workingDir = projectDir
}

testsJar()