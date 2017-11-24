
apply { plugin("kotlin") }

configureIntellijPlugin()

dependencies {
    compile(projectDist(":kotlin-stdlib"))
    compile(project(":core:util.runtime"))
    compile(project(":compiler:backend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":idea:ide-common"))
    compile(project(":plugins:uast-kotlin"))
}

afterEvaluate {
    dependencies {
        compileOnly(intellij { include("openapi.jar", "util.jar") })
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

