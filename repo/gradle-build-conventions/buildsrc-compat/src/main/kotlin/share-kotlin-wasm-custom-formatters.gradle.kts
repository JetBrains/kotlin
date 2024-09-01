/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

private val CUSTOM_FORMATTERS_ATTRIBUTE = objects.named<Usage>("devtools-custom-previews")

val wasmCustomFormatters by configurations.creating {
    isCanBeResolved = false
    isCanBeConsumed = false
    isCanBeDeclared = true
    isVisible = false
}

val wasmCustomFormattersProvider by configurations.creating {
    isCanBeResolved = false
    isCanBeConsumed = true
    isCanBeDeclared = false
    isVisible = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, CUSTOM_FORMATTERS_ATTRIBUTE)
    }
}

val wasmCustomFormattersResolver by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    isCanBeDeclared = false
    isVisible = false
    extendsFrom(wasmCustomFormatters)
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, CUSTOM_FORMATTERS_ATTRIBUTE)
    }
}
