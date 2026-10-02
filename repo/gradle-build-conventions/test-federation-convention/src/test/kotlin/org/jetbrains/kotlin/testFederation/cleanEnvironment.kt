/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

fun cleanEnvironment(): Map<String, String> =
    System.getenv().toMutableMap().apply {
        remove(TEST_FEDERATION_ENABLED_ENV_KEY)
        remove(TEST_FEDERATION_MODE_ENV_KEY)
        remove(TEST_FEDERATION_AFFECTED_DOMAINS_ENV_KEY)
        remove(TEST_FEDERATION_CHANGED_DOMAINS_ENV_KEY)
        remove(TEST_FEDERATION_SUBSETS_ENV_KEY)
    }
