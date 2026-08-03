/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package hair.ir.generator

import hair.ir.generator.toolbox.FileSink
import hair.ir.generator.toolbox.Generators
import hair.ir.generator.toolbox.SchemaBuilder
import hair.ir.generator.toolbox.validateControlFlow
import java.io.File

fun main(args: Array<String>) {
    val schema = SchemaBuilder.build(Models.all)
    schema.validateControlFlow()
    val sink = FileSink(File(args.first()))
    Generators.all.forEach { it.generate(schema, sink) }
}
