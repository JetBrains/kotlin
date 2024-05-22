/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.rhizomedb.fir

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object RhizomedbPackages {
    val packageFqName = FqName("com.jetbrains.rhizomedb")
}

object RhizomedbAnnotations {
    val generatedEntityTypeFqName = FqName("com.jetbrains.rhizomedb.GeneratedEntityType")
}

object RhizomedbSymbolNames {
    val entityTypeClassId = ClassId(RhizomedbPackages.packageFqName, Name.identifier("EntityType"))
}
