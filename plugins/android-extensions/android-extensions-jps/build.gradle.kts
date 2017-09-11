
apply { plugin("kotlin") }

dependencies {
    val compile by configurations
    compile(project(":compiler:util"))
    compile(project(":jps-plugin"))
    compile(project(":android-extensions-compiler"))
    compile(ideaPluginDeps("android-jps-plugin", plugin = "android", subdir = "lib/jps"))
}

configureKotlinProjectSourcesDefault()
configureKotlinProjectNoTests()

