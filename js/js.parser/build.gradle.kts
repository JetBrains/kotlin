
apply { plugin("kotlin") }

dependencies {
    compile(project(":compiler:util"))
    compile(project(":js:js.ast"))
    compile(ideaSdkCoreDeps("intellij-core"))
    compile(preloadedDeps("json-org"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

