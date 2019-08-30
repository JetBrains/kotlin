
plugins {
    kotlin("jvm")
}

jvmTarget = "1.6"

dependencies {
    compile(project(":kotlin-scripting-common"))
    compile(project(":compiler:cli-common"))
    compile(project(":js:js.engines"))
    compile(intellijCoreDep()) { includeJars("intellij-core") }
}

sourceSets {
    "main" { projectDefault() }
    "test" {  }
}

publish()

standardPublicJars()
