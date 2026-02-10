// LANGUAGE: +AnnotationsInMetadata

package test

import org.jetbrains.kotlin.plugin.sandbox.EmitMetadata

@EmitMetadata(1)
class Some @EmitMetadata(3) constructor() {
    @EmitMetadata(2)
    fun foo() = ""

    @EmitMetadata(4)
    val bar = ""
}
