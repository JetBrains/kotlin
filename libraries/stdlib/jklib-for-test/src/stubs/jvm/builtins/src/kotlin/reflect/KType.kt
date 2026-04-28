/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect

public actual interface KType {
    public actual val classifier: KClassifier?
    public actual val arguments: List<KTypeProjection>
    public actual val isMarkedNullable: Boolean
}
