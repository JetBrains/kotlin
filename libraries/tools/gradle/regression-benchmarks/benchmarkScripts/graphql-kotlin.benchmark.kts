/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// Quite big library written in Kotlin:
// - uses Spring boot
// - uses Kotlin kapt
@file:BenchmarkProject(
    name = "graphql-kotlin",
    gitUrl = "https://github.com/ExpediaGroup/graphql-kotlin.git",
    gitCommitSha = "fd1e9063f3aae144e099cdcfa69a4416fa434fb2"
)

import java.io.File

val stableReleasePatch = {
    "graphql-kotlin-1.7.20.patch" to File("benchmarkScripts/files/graphql-kotlin-1.7.20.patch")
        .readText()
        .byteInputStream()
}
val currentReleasePatch = {
    "graphql-kotlin-current.patch" to File("benchmarkScripts/files/graphql-kotlin-current.patch")
        .readText()
        .run { replace("<kotlin_version>", currentKotlinVersion) }
        .byteInputStream()
}

runAllBenchmarks(
    suite {
        scenario {
            title = "Spring server clean build"
            useGradleArgs("--no-build-cache")

            runTasks(":graphql-kotlin-spring-server:assemble")
            runCleanupTasks("clean")
        }

        scenario {
            title = "Spring client clean build"
            useGradleArgs("--no-build-cache")

            runTasks(":graphql-kotlin-spring-client:assemble")
            runCleanupTasks("clean")
        }

        scenario {
            title = "Ktor client clean build"
            useGradleArgs("--no-build-cache")

            runTasks(":graphql-kotlin-ktor-client:assemble")
            runCleanupTasks("clean")
        }

        scenario {
            title = "Incremental Spring server build with ABI change in FederatedSchemaGenerator"
            useGradleArgs("--no-build-cache")

            runTasks(":graphql-kotlin-spring-server:assemble")
            applyAbiChangeTo("generator/graphql-kotlin-federation/src/main/kotlin/com/expediagroup/graphql/generator/federation/FederatedSchemaGenerator.kt")
        }

        scenario {
            title = "Incremental Spring client build with ABI change in GraphQLClient"
            useGradleArgs("--no-build-cache")

            runTasks(":graphql-kotlin-spring-server:assemble")
            applyAbiChangeTo("clients/graphql-kotlin-client/src/main/kotlin/com/expediagroup/graphql/client/GraphQLClient.kt")
        }

        scenario {
            title = "Dry run configuration time"
            useGradleArgs("--no-build-cache", "-m")

            iterations = 20
            runTasks("assemble")
        }

        scenario {
            title = "No-op configuration time"
            useGradleArgs("--no-build-cache")

            iterations = 20
            runTasks("help")
        }

        scenario {
            title = "UP-TO-DATE configuration time"
            useGradleArgs("--no-build-cache")

            iterations = 20
            runTasks("assemble")
        }
    },
    mapOf(
        "1.7.20" to stableReleasePatch,
        "1.8.0" to currentReleasePatch
    )
)
