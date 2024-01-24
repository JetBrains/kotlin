/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.transformers.documentables

@Deprecated(
    message = "Declaration was moved to dokka-core",
    replaceWith = ReplaceWith("org.jetbrains.dokka.transformers.documentation.ClashingDriIdentifier"),
    level = DeprecationLevel.WARNING // TODO change to error after Kotlin 1.9.20
)
public typealias ClashingDriIdentifier = org.jetbrains.dokka.transformers.documentation.ClashingDriIdentifier
