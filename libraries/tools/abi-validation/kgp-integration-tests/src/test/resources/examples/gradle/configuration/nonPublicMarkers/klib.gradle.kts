/*
 * Copyright 2016-2023 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation {
        filters.excluded.annotatedWith.addAll(
            "annotations.HiddenClass",
            "annotations.HiddenCtor",
            "annotations.HiddenProperty",
            "annotations.HiddenGetter",
            "annotations.HiddenSetter",
            "annotations.HiddenFunction"
        )
    }
}
