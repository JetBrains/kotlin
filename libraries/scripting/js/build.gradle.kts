
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":kotlin-scripting-common"))
    compileOnly(project(":compiler:backend.js"))
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":js:js.engines"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {  }
}

publish()

standardPublicJars()
