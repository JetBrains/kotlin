#!/usr/bin/env kotlinc -cp dist/kotlinc/lib/kotlin-main-kts.jar -script

@file:Repository("https://packages.jetbrains.team/maven/p/teamcity-rest-client/teamcity-rest-client")
@file:DependsOn("org.jetbrains.teamcity:teamcity-rest-client:3.0.3")

import org.jetbrains.teamcity.rest.BuildId

val buildId = BuildId("test")
println(buildId.stringId)
