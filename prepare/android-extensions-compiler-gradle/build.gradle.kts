description = "Kotlin Android Extensions Compiler"

plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:backend"))
    compileOnly(project(":kotlin-android-extensions-runtime"))
    runtimeOnly(project(":kotlin-compiler-embeddable"))
    compileOnly(commonDependency("com.google.android", "android"))
    compileOnly(intellijCore())

    embedded(project(":plugins:android-extensions-compiler")) { isTransitive = false }
    embedded(project(":kotlin-android-extensions-runtime")) { isTransitive = false }

    val httpClientVersion = libs.versions.http.client.get()
    val jsonVersion = libs.versions.json.get()
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.apache.httpcomponents" && requested.name == "httpclient") {
                useVersion(httpClientVersion)
            }
            if (requested.group == "org.json" && requested.name == "json") {
                useVersion(jsonVersion)
            }
        }
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

publish()

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())

sourcesJar()

javadocJar()

