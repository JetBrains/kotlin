
plugins {
    kotlin("jvm")
}

dependencies {
    compile(project(":kotlin-scripting-jvm"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
