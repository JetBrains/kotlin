
apply { plugin("kotlin") }

jvmTarget = "1.6"

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":js:js.ast"))
    compile(project(":js:js.parser"))
    compile(project(":js:js.serializer"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeJars("guava-21.0") }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

