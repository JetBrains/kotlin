package org.jetbrains.kotlin.model

import java.lang.String
//import std.*
import std.util.*

import java.util.*

fun extensionFunctions(functions: Collection<KFunction>): SortedMap<KClass, List<KFunction>> {
    val map = TreeMap<KClass, List<KFunction>>()
    functions.filter{ it.extensionClass != null }.groupBy(map){ it.extensionClass.sure() }
    return map
}

trait KClassOrPackage {

}

class KModel(var title: String = "Documentation", var version: String = "TODO") {
    // TODO generates java.lang.NoSuchMethodError: std.util.namespace.hashMap(Ljet/TypeInfo;Ljet/TypeInfo;)Ljava/util/HashMap;
    //val packages = sortedMap<String,KPackage>()
    public val packageMap: SortedMap<String, KPackage> = TreeMap<String, KPackage>()

    public val allPackages: Collection<KPackage>
    get() = packageMap.values().sure()

    /** Returns the local packages */
    public val packages: Collection<KPackage>
    get() = allPackages.filter{ it.local }

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
        var description: String = "", var detailedDescription: String = "",
        var functions: SortedSet<KFunction> = TreeSet<KFunction>(),
        var local: Boolean = false) : Comparable<KPackage>, KClassOrPackage {
    override fun compareTo(other: KPackage): Int = name.compareTo(other.name)


    fun equals(other: KPackage) = name == other.name

    fun toString() = "KPackage($name)"

    private var _initialised = false

    /** Runs the initialisation block if this class has not yet been initialised */
    fun initialise(fn: () -> Unit): Unit {
        if (!_initialised) {
            _initialised = true
            fn()
        }
    }

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
    public val classMap: SortedMap<String, KClass> = TreeMap<String, KClass>()

    public val classes: Collection<KClass> = classMap.values().sure()

    public val annotations: Collection<KClass> = ArrayList<KClass>()


    /* Returns the class for the given name, creating one if its not already been create yet */
    fun getClass(simpleName: String): KClass {
        return classMap.getOrPut(simpleName) {
            KClass(this, simpleName) }
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

    fun groupClassMap(): Map<String, List<KClass>> {
        return classes.groupBy(TreeMap<String, List<KClass>>()){it.group}
    }
}

class KClass(val pkg: KPackage, val simpleName: String,
        var kind: String = "class", var group: String = "Other",
        var description: String = "", var detailedDescription: String = "",
        var annotations: List<KAnnotation> = arrayList<KAnnotation>(),
        var since: String = "",
        var authors: List<String> = arrayList<String>(),
        var functions: SortedSet<KFunction> = TreeSet<KFunction>(),
        var baseClasses: List<KClass> = arrayList<KClass>(),
        var nestedClasses: List<KClass> = arrayList<KClass>(),
        var sourceLine: Int = 2) : Comparable<KClass>, KClassOrPackage {

    private var _initialised = false

    /** Runs the initialisation block if this class has not yet been initialised */
    fun initialise(fn: () -> Unit): Unit {
        if (!_initialised) {
            _initialised = true
            fn()
        }
    }

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

class KFunction(val owner: KClassOrPackage, val name: String,
        var returnType: KClass,
        var description: String = "", var detailedDescription: String = "",
        var deprecated: Boolean = false,
        var extensionClass: KClass? = null,
        var modifiers: List<String> = arrayList<String>(),
        var typeParameters: List<KTypeParameter> = arrayList<KTypeParameter>(),
        var parameters: List<KParameter> = arrayList<KParameter>(),
        var exceptions: List<KClass> = arrayList<KClass>(),
        var annotations: List<KAnnotation> = arrayList<KAnnotation>(),
        var sourceLine: Int = 2) : Comparable<KFunction> {

    override fun compareTo(other: KFunction): Int {
        var answer = name.compareTo(other.name)
        if (answer == 0) {
            answer = parameterTypeText.compareTo(other.parameterTypeText)
        }
        return answer
    }

    fun equals(other: KFunction) = name == other.name

    fun toString() = "fun ($name)"

    /** TODO generate a link with the argument type names */
    public val link: String = "$name($parameterTypeText)"

    /** Returns a list of generic type parameter names kinds like "A, I" */
    public val typeParametersText: String
    get() = typeParameters.map{ it.name }.join(", ")

    /** Returns a list of parameter value types */
    private var _parameterTypeText: String? = null
    public val parameterTypeText: String
    get() {
        if (_parameterTypeText == null) {
            _parameterTypeText = typeParameters.map{ it.klass.name }.join(", ")
        }
        return _parameterTypeText.sure()
    }
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