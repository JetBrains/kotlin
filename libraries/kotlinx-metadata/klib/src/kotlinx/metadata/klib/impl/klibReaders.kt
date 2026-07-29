/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.klib.impl

import kotlin.metadata.KmAnnotation
import kotlin.metadata.KmType
import kotlin.metadata.internal.ReadContextExtension

class KlibTypeReadExtension(val processType: (KmType) -> Unit) : ReadContextExtension

class KlibAnnotationReadExtension(val processAnnotation: (KmAnnotation) -> Unit) : ReadContextExtension
