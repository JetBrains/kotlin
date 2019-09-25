
plugins {
    kotlin("jvm")
}

jvmTarget = "1.6"

dependencies {
    compile(project(":kotlin-scripting-common"))
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
