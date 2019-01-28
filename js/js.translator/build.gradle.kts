import org.gradle.plugins.ide.idea.model.IdeaModel

plugins {
    idea
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":core:descriptors"))
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:backend-common"))
    compile(project(":js:js.ast"))
    compile(project(":js:js.frontend"))
    compile(project(":js:js.parser"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeJars("trove4j", "guava", rootProject = rootProject) }
}

sourceSets {
    "main" {
        projectDefault()
        java.srcDir("../js.inliner/src")
    }
    "test" {}
}

configure<IdeaModel> {
    module {
        excludeDirs = excludeDirs + files("testData/out-min")
    }
}