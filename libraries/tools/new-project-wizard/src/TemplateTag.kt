/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


class TemplateTag private constructor(
    val text: String,
    val tooltip: String?,
    val priority: Int
) {
    companion object {
        private val allTags = mutableListOf<TemplateTag>()
        private fun tag(text: String, tooltip: String? = null): TemplateTag =
            TemplateTag(text, tooltip, priority = allTags.size).also {
                allTags += it
            }

        val ALL: List<TemplateTag>
            get() = allTags

        val MPP = tag("Multiplatform")
        val JVM = tag("JVM")
        val JS = tag("JS")
        val NATIVE = tag("Native")
        val ANDROID = tag("Android")
    }
}
