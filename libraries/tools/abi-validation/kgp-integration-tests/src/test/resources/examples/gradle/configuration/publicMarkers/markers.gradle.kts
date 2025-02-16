/*
 * Copyright 2016-2022 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation {
        filters.included {
            classes.addAll("foo.api.**", "foo.PublicClass")
            annotatedWith.addAll("foo.PublicClass", "foo.PublicField", "foo.PublicProperty")
        }
    }
}
