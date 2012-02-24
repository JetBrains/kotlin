package org.jetbrains.kotlin.model

import java.lang.String
import std.*
import std.util.*

import java.util.*
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor
import org.jetbrains.jet.lang.resolve.java.JavaNamespaceDescriptor
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.resolve.BindingContextUtils
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor

fun containerName(descriptor: DeclarationDescriptor): String = qualifiedName(descriptor.containingDeclaration)

fun qualifiedName(descriptor: DeclarationDescriptor?): String {
    if (descriptor == null || descriptor is ModuleDescriptor || descriptor is JavaNamespaceDescriptor) {
        return ""
    } else {
        val parent = containerName(descriptor)
        val name = descriptor.getName() ?: ""
        val answer = if (parent.length() > 0) parent + "." + name else name
        return if (answer.startsWith(".")) answer.substring(1) else answer
    }
}

// TODO for some reason the SortedMap causes kotlin to freak out a little :)
fun inheritedExtensionFunctions(functions: Collection<KFunction>): Map<KClass, SortedSet<KFunction>> {
    //fun inheritedExtensionFunctions(functions: Collection<KFunction>): SortedMap<KClass, SortedSet<KFunction>> {
    val map = extensionFunctions(functions)
    // for each class, lets walk its base classes and add any other extension functions from base classes
    val classes = map.keySet().toList()
    val answer = TreeMap<KClass, SortedSet<KFunction>>()
    for (c in map.keySet()) {
        val allFunctions = map.get(c).notNull().toSortedSet()
        answer.put(c, allFunctions)
        val des = c.descendants()
        for (b in des) {
            val list = map.get(b)
            if (list != null) {
                if (allFunctions != null) {
                    for (f in list) {
                        if (f != null) {
                            // add the methods from the base class if we don't have a matching method
                            if (!allFunctions.any{ it.name == f.name && it.parameterTypeText == f.parameterTypeText}) {
                                allFunctions.add(f)
                            }
                        }
                    }
                }
            }
        }
    }
    return answer
}

// TODO for some reason the SortedMap causes kotlin to freak out a little :)
fun extensionFunctions(functions: Collection<KFunction>): Map<KClass, List<KFunction>> {
    //fun extensionFunctions(functions: Collection<KFunction>): SortedMap<KClass, List<KFunction>> {
    val map = TreeMap<KClass, List<KFunction>>()
    functions.filter{ it.extensionClass != null }.groupBy(map){ it.extensionClass.sure() }
    return map
}

trait KClassOrPackage {
}

class KModel(var context: BindingContext, var title: String = "Documentation", var version: String = "TODO") {
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

    /** Loads the model from the given set of source files */
    fun load(sources: List<JetFile?>): Unit {
        val allNamespaces = HashSet<NamespaceDescriptor>()
        for (source in sources) {
            if (source != null) {
                // We retrieve a descriptor by a PSI element from the context
                val namespaceDescriptor = BindingContextUtils.namespaceDescriptor(context, source)
                if (namespaceDescriptor != null) {
                    allNamespaces.add(namespaceDescriptor);
                }
            }
        }
        val allClasses = HashSet<KClass>()
        for (namespace in allNamespaces) {
            getPackage(namespace)
            for (descriptor in namespace.getMemberScope().getAllDescriptors()) {
                if (descriptor is ClassDescriptor) {
                    val klass = getClass(descriptor)
                    if (klass != null) {
                        allClasses.add(klass)
                    }
                } else if (descriptor is NamespaceDescriptor) {
                    getPackage(descriptor)
                }
            }
        }
    }

    /* Returns the package for the given name or null if it does not exist */
    fun getPackage(name: String): KPackage? = packageMap.get(name)

    /** Returns the package for the given descriptor, creating one if its not available */
    fun getPackage(descriptor: NamespaceDescriptor): KPackage {
        val name = qualifiedName(descriptor)
        var created = false
        val pkg = packageMap.getOrPut(name) {
            created = true
            KPackage(this, descriptor, name)
        }
        if (created) {
            pkg.description = commentsFor(descriptor)
            addFunctions(pkg, pkg.functions, descriptor.getMemberScope())
            if (pkg.functions.notEmpty()) {
                pkg.local = true
            }
        }
        return pkg;
    }

    protected fun addFunctions(owner: KClassOrPackage, list: Collection<KFunction>, scope: JetScope): Unit {
        try {
            val descriptors = scope.getAllDescriptors()
            for (descriptor in descriptors) {
                if (descriptor is PropertyDescriptor) {
                    if (owner is KClass) {
                        val name = descriptor.getName()
                        val returnType = getClass(descriptor.getReturnType())
                        if (returnType != null) {
                            val property = KProperty(owner, descriptor, name, returnType)
                            owner.properties.add(property)
                        }
                    }
                } else if (descriptor is CallableDescriptor) {
                    val function = createFunction(owner, descriptor)
                    if (function != null) {
                        list.add(function)
                    }
                }
            }
        } catch (e: Throwable) {
            println("Caught exception finding function declarations on $owner")
        }

    }

    protected fun createFunction(owner: KClassOrPackage, descriptor: CallableDescriptor): KFunction? {
        val returnType = getClass(descriptor.getReturnType())
        if (returnType != null) {
            val function = KFunction(owner, descriptor.getName() ?: "null", returnType)
            function.description = commentsFor(descriptor)
            val params = descriptor.getValueParameters()
            for (param in params) {
                if (param != null) {
                    val p = createParameter(param)
                    if (p != null) {
                        function.parameters.add(p)
                    }
                }
            }
            val receiver = descriptor.getReceiverParameter()
            if (receiver is ExtensionReceiver) {
                function.extensionClass = getClass(receiver.getType())
            }
            return function
        }
        return null
    }

    protected fun createParameter(descriptor: ValueParameterDescriptor): KParameter? {
        val returnType = getClass(descriptor.getReturnType())
        if (returnType != null) {
            val name = descriptor.getName()
            val answer = KParameter(name, returnType)
            answer.description = commentsFor(descriptor)
            return answer
        }
        return null
    }


    fun commentsFor(descriptor: DeclarationDescriptor): String {
        val psiElement = try {
            BindingContextUtils.descriptorToDeclaration(context, descriptor)
        } catch (e: Throwable) {
            // ignore exceptions on fake descriptors
            null
        }

        // This method is a hack. Doc comments should be easily accessible, but they aren't for now.
        if (psiElement != null) {
            var node = psiElement.getNode()?.getTreePrev()
            while (node != null && (node?.getElementType() == JetTokens.WHITE_SPACE || node?.getElementType() == JetTokens.BLOCK_COMMENT)) {
                node = node?.getTreePrev()
            }
            if (node == null) return ""
            if (node?.getElementType() != JetTokens.DOC_COMMENT) return ""
            var text = node?.getText() ?: ""
            // lets remove the comment tokens
            //val lines = text.trim().split("\\n")
            text = text.trim().trim("/").trim("*").trim()
            // TODO convert any macros or wiki text!
            return text
        }
        return ""
    }


    fun getClass(aType: JetType?): KClass? {
        if (aType != null) {
            val classifierDescriptor = aType.constructor.declarationDescriptor
            if (classifierDescriptor is ClassDescriptor) {
                return getClass(classifierDescriptor)
            }
        }
        return null
    }

    fun getClass(classElement: ClassDescriptor): KClass? {
        val name = classElement.getName()
        val container = classElement.containingDeclaration
        if (name != null && container is NamespaceDescriptor) {
            val pkg = getPackage(container)
            return pkg.getClass(name, classElement)
        } else {
            println("No package found for $container and class $name")
            return null
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

abstract class KAnnotated {
    public open var description: String = ""

    public open var detailedDescription: String = ""
    get() = if ($detailedDescription.notEmpty()) $detailedDescription else description

    public open var deprecated: Boolean = false
}

class KPackage(val model: KModel, val descriptor: NamespaceDescriptor,
        val name: String, var external: Boolean = false,
        var functions: SortedSet<KFunction> = TreeSet<KFunction>(),
        var local: Boolean = false) : KAnnotated(), Comparable<KPackage>, KClassOrPackage {

    override fun compareTo(other: KPackage): Int = name.compareTo(other.name)

    fun equals(other: KPackage) = name == other.name

    fun toString() = "KPackage($name)"

    fun getClass(name: String, classElement: ClassDescriptor): KClass {
        var created = false
        val klass = classMap.getOrPut(name) {
            created = true
            KClass(this, classElement, name)
        }
        if (created) {
            local = true
            klass.description = model.commentsFor(classElement)
            val superTypes = classElement.getTypeConstructor().getSupertypes()
            for (st in superTypes) {
                val sc = model.getClass(st)
                if (sc != null) {
                    klass.baseClasses.add(sc)
                }
            }
            model.addFunctions(klass, klass.functions, classElement.getDefaultType().getMemberScope())
        }
        return klass
    }

    /** Returns the name as a directory using '/' instead of '.' */
    public val nameAsPath: String
    get() = if (name.length() == 0) "." else name.replace('.', '/')

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

    fun packageFunctions() = functions.filter{ it.extensionClass == null }
}

class KClass(val pkg: KPackage, val descriptor: ClassDescriptor,
        val simpleName: String,
        var kind: String = "class", var group: String = "Other",
        var annotations: List<KAnnotation> = arrayList<KAnnotation>(),
        var since: String = "",
        var authors: List<String> = arrayList<String>(),
        var functions: SortedSet<KFunction> = TreeSet<KFunction>(),
        var properties: SortedSet<KProperty> = TreeSet<KProperty>(),
        var baseClasses: List<KClass> = arrayList<KClass>(),
        var nestedClasses: List<KClass> = arrayList<KClass>(),
        var sourceLine: Int = 2) : KAnnotated(), Comparable<KClass>, KClassOrPackage {

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

    /** Returns all of the base classes and all of their descendants */
    fun descendants(answer: Set<KClass> = LinkedHashSet<KClass>()): Set<KClass> {
        answer.addAll(baseClasses)
        for (b in baseClasses) {
            b.descendants(answer)
        }
        return answer
    }
}

class KFunction(val owner: KClassOrPackage, val name: String,
        var returnType: KClass,
        var extensionClass: KClass? = null,
        var modifiers: List<String> = arrayList<String>(),
        var typeParameters: List<KTypeParameter> = arrayList<KTypeParameter>(),
        var parameters: List<KParameter> = arrayList<KParameter>(),
        var exceptions: List<KClass> = arrayList<KClass>(),
        var annotations: List<KAnnotation> = arrayList<KAnnotation>(),
        var sourceLine: Int = 2) : KAnnotated(), Comparable<KFunction> {

    override fun compareTo(other: KFunction): Int {
        var answer = name.compareTo(other.name)
        if (answer == 0) {
            answer = parameterTypeText.compareTo(other.parameterTypeText)
        }
        return answer
    }

    fun equals(other: KFunction) = name == other.name

    fun toString() = "fun ($name)"

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

class KProperty(val owner: KClassOrPackage, val descriptor: PropertyDescriptor, val name: String,
        val returnType: KClass) : KAnnotated(), Comparable<KProperty> {

    override fun compareTo(other: KProperty): Int = name.compareTo(other.name)

    public val link: String = "$name"

    fun equals(other: KFunction) = name == other.name

    fun toString() = "property $name}"
}

class KParameter(val name: String,
        var klass: KClass) : KAnnotated()  {

    fun toString() = "$name: ${klass.name}"
}

class KTypeParameter(val name: String,
        var klass: KClass,
        var extends: List<KClass> = arrayList<KClass>())  {

    fun toString() = if (extends.isEmpty()) name else {
        "$name extends ${extends.map{it.name}.join(" & ")}"
    }
}

class KAnnotation(var klass: KClass) : KAnnotated()  {

    // TODO add some parameter values?

    fun toString() = "@$klass.simpleName"
}