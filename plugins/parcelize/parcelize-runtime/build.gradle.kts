description = "Runtime library for the Parcelize compiler plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(kotlinStdlib())
    api(project(":kotlin-android-extensions-runtime"))
    compileOnly(commonDependency("com.google.android", "android"))

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

publish {
    artifactId = "kotlin-parcelize-runtime"
}

runtimeJar()
sourcesJar()
javadocJar()
