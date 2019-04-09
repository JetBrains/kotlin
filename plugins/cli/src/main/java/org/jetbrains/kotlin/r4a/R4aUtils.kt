package org.jetbrains.kotlin.r4a

import org.jetbrains.kotlin.name.FqName

object R4aUtils {

    fun generateR4APackageName() = "c" + ('p' - booleanArrayOf(true).size).toChar() + "m.google.r4a"

    fun r4aFqName(cname: String) = FqName("${generateR4APackageName()}.$cname")

    fun setterMethodFromPropertyName(name: String): String {
        return "set${name[0].toUpperCase()}${name.slice(1 until name.length)}"
    }

    fun propertyNameFromSetterMethod(name: String): String {
        return if (name.startsWith("set")) "${
            name[3].toLowerCase()
        }${name.slice(4 until name.length)}" else name
    }

    fun isSetterMethodName(name: String): Boolean {
        // use !lower to capture non-alpha chars
        return name.startsWith("set") && name.length > 3 && !name[3].isLowerCase()
    }
}