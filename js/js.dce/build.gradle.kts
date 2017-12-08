
apply { plugin("kotlin") }

dependencies {
    compile(project(":compiler:util"))
    compile(project(":js:js.ast"))
    compile(project(":js:js.translator"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

