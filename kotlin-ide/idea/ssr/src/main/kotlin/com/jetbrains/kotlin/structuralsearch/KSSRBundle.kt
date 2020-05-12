package com.jetbrains.kotlin.structuralsearch

import com.intellij.DynamicBundle
import org.jetbrains.annotations.PropertyKey

object KSSRBundle {
    private const val BUNDLE = "KSSRBundle"
    private val dynamicBundle = object : DynamicBundle(BUNDLE) {}

    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) = dynamicBundle.getMessage(key, params)

    fun messagePointer(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
        dynamicBundle.getLazyMessage(key, params)
}