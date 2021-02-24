
plugins {
    kotlin("jvm")
}

dependencies {
    compile(project(":kotlin-scripting-jvm"))
    compile(project(":kotlin-scripting-dependencies"))
    compile(project(":kotlin-scripting-dependencies-maven"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { }
}
