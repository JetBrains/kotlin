plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.8"

dependencies {
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":js:js.frontend"))
    compileOnly(project(":js:js.serializer"))
    compileOnly(intellijCore())

}

sourceSets {
    "main" { projectDefault() }
    "test" { }
}
