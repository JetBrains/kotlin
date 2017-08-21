
apply { plugin("kotlin") }

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":js:js.ast"))
    compile(project(":js:js.parser"))
    compile(project(":js:js.serializer"))
    compile(ideaSdkCoreDeps("intellij-core"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

