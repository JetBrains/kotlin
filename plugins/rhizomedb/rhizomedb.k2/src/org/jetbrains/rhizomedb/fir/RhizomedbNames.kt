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
    val manyFqName = FqName("com.jetbrains.rhizomedb.Many")
    val generatedEntityTypeFqName = FqName("com.jetbrains.rhizomedb.GeneratedEntityType")
    val valueAttributeFqName = FqName("com.jetbrains.rhizomedb.ValueAttribute")
    val transientAttributeFqName = FqName("com.jetbrains.rhizomedb.TransientAttribute")
    val referenceAttributeFqName = FqName("com.jetbrains.rhizomedb.RefAttribute")
    val entityConstructorFqName = FqName("com.jetbrains.rhizomedb.EntityConstructor")

    val manyAnnotationClassId = ClassId.topLevel(manyFqName)
    val generatedEntityTypeClassId = ClassId.topLevel(generatedEntityTypeFqName)
    val valueAttributeClassId = ClassId.topLevel(valueAttributeFqName)
    val transientAttributeClassId = ClassId.topLevel(transientAttributeFqName)
    val referenceAttributeClassId = ClassId.topLevel(referenceAttributeFqName)
    val entityConstructorClassId = ClassId.topLevel(entityConstructorFqName)
}

object RhizomedbSymbolNames {
    val requiredClassId = ClassId(RhizomedbPackages.packageFqName, FqName("Attributes.Required"), isLocal = false)
    val optionalClassId = ClassId(RhizomedbPackages.packageFqName, FqName("Attributes.Optional"), isLocal = false)
    val manyClassId = ClassId(RhizomedbPackages.packageFqName, FqName("Attributes.Many"), isLocal = false)

    val entityClassId = ClassId(RhizomedbPackages.packageFqName, Name.identifier("Entity"))
    val entityTypeClassId = ClassId(RhizomedbPackages.packageFqName, Name.identifier("EntityType"))
    val eidClassId = ClassId(RhizomedbPackages.packageFqName, Name.identifier("EID"))
}


object KotlinStdlib {
    val setFqName = FqName("kotlin.collections.Set")
    val setClassId = ClassId.topLevel(setFqName)
}
