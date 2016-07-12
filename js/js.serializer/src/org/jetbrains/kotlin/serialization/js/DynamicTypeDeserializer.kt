/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.serialization.js

import org.jetbrains.kotlin.serialization.deserialization.FlexibleTypeDeserializer
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.createDynamicType
import org.jetbrains.kotlin.types.typeUtil.builtIns

object DynamicTypeDeserializer : FlexibleTypeDeserializer {
    val id = "kotlin.DynamicType"

    override fun create(flexibleId: String, lowerBound: SimpleType, upperBound: SimpleType): KotlinType {
        if (flexibleId != id) return ErrorUtils.createErrorType("Unexpected id: $flexibleId. ($lowerBound..$upperBound)")
        if (KotlinTypeChecker.FLEXIBLE_UNEQUAL_TO_INFLEXIBLE.equalTypes(lowerBound, lowerBound.builtIns.nothingType) &&
            KotlinTypeChecker.FLEXIBLE_UNEQUAL_TO_INFLEXIBLE.equalTypes(upperBound, upperBound.builtIns.nullableAnyType)) {
            return createDynamicType(lowerBound.builtIns)
        }
        else {
            return ErrorUtils.createErrorType("Illegal type range for dynamic type: $lowerBound..$upperBound")
        }
    }
}