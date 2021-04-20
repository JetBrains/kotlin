plugins {
    java
}

val embedded by configurations

dependencies {
    embedded(commonDep("com.android.tools:r8"))
    embedded(intellijCoreDep()){ includeJars("intellij-core") }
    embedded(intellijDep()) { includeJars("platform-concurrency") }
    embedded(intellijDep()) {
        includeJars(
            "testFramework",
            "testFramework.core",
            rootProject = rootProject
        )
    }
    embedded(intellijDep()) { includeJars("intellij-deps-fastutil-8.3.1-1") }
    embedded(intellijDep()) {
        includeJars(
            "jps-model",
            "extensions",
            "util",
            "platform-api",
            "platform-impl",
            "idea",
            "idea_rt",
            "guava",
            "trove4j",
            "asm-all",
            "log4j",
            "jdom",
            "streamex",
            "bootstrap",
            "jna",
            rootProject = rootProject
        )
        isTransitive = false
    }

    embedded(intellijDep()) { includeJars("platform-util-ui", "platform-concurrency", "platform-objectSerializer") }
    embedded(intellijDep()) { includeJars("platform-ide-util-io") }
}

publish()

runtimeJar() {
    isZip64 = true
}