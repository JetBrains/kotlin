
apply { plugin("kotlin") }

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:cli-common"))
    compile(project(":idea"))
    compile(project(":idea:idea-jps-common"))
    compile(project(":idea:idea-gradle"))
    compile(project(":idea:idea-maven"))
    //compileOnly(ideaPluginDeps("maven", "maven-server-api", plugin = "maven"))
    compileOnly(ideaPluginDeps("gradle-tooling-api", "gradle", plugin = "gradle"))
    compileOnly(ideaSdkDeps("openapi", "idea"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

