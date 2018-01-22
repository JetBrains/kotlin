
apply { plugin("kotlin") }
apply { plugin("jps-compatible") }

dependencies {
    compile(projectDist(":kotlin-stdlib"))
    compile(project(":core:util.runtime"))
    compile(project(":compiler:backend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":idea:ide-common"))
    compile(project(":plugins:uast-kotlin"))
    compileOnly(intellijDep()) { includeJars("openapi", "util", "platform-api") }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

