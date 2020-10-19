package org.jetbrains.kotlin.idea.structuralsearch

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import java.util.function.Supplier

object KSSRBundle {
    @NonNls private const val BUNDLE = "structuralsearch/KSSRBundle"

    private val dynamicBundle = object : DynamicBundle(BUNDLE) {}

    @Nls
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String =
        dynamicBundle.getMessage(key, params)

    @Nls
    fun messagePointer(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): Supplier<String> =
        dynamicBundle.getLazyMessage(key, params)
}