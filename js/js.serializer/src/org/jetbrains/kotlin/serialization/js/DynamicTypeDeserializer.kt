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

import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.FlexibleTypeDeserializer
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.error.ErrorUtils;
import org.jetbrains.kotlin.types.checker.StrictEqualityTypeChecker
import org.jetbrains.kotlin.types.error.ErrorTypeKind
import org.jetbrains.kotlin.types.typeUtil.builtIns

object DynamicTypeDeserializer : FlexibleTypeDeserializer {
    const val id = "kotlin.DynamicType"

    override fun create(proto: ProtoBuf.Type, flexibleId: String, lowerBound: SimpleType, upperBound: SimpleType): KotlinType {
        if (flexibleId != id) return ErrorUtils.createErrorType(ErrorTypeKind.UNEXPECTED_FLEXIBLE_TYPE_ID, flexibleId, lowerBound.toString(), upperBound.toString())

        return if (StrictEqualityTypeChecker.strictEqualTypes(lowerBound, lowerBound.builtIns.nothingType) &&
            StrictEqualityTypeChecker.strictEqualTypes(upperBound, upperBound.builtIns.nullableAnyType)
        ) {
            createDynamicType(lowerBound.builtIns)
        } else {
            ErrorUtils.createErrorType(ErrorTypeKind.ILLEGAL_TYPE_RANGE_FOR_DYNAMIC, lowerBound.toString(), upperBound.toString())
        }
    }
}
