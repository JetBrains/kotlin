/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.allopen

object AllOpenPluginNames {
    val SUPPORTED_PRESETS = mapOf(
        "spring" to listOf(
            "org.springframework.stereotype.Component",
            "org.springframework.transaction.annotation.Transactional",
            "org.springframework.scheduling.annotation.Async",
            "org.springframework.cache.annotation.Cacheable",
            "org.springframework.boot.test.context.SpringBootTest",
            "org.springframework.validation.annotation.Validated"
        ),
        "quarkus" to listOf(
            "javax.enterprise.context.ApplicationScoped",
            "javax.enterprise.context.RequestScoped"
        ),
        "micronaut" to listOf(
            "io.micronaut.aop.Around",
            "io.micronaut.aop.Introduction",
            "io.micronaut.aop.InterceptorBinding",
            "io.micronaut.aop.InterceptorBindingDefinitions"
        )
    )

    const val PLUGIN_ID = "org.jetbrains.kotlin.allopen"
    const val ANNOTATION_OPTION_NAME = "annotation"
}
