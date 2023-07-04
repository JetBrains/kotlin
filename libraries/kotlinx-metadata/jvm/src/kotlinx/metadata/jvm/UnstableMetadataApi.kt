/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.jvm

/**
 * Marks an API that is still in development and not feature-complete or finalized.
 * Such an API does not provide compatibility guarantees.
 * It can be changed in future releases without migration aids or removed without replacement.
 */
@RequiresOptIn(
    "This part of API is not yet finished, does not provide any compatibility guarantees and can be changed in the future without notice",
    level = RequiresOptIn.Level.WARNING
)
@MustBeDocumented
public annotation class UnstableMetadataApi
