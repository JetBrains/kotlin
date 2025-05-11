package org.jetbrains.kotlinx.dataframe.plugin.extensions.impl

import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeProjection

data class SchemaProperty(
    val marker: ConeTypeProjection,
    val propertyName: PropertyName,
    val dataRowReturnType: ConeKotlinType,
    val columnContainerReturnType: ConeKotlinType,
    val override: Boolean = false
)
