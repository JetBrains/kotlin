package org.jetbrains.kotlin.model

import java.lang.String
//import std.*
import std.util.*

import java.util.*

class KModel(var title: String = "Documentation", var version: String = "TODO") {
    // TODO generates java.lang.NoSuchMethodError: std.util.namespace.hashMap(Ljet/TypeInfo;Ljet/TypeInfo;)Ljava/util/HashMap;
    //val packages = sortedMap<String,KPackage>()
    public val packageMap: SortedMap<String,KPackage> = TreeMap<String,KPackage>()

    public val packages: Collection<KPackage> = packageMap.values().sure()

    public val classes: Collection<KClass>
        get() = packages.flatMap{ it.classes }

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

    fun previous(pkg: KPackage): KPackage? {
        return null
    }

    fun next(pkg: KPackage): KPackage? {
        return null
    }
}

class KPackage(val name: String, var description: String = "", var detailedDescription: String = "") : Comparable<KPackage> {
    override fun compareTo(other: KPackage): Int = name.compareTo(other.name)

    fun equals(other: KPackage) = name == other.name

    fun toString() = "KPackage($name)"

    /** Returns the name as a directory using '/' instead of '.' */
    public val nameAsPath: String
        get() = name.replace('.', '/')

    /** Returns a list of all the paths in the package name */
    public val namePaths: List<String>
        get() {
            val answer = ArrayList<String>()
            for (n in name.split("\\.")) {
                if (n != null) {
                    answer.add(n)
                }
            }
            return answer;
        }

    /** Returns a relative path like ../.. for each path in the name */
    public val nameAsRelativePath: String
        get() = namePaths.map{ ".." }.join("/")

    // TODO generates java.lang.NoSuchMethodError: std.util.namespace.hashMap(Ljet/TypeInfo;Ljet/TypeInfo;)Ljava/util/HashMap;
    //val classes = sortedMap<String,KClass>()
    public val classMap: SortedMap<String,KClass> = TreeMap<String,KClass>()

    public val classes: Collection<KClass> = classMap.values().sure()

    public val annotations: Collection<KClass> = ArrayList<KClass>()


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

    fun groupClassMap(): Map<String,List<KClass>> {
        return classes.groupBy(TreeMap<String,List<KClass>>()){it.group}
    }
}

class KClass(val kpackage: KPackage, val simpleName: String,
        var kind: String = "class", var group: String = "Other", var description: String = "")  : Comparable<KClass> {
    override fun compareTo(other: KClass): Int = name.compareTo(other.name)

    fun equals(other: KClass) = name == other.name

    fun toString() = "$kind($name)"

    /** Link to the type which is relative if its a local type but could be a type in a different library or null if no link */
    public var url: String? = null
        get() {
            if ($url == null) $url = "${nameAsPath}.html"
            return $url
        }

    public val name: String = kpackage.qualifiedName(simpleName)
    public val packageName: String = kpackage.name

    /** Returns the name as a directory using '/' instead of '.' */
    public val nameAsPath: String
        get() = name.replace('.', '/')

}