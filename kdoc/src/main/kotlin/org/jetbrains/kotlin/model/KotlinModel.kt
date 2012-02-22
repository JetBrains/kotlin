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
        return packageMap.getOrPut(name){ KPackage(this, name) }
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
        // TODO
        return null
    }

    fun next(pkg: KPackage): KPackage? {
        // TODO
        return null
    }
}

class KPackage(val model: KModel, val name: String, var external: Boolean = false,
        var description: String = "", var detailedDescription: String = "") : Comparable<KPackage> {
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
        get() {
            val answer = namePaths.map{ ".." }.join("/")
            return if (answer.length == 0) "" else answer + "/"
        }

    // TODO generates java.lang.NoSuchMethodError: std.util.namespace.hashMap(Ljet/TypeInfo;Ljet/TypeInfo;)Ljava/util/HashMap;
    //val classes = sortedMap<String,KClass>()
    public val classMap: SortedMap<String,KClass> = TreeMap<String,KClass>()

    public val classes: Collection<KClass> = classMap.values().sure()

    public val annotations: Collection<KClass> = ArrayList<KClass>()


    /* Returns the class for the given name, creating one if its not already been create yet */
    fun getClass(simpleName: String): KClass {
        return classMap.getOrPut(simpleName) {
            val base = if (simpleName == "Object" && name == "java.lang") null else model.getClass("java.lang.Object")
            KClass(this, simpleName, base) }
    }

    fun qualifiedName(simpleName: String): String {
        return if (name.length() > 0) {
            "${name}.${simpleName}"
        } else {
            simpleName
        }
    }


    fun previous(pkg: KClass): KClass? {
        // TODO
        return null
    }

    fun next(pkg: KClass): KClass? {
        // TODO
        return null
    }

    fun groupClassMap(): Map<String,List<KClass>> {
        return classes.groupBy(TreeMap<String,List<KClass>>()){it.group}
    }
}

class KClass(val pkg: KPackage, val simpleName: String,
        var baseClass: KClass? = null,
        var kind: String = "class", var group: String = "Other",
        var description: String = "", var detailedDescription: String = "",
        var annotations: List<KAnnotation> = arrayList<KAnnotation>(),
        var since: String = "",
        var authors: List<String> = arrayList<String>(),
        var methods: List<KMethod> = arrayList<KMethod>(),
        var nestedClasses: List<KClass> = arrayList<KClass>(),
        var sourceLine: Int = 2) : Comparable<KClass> {

    override fun compareTo(other: KClass): Int = name.compareTo(other.name)

    fun equals(other: KClass) = name == other.name

    fun toString() = "$kind($name)"

    /** Link to the type which is relative if its a local type but could be a type in a different library or null if no link */
    public var url: String? = null
        get() {
            if ($url == null) $url = "${nameAsPath}.html"
            return $url
        }

    public val name: String = pkg.qualifiedName(simpleName)
    public val packageName: String = pkg.name

    /** Returns the name as a directory using '/' instead of '.' */
    public val nameAsPath: String
        get() = name.replace('.', '/')


    fun isAnnotation() = kind == "annotation"
    fun isInterface() = kind == "interface"
}

class KMethod(val name: String,
        var returnType: KClass,
        var description: String = "", var detailedDescription: String = "",
        var deprecated: Boolean = false,
        var modifiers: List<String> = arrayList<String>(),
        var typeParameters: List<KTypeParameter> = arrayList<KTypeParameter>(),
        var parameters: List<KParameter> = arrayList<KParameter>(),
        var exceptions: List<KClass> = arrayList<KClass>(),
        var annotations: List<KAnnotation> = arrayList<KAnnotation>()) : Comparable<KMethod> {
    // TODO compare other things than just name :)

    override fun compareTo(other: KMethod): Int = name.compareTo(other.name)

    fun equals(other: KMethod) = name == other.name

    fun toString() = "fun ($name)"

    /** TODO generate a link with the argument type names */
    public val link: String = name

    /** Returns a list of generic type parameter names kinds like "A, I" */
    public val typeParametersText: String
        get() = typeParameters.map{ it.name }.join(", ")
}

class KParameter(val name: String,
        var klass: KClass,
        var description: String = "", var detailedDescription: String = "")  {

    fun toString() = "$name: ${klass.name}"
}

class KTypeParameter(val name: String,
        var klass: KClass,
        var extends: List<KClass> = arrayList<KClass>())  {

    fun toString() = if (extends.isEmpty()) name else {
        "$name extends ${extends.map{it.name}.join(" & ")}"
    }
}

class KAnnotation(var klass: KClass,
        var description: String = "", var detailedDescription: String = "")  {

    // TODO add some parameter values?

    fun toString() = "@$klass.simpleName"
}