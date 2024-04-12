/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.rhizomedb.fir.resolve

import org.jetbrains.kotlin.name.*

object RhizomedbPackages {
    val packageFqName = FqName("com.jetbrains.rhizomedb")
}

object RhizomedbAnnotations {
    val indexedFqName = FqName("com.jetbrains.rhizomedb.Indexed")
    val uniqueFqName = FqName("com.jetbrains.rhizomedb.Unique")
    val manyFqName = FqName("com.jetbrains.rhizomedb.Many")
    val cascadeDeleteFqName = FqName("com.jetbrains.rhizomedb.CascadeDelete")
    val cascadeDeleteByFqname = FqName("com.jetbrains.rhizomedb.CascadeDeleteBy")
    val generatedEntityTypeFqName = FqName("com.jetbrains.rhizomedb.GeneratedEntityType")
    val attributeAnnotationFqName = FqName("com.jetbrains.rhizomedb.Attribute")

    val indexedAnnotationClassId = ClassId.topLevel(indexedFqName)
    val uniqueAnnotationClassId = ClassId.topLevel(uniqueFqName)
    val manyAnnotationClassId = ClassId.topLevel(manyFqName)
    val cascadeDeleteAnnotationClassId = ClassId.topLevel(cascadeDeleteFqName)
    val cascadeDeleteByAnnotationClassId = ClassId.topLevel(cascadeDeleteByFqname)
    val generatedEntityTypeAnnotationClassId = ClassId.topLevel(generatedEntityTypeFqName)
    val attributeAnnotationClassId = ClassId.topLevel(attributeAnnotationFqName)
}

object RhizomedbSymbolNames {
    val requiredClassId = ClassId(RhizomedbPackages.packageFqName, FqName("Attributes.Required"), isLocal = false)
    val optionalClassId = ClassId(RhizomedbPackages.packageFqName, FqName("Attributes.Optional"), isLocal = false)
    val manyClassId = ClassId(RhizomedbPackages.packageFqName, FqName("Attributes.Many"), isLocal = false)

    val entityTypeClassId = ClassId(RhizomedbPackages.packageFqName, Name.identifier("EntityType"))
}


object KotlinStdlib {
    val setFqName = FqName("kotlin.collections.Set")
    val setClassId = ClassId.topLevel(setFqName)
}
