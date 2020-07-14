package org.jetbrains.kotlin.idea.formatter

import com.intellij.application.options.codeStyle.properties.CodeStyleFieldAccessor
import com.intellij.application.options.codeStyle.properties.ExternalStringAccessor
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings
import org.jetbrains.kotlin.idea.util.applyKotlinCodeStyle
import java.lang.reflect.Field

class KotlinCodeStyleFieldAccessor(private val kotlinCodeStyle: KotlinCodeStyleSettings, field: Field) : ExternalStringAccessor<String>(kotlinCodeStyle, field) {
    override fun fromExternal(extVal: String): String = extVal
    override fun toExternal(value: String): String = value

    override fun set(extVal: String): Boolean = applyKotlinCodeStyle(extVal, kotlinCodeStyle.container)
    override fun get(): String? = kotlinCodeStyle.container.kotlinCodeStyleDefaults()

    companion object {
        fun create(codeStyleObject: Any, field: Field): CodeStyleFieldAccessor<*, *>? {
            if (codeStyleObject is KotlinCodeStyleSettings && field.name == CODE_STYLE_DEFAULTS) {
                return KotlinCodeStyleFieldAccessor(codeStyleObject, field)
            }

            return null
        }

        const val CODE_STYLE_DEFAULTS = "CODE_STYLE_DEFAULTS"
    }
}