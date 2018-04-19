
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(projectDist(":kotlin-stdlib"))
    compile(project(":core:util.runtime"))
    compile(project(":compiler:backend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":idea:ide-common"))
    compile(project(":plugins:uast-kotlin"))
    compileOnly(intellijDep())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

