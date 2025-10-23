/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// Relatively big library written in Kotlin:
// - uses Spring boot
// - uses Kotlin kapt
@file:BenchmarkProject(
    name = "graphql-kotlin",
    gitUrl = "https://github.com/ExpediaGroup/graphql-kotlin.git",
    gitCommitSha = "13720ddabaa054970350135df60688b157156534",
    stableKotlinVersion = "2.2.21-RC2",
)

import java.io.File

val repoPatch = {
    listOf(
        "graphql-kotlin-current.patch" to File("benchmarkScripts/files/graphql-kotlin-repo.patch")
            .readText()
            .run { replace("<kotlin_version>", currentKotlinVersion) }
            .byteInputStream(),
    )
}

val defaultIterations = 20

runBenchmarks(
    repoPatch,
    suite {
        scenario {
            title = "Spring server clean build"
            useGradleArgs("--no-build-cache")
            iterations = defaultIterations

            runTasks(":graphql-kotlin-spring-server:assemble")
            runCleanupTasks("clean")

        }

        scenario {
            title = "Incremental Spring server build with ABI change in FederatedSchemaGenerator"
            useGradleArgs("--no-build-cache")
            iterations = defaultIterations

            runTasks(":graphql-kotlin-spring-server:assemble")
            applyAbiChangeTo("generator/graphql-kotlin-federation/src/main/kotlin/com/expediagroup/graphql/generator/federation/FederatedSchemaGenerator.kt")
        }

        scenario {
            title = "Incremental Spring client build with ABI change in GraphQLClient"
            useGradleArgs("--no-build-cache")
            iterations = defaultIterations

            runTasks(":graphql-kotlin-spring-server:assemble")
            applyAbiChangeTo("clients/graphql-kotlin-client/src/main/kotlin/com/expediagroup/graphql/client/GraphQLClient.kt")
        }

        scenario {
            title = "Dry run configuration time"
            useGradleArgs("--no-build-cache", "-m")
            iterations = defaultIterations

            runTasks("assemble")
        }

        scenario {
            title = "No-op configuration time"
            useGradleArgs("--no-build-cache")
            iterations = defaultIterations

            runTasks("help")
        }

        scenario {
            title = "UP-TO-DATE configuration time"
            useGradleArgs("--no-build-cache")
            iterations = defaultIterations

            runTasks("assemble")
        }
    }
)
