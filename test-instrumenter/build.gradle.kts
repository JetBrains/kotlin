import org.gradle.jvm.tasks.Jar

plugins { java }
apply { plugin("kotlin") }

configureIntellijPlugin {
    setExtraDependencies("intellij-core")
}

dependencies {
    compile(project(":kotlin-stdlib"))
}

afterEvaluate {
    dependencies {
        compile(intellijCoreJar())
        compile(intellij { include("asm-all.jar") })
    }
}

sourceSets {
    "main" { projectDefault() }
}

tasks {
    "jar" {
        this as Jar
        manifest {
            attributes["Manifest-Version"] = 1.0
            attributes["PreMain-Class"] = "org.jetbrains.kotlin.testFramework.TestInstrumentationAgent"
        }
    }
}