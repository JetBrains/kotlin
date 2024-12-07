description = "Kotlin Android Extensions Runtime"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(kotlinStdlib())
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

publish()

runtimeJar()
sourcesJar()
javadocJar()
