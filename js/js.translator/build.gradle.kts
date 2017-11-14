import org.gradle.plugins.ide.idea.model.IdeaModel

plugins {
    idea
}

apply { plugin("kotlin") }

jvmTarget = "1.6"

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
    compile(project(":compiler:ir.tree"))
    compile(project(":compiler:ir.psi2ir"))
}

sourceSets {
    "main" {
        projectDefault()
        java.srcDir("../js.inliner/src")
        java.srcDir("../../compiler/ir/backend.js/src")
    }
    "test" {}
}

configure<IdeaModel> {
    module {
        excludeDirs = excludeDirs + files("testData/out-min")
    }
}