
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":compiler:util"))
    compile(project(":js:js.ast"))
    compile(project(":js:js.translator"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly("com.eclipsesource.j2v8:j2v8_linux_x86_64:4.6.0")
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
