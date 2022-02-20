plugins {
    kotlin("jvm") version "1.6.10"
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies")
}

dependencies {
    implementation("org.jetbrains.dukat:dukat:0.5.8-rc.4")
    implementation("org.jsoup:jsoup:1.14.2")
}

task("downloadIDL", JavaExec::class) {
    main = "org.jetbrains.kotlin.tools.dukat.DownloadKt"
    classpath = sourceSets["main"].runtimeClasspath
    dependsOn(":dukat:build")
}

task("generateStdlibFromIDL", JavaExec::class) {
    main = "org.jetbrains.kotlin.tools.dukat.LaunchKt"
    classpath = sourceSets["main"].runtimeClasspath
    dependsOn(":dukat:build")
    systemProperty("line.separator", "\n")
}
