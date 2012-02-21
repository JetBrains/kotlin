package org.jetbrains.kotlin.model

import java.lang.String
//import std.*
import std.util.*

import java.util.*

class KModel(var title: String = "Documentation") {
    // TODO generates java.lang.NoSuchMethodError: std.util.namespace.hashMap(Ljet/TypeInfo;Ljet/TypeInfo;)Ljava/util/HashMap;
    //val packages = sortedMap<String,KPackage>()
    public val packageMap: SortedMap<String,KPackage> = TreeMap<String,KPackage>()

    public val packages: Collection<KPackage> = packageMap.values().sure()

    /* Returns the package for the given name, creating one if its not already been create yet */
    fun getPackage(name: String): KPackage {
        return packageMap.getOrPut(name){ KPackage(name) }
    }

    /* Returns the class for the given qualified name, creating one if its not already been created yet */
    fun getClass(qualifiedName: String): KClass {
        val idx = qualifiedName.lastIndexOf('.')
        return if (idx > 0) {
            getPackage(qualifiedName.substring(0, idx)).getClass(qualifiedName.substring(idx + 1))
        } else {
            getPackage("").getClass(qualifiedName)
        }
    }
}

class KPackage(val name: String) : Comparable<KPackage> {
    override fun compareTo(other: KPackage): Int = name.compareTo(other.name)

    fun equals(other: KPackage) = name == other.name

    fun toString() = "KPackage($name)"

    // TODO generates java.lang.NoSuchMethodError: std.util.namespace.hashMap(Ljet/TypeInfo;Ljet/TypeInfo;)Ljava/util/HashMap;
    //val classes = sortedMap<String,KClass>()
    public val classMap: SortedMap<String,KClass> = TreeMap<String,KClass>()

    public val classes: Collection<KClass> = classMap.values().sure()

    /* Returns the class for the given name, creating one if its not already been create yet */
    fun getClass(simpleName: String): KClass {
        return classMap.getOrPut(simpleName){ KClass(this, simpleName) }
    }

    fun qualifiedName(simpleName: String): String {
        return if (name.length() > 0) {
            "${name}.${simpleName}"
        } else {
            simpleName
        }
    }

}

class KClass(val kpackage: KPackage, val simpleName: String)  : Comparable<KClass> {
    override fun compareTo(other: KClass): Int = name.compareTo(other.name)

    fun equals(other: KClass) = name == other.name

    fun toString() = "KClass($name)"

    public val name: String = kpackage.qualifiedName(simpleName)
    public val packageName: String = kpackage.name

}