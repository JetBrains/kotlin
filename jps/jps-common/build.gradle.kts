
apply { plugin("kotlin") }

configureIntellijPlugin {
    setExtraDependencies("intellij-core")
}

dependencies {
    compile(projectDist(":kotlin-stdlib"))
    compileOnly(project(":kotlin-reflect-api"))
    compile(project(":compiler:util"))
    compile(project(":compiler:cli-common"))
    compile(project(":compiler:frontend.java"))
}

afterEvaluate {
    dependencies {
        compile(intellijCoreJar())
        compile(intellij { include("util.jar") })
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

