/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.types.KaStandardTypeClassIds
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.classId
import org.jetbrains.kotlin.analysis.api.types.isNullable

/**
 * Unlike AA, descriptors do verify nullability [org.jetbrains.kotlin.builtins.KotlinBuiltIns.isNothing]
 * See K1 usage [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportMapper.bridgeReturnType]
 */
context(session: KaSession)
fun isObjCNothing(type: KaType?): Boolean = type?.classId == KaStandardTypeClassIds.NOTHING && !type.isNullable
