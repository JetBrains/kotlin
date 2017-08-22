
apply { plugin("kotlin") }

dependencies {
    compile(project(":kotlin-stdlib"))
    compile(project(":core:util.runtime"))
    compile(project(":compiler:backend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":idea:ide-common"))
    compile(project(":plugins:uast-kotlin"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

