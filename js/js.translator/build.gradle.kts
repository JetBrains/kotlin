
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
    compile(project(":compiler:ir.tree"))
    compile(project(":compiler:ir.psi2ir"))
    compile(ideaSdkCoreDeps("intellij-core"))
}

sourceSets {
    "main" {
        projectDefault()
        java.srcDir("../js.inliner/src")
        java.srcDir("../../compiler/ir/backend.js/src")
    }
    "test" {}
}
