import org.gradle.plugins.ide.idea.model.IdeaModel

plugins {
    idea
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":core:descriptors"))
    api(project(":compiler:util"))
    api(project(":compiler:frontend"))
    api(project(":compiler:backend-common"))
    api(project(":js:js.ast"))
    api(project(":js:js.frontend"))
    api(project(":js:js.parser"))
    compileOnly(project(":js:js.sourcemap"))
    compileOnly(intellijCore())
    compileOnly(libs.guava)
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
        excludeDirs = excludeDirs + files("testData/out", "testData/out-min")
    }
}
