plugins {
    kotlin("jvm") version "1.7.20"
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies")
}

dependencies {
    implementation("org.jetbrains.dukat:dukat:0.5.8-rc.5")
    implementation("org.jsoup:jsoup:1.14.2")
}

task("downloadIDL", JavaExec::class) {
    main = "org.jetbrains.kotlin.tools.dukat.DownloadKt"
    classpath = sourceSets["main"].runtimeClasspath
    dependsOn("build")
}

task("generateStdlibFromIDL", JavaExec::class) {
    main = "org.jetbrains.kotlin.tools.dukat.LaunchJsKt"
    classpath = sourceSets["main"].runtimeClasspath
    dependsOn("build")
    systemProperty("line.separator", "\n")
}

// Configured version of Dukat creates incorrect declarations for kotlin/wasm, pathed version located in yakovlev/dynamicAsType
// After patched version be published we need to update dukat version in dependencies here.
task("generateWasmStdlibFromIDL", JavaExec::class) {
    main = "org.jetbrains.kotlin.tools.dukat.LaunchWasmKt"
    classpath = sourceSets["main"].runtimeClasspath
    dependsOn("build")
    systemProperty("line.separator", "\n")
}
