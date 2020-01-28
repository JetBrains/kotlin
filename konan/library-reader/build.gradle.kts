plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

standardPublicJars()
