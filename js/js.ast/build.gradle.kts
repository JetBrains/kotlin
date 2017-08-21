
apply { plugin("kotlin") }

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(ideaSdkCoreDeps("trove4j", "intellij-core"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

