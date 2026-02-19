package org.jetbrains.kotlin.native.interop.gen.jvm

enum class GenerationMode(val modeName: String) {
    SOURCE_CODE("sourcecode"),
    METADATA("metadata");

    override fun toString(): String = modeName
}