
apply { plugin("kotlin") }

jvmTarget = "1.6"

configureIntellijPlugin {
    setExtraDependencies("intellij-core")
}

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":js:js.ast"))
    compile(project(":js:js.parser"))
    compile(project(":js:js.serializer"))
}

afterEvaluate {
    dependencies {
        compile(intellijCoreJar())
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

