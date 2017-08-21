
apply { plugin("kotlin") }

dependencies {
    compile(project(":compiler:util"))
    compile(project(":jps-plugin"))
    compile(project(":android-extensions-compiler"))
    compile(ideaPluginDeps("android-jps-plugin", plugin = "android", subdir = "lib/jps"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

