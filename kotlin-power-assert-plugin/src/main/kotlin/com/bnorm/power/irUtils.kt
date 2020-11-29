package com.bnorm.power

import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irString

fun IrBuilderWithScope.irString(builderAction: StringBuilder.() -> Unit) =
  irString(buildString { builderAction() })
