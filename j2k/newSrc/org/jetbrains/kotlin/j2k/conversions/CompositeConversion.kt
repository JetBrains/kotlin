/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.tree.JKTreeElement

class BatchPipelineConversion(val conversions: List<BatchBaseConversion>) : BatchBaseConversion {
    override fun runConversion(treeRoots: List<JKTreeElement>, context: ConversionContext): Boolean {
        return conversions.asSequence().map { it.runConversion(treeRoots, context) }.max() ?: false
    }
}

class SequentialPipelineConversion(val conversions: List<SequentialBaseConversion>) : SequentialBaseConversion {
    override fun runConversion(treeRoot: JKTreeElement, context: ConversionContext): Boolean {
        return conversions.asSequence().map { it.runConversion(treeRoot, context) }.max() ?: false
    }
}

class BatchRepeatConversion(val conversion: BatchBaseConversion) : BatchBaseConversion {

    override fun runConversion(treeRoots: List<JKTreeElement>, context: ConversionContext): Boolean {
        return true in generateSequence { conversion.runConversion(treeRoots, context) }.takeWhile { it }
    }

}

class SequentialRepeatConversion(val conversion: SequentialBaseConversion) : SequentialBaseConversion {
    override fun runConversion(treeRoot: JKTreeElement, context: ConversionContext): Boolean {
        return true in generateSequence { conversion.runConversion(treeRoot, context) }.takeWhile { it }
    }

}


class PipelineConversionBuilder<T : BatchBaseConversion> {
    val conversions = mutableListOf<T>()
    operator fun T.unaryPlus() {
        conversions += this
    }
}

internal inline fun sequentialPipe(crossinline configure: PipelineConversionBuilder<SequentialBaseConversion>.() -> Unit): SequentialPipelineConversion {
    return SequentialPipelineConversion(
        PipelineConversionBuilder<SequentialBaseConversion>().apply(configure).conversions
    )
}

internal inline fun batchPipe(crossinline configure: PipelineConversionBuilder<BatchBaseConversion>.() -> Unit): BatchPipelineConversion {
    return BatchPipelineConversion(
        PipelineConversionBuilder().apply(configure).conversions
    )
}
internal inline fun batchRepeat(batchBaseConversion: BatchBaseConversion): BatchRepeatConversion {
    return BatchRepeatConversion(batchBaseConversion)
}
internal inline fun sequentialRepeat(sequentialBaseConversion: SequentialBaseConversion): SequentialRepeatConversion {
    return SequentialRepeatConversion(sequentialBaseConversion)
}
