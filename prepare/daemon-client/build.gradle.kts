
description = "Kotlin Daemon Client"

apply { plugin("kotlin") }

val nativePlatformUberjar = "$rootDir/dependencies/native-platform-uberjar.jar"

dependencies {
    val compile by configurations
    compile(project(":compiler:util"))
    compile(project(":compiler:cli-common"))
    compile(project(":compiler:daemon-common"))
    compile(files(nativePlatformUberjar))
    buildVersion()
}

sourceSets {
    "main" { default() }
    "test" { none() }
}

runtimeJar {
    from(zipTree(nativePlatformUberjar))
}

sourcesJar()
javadocJar()

dist()

publish()
