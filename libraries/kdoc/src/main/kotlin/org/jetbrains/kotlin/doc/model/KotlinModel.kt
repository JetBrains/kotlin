package org.jetbrains.kotlin.doc.model

import kotlin.*
import kotlin.util.*

import org.jetbrains.kotlin.doc.KDocConfig
import org.jetbrains.kotlin.doc.highlighter.SyntaxHighligher

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
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor
import org.pegdown.PegDownProcessor
import org.pegdown.LinkRenderer
import org.pegdown.ast.WikiLinkNode
import org.pegdown.LinkRenderer.Rendering
import org.pegdown.ast.RefLinkNode
import org.pegdown.ast.AutoLinkNode
import org.pegdown.ast.ExpLinkNode
import org.pegdown.Extensions
import org.jetbrains.kotlin.doc.templates.KDocTemplate
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl
import org.jetbrains.jet.lang.descriptors.Visibility
import org.jetbrains.jet.lang.descriptors.ClassKind
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiDirectory
import org.jetbrains.jet.lang.descriptors.Visibilities


/**
 * Returns the collection of functions with duplicate function names filtered out
 * so only the first one is included
 */
fun filterDuplicateNames(functions: Collection<KFunction>): Collection<KFunction> {
    var lastName = ""
    return functions.filter{
        val name = it.name
        val answer = name != lastName
        lastName = name
        answer
    }
}

fun containerName(descriptor: DeclarationDescriptor): String = qualifiedName(descriptor.getContainingDeclaration())

fun qualifiedName(descriptor: DeclarationDescriptor?): String {
    if (descriptor == null || descriptor is ModuleDescriptor) {
        return ""
    } else {
        val parent = containerName(descriptor)
        var name = descriptor.getName() ?: ""
        if (name.startsWith("<")) {
            name = ""
        }
        val answer = if (parent.length() > 0) parent + "." + name else name
        return if (answer.startsWith(".")) answer.substring(1) else answer
    }
}

fun warning(message: String) {
    println("Warning: $message")
}

// TODO for some reason the SortedMap causes kotlin to freak out a little :)
fun inheritedExtensionFunctions(functions: Collection<KFunction>): Map<KClass, SortedSet<KFunction>> {
    //fun inheritedExtensionFunctions(functions: Collection<KFunction>): SortedMap<KClass, SortedSet<KFunction>> {
    val map = extensionFunctions(functions)
    // for each class, lets walk its base classes and add any other extension functions from base classes
    val answer = TreeMap<KClass, SortedSet<KFunction>>()
    for (c in map.keySet()) {
        val allFunctions = map.get(c).orEmpty().toSortedSet()
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
fun inheritedExtensionProperties(properties: Collection<KProperty>): Map<KClass, SortedSet<KProperty>> {
    val map = extensionProperties(properties)
    // for each class, lets walk its base classes and add any other extension properties from base classes
    val answer = TreeMap<KClass, SortedSet<KProperty>>()
    for (c in map.keySet()) {
        val allProperties = map.get(c).orEmpty().toSortedSet()
        answer.put(c, allProperties)
        val des = c.descendants()
        for (b in des) {
            val list = map.get(b)
            if (list != null) {
                if (allProperties != null) {
                    for (f in list) {
                        if (f != null) {
                            // add the proeprties from the base class if we don't have a matching method
                            if (!allProperties.any{ it.name == f.name}) {
                                allProperties.add(f)
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
    val map = TreeMap<KClass, List<KFunction>>()
    functions.filter{ it.extensionClass != null }.groupBy(map){ it.extensionClass.sure() }
    return map
}

// TODO for some reason the SortedMap causes kotlin to freak out a little :)
fun extensionProperties(properties: Collection<KProperty>): Map<KClass, List<KProperty>> {
    val map = TreeMap<KClass, List<KProperty>>()
    properties.filter{ it.extensionClass != null }.groupBy(map){ it.extensionClass.sure() }
    return map
}

abstract class KClassOrPackage(model: KModel, declarationDescriptor: DeclarationDescriptor): KAnnotated(model, declarationDescriptor) {

    public open val functions: SortedSet<KFunction> = TreeSet<KFunction>()

    public open val properties: SortedSet<KProperty> = TreeSet<KProperty>()

    fun findProperty(name: String): KProperty? {
        // TODO we should use a Map<String>?
        return properties.find{ it.name == name }
    }

    fun findFunction(expression: String): KFunction? {
        val idx = expression.indexOf('(')
        val name = if (idx > 0) expression.substring(0, idx) else expression
        val postfix = if (idx > 0) expression.substring(idx).trimTrailing("()") else ""
        return functions.find{ it.name == name && it.parameterTypeText == postfix }
    }
}

class KModel(var context: BindingContext, val config: KDocConfig) {
    // TODO generates java.lang.NoSuchMethodError: kotlin.util.namespace.hashMap(Ljet/TypeInfo;Ljet/TypeInfo;)Ljava/util/HashMap;
    //val packages = sortedMap<String,KPackage>()
    public val packageMap: SortedMap<String, KPackage> = TreeMap<String, KPackage>()

    public val allPackages: Collection<KPackage>
    get() = packageMap.values().sure()

    /** Returns the local packages */
    public val packages: Collection<KPackage>
    get() = allPackages.filter{ it.local && config.includePackage(it) }

    public val classes: Collection<KClass>
    get() = packages.flatMap{ it.classes }

    public var markdownProcessor: PegDownProcessor = PegDownProcessor(Extensions.ALL)
    public var highlighter: SyntaxHighligher = SyntaxHighligher()

    public val title: String
    get() = config.title

    public val version: String
    get() = config.version

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
            configureComments(pkg, descriptor)
            val scope = descriptor.getMemberScope()
            addFunctions(pkg, scope)
            pkg.local = isLocal(descriptor)
        }
        return pkg;
    }

    fun wikiConvert(text: String, linkRenderer: LinkRenderer, fileName: String?): String {
        return markdownProcessor.markdownToHtml(text, linkRenderer).sure()
    }

    protected fun isLocal(descriptor: DeclarationDescriptor): Boolean {
        return if (descriptor is ModuleDescriptor) {
            true
        } else {
            val parent = descriptor.getContainingDeclaration()
            if (parent != null) {
                isLocal(parent)
            } else {
                false
            }
        }
    }

    fun addFunctions(owner: KClassOrPackage, scope: JetScope): Unit {
        try {
            val descriptors = scope.getAllDescriptors()
            for (descriptor in descriptors) {
                if (descriptor is PropertyDescriptor) {
                    val name = descriptor.getName()
                    val returnType = getType(descriptor.getReturnType())
                    if (returnType != null) {
                        val receiver = descriptor.getReceiverParameter()
                        val extensionClass = if (receiver is ExtensionReceiver) {
                            getType(receiver.getType())
                        } else null

                        val property = KProperty(owner, descriptor, name, returnType, extensionClass?.klass)
                        owner.properties.add(property)
                    }
                } else if (descriptor is CallableDescriptor) {
                    val function = createFunction(owner, descriptor)
                    if (function != null) {
                        owner.functions.add(function)
                    }
                }
            }
        } catch (e: Throwable) {
            warning("Caught exception finding function declarations on $owner $e")
            e.printStackTrace()
        }

    }

    protected fun createFunction(owner: KClassOrPackage, descriptor: CallableDescriptor): KFunction? {
        val returnType = getType(descriptor.getReturnType())
        if (returnType != null) {
            val name = descriptor.getName() ?: "null"
            val parameters = ArrayList<KParameter>()
            val params = descriptor.getValueParameters()
            for (param in params) {
                if (param != null) {
                    val p = createParameter(param)
                    if (p != null) {
                        parameters.add(p)
                    }
                }
            }
            val function = KFunction(descriptor, owner, name, returnType, parameters)
            addTypeParameters(function.typeParameters, descriptor.getTypeParameters())
            configureComments(function, descriptor)
            val receiver = descriptor.getReceiverParameter()
            if (receiver is ExtensionReceiver) {
                val receiverType = getType(receiver.getType())
                function.receiverType = receiverType
                function.extensionClass = receiverType?.klass
            }
            return function
        }
        return null
    }

    fun addTypeParameters(answer: List<KTypeParameter>, descriptors: List<TypeParameterDescriptor?>): Unit {
        for (typeParam in descriptors) {
            if (typeParam != null) {
                val p = createTypeParameter(typeParam)
                if (p != null){
                    answer.add(p)
                }
            }
        }
    }

    protected fun createTypeParameter(descriptor: TypeParameterDescriptor): KTypeParameter? {
        val name = descriptor.getName()
        val answer = KTypeParameter(name, descriptor, this)
        configureComments(answer, descriptor)
        return answer
    }

    protected fun createParameter(descriptor: ValueParameterDescriptor): KParameter? {
        val returnType = getType(descriptor.getReturnType())
        if (returnType != null) {
            val name = descriptor.getName()
            val answer = KParameter(descriptor, name, returnType)
            configureComments(answer, descriptor)
            return answer
        }
        return null
    }


    fun fileFor(descriptor: DeclarationDescriptor): String? {
        val psiElement = getPsiElement(descriptor)
        return psiElement?.getContainingFile()?.getName()
    }

    protected fun getPsiElement(descriptor: DeclarationDescriptor): PsiElement? {
        return try {
            BindingContextUtils.descriptorToDeclaration(context, descriptor)
        } catch (e: Throwable) {
            // ignore exceptions on fake descriptors
            null
        }
    }

    protected fun commentsFor(descriptor: DeclarationDescriptor): String {
        val psiElement =  getPsiElement(descriptor)

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
            val lines = text.trim().split("\\n")
            if (lines != null) {
                // lets remove the /** ... * ... */ tokens
                val buffer = StringBuilder()
                val last = lines.size - 1
                for (i in 0.upto(last)) {
                    var text = lines[i] ?: ""
                    text = text.trim()
                    if (i == 0) {
                        text = text.trimLeading("/**").trimLeading("/*")
                    } else {
                        buffer.append("\n")
                    }
                    if (i >= last) {
                        text = text.trimTrailing("*/")
                    } else if (i > 0) {
                        text = text.trimLeading("* ")
                        if (text == "*") text = ""
                    }
                    // lets check for javadoc style @ tags and macros
                    if (text.startsWith("@")) {
                        val remaining = text.substring(1)
                        val macro = "includeFunctionBody"
                        if (remaining.startsWith(macro)) {
                            val next = remaining.substring(macro.length()).trim()
                            val words = next.split("\\s")
                            // TODO we could default the test function name to match that of the
                            // source code function if folks adopted a convention of naming the test method after the
                            // method its acting as a demo/test for
                            if (words != null && words.size > 1) {
                                val includeFile = words[0].sure()
                                val fnName = words[1].sure()
                                val content = findFunctionInclude(psiElement, includeFile, fnName)
                                if (content != null) {
                                    text = highlighter.highlight(content)
                                } else {
                                    warning("could not find function $fnName in file $includeFile from source file ${psiElement.getContainingFile()}")
                                }
                             }
                        } else {
                            warning("Uknown kdoc macro @$remaining")
                        }
                    }
                    buffer.append(text)
                }
                return buffer.toString() ?: ""
            } else {
                return text
            }
        }
        return ""
    }

    protected fun findFunctionInclude(psiElement: PsiElement, includeFile: String, functionName: String): String? {
        var dir = psiElement.getContainingFile()?.getParent()
        if (dir != null) {
            val file = relativeFile(dir.sure(), includeFile)
            if (file != null) {
                val text = file.getText()
                if (text != null) {
                    // lets find the function definition
                    val regex = """fun\s+$functionName\(.*\)""".toRegex()
                    val matcher = regex.matcher(text)!!
                    if (matcher.find()) {
                        val idx = matcher.end()
                        val remaining = text.substring(idx)
                        return extractBlock(remaining)
                    }
                }
            }
        }
        return null
    }

    /**
     * Extracts the block of code within { .. } tokens or returning null if it can't be found
     */
    protected fun extractBlock(text: String): String? {
        val idx = text.indexOf('{')
        if (idx >= 0) {
            var remaining = text.substring(idx + 1)
            // lets remove any leading blank lines
            while (true) {
                val nidx = remaining.indexOf('\n')
                if (nidx >= 0) {
                    val line = remaining.substring(0, nidx).trim()
                    if (line.isEmpty()) {
                        remaining = remaining.substring(nidx + 1)
                        continue
                    }
                }
                break
            }
            var count = 1
            for (i in 0.upto(remaining.size - 1)) {
                val ch = remaining[i]
                if (ch == '{') count ++
                else if (ch == '}') {
                    if (--count <= 0) {
                        return remaining.substring(0, i)
                    }
                }
            }
            warning("missing } in code block for $remaining")
            return remaining
        }
        return null
    }

    protected fun relativeFile(directory: PsiDirectory, relativeName: String): PsiFile? {
        // TODO would have thought there's some helper function already to resolve relative names!

        var dir: PsiDirectory? = directory

        // lets try resolve the include name relative to this file
        val paths = relativeName.split("/")
        if (paths != null) {
            val size = paths.size
            for (i in 0.upto(size - 2)) {
                val path = paths[i]
                if (path == ".") continue
                else if (path == "..") dir = dir?.getParent()
                else if (path != null) dir = dir?.findSubdirectory(path)
            }
            val name = paths[size - 1]
            if (dir != null && name != null) {
                val file = dir?.findFile(name)
                if (file != null) {
                    return file
                } else {
                    warning("could not find file $relativeName in $dir with name $name")
                }
            }
        }
        return null
    }

    fun configureComments(annotated: KAnnotated, descriptor: DeclarationDescriptor): Unit {
        val detailedText = commentsFor(descriptor).trim()
        annotated.wikiDescription = detailedText
    }

    fun getType(aType: JetType?): KType? {
        if (aType != null) {
            val classifierDescriptor = aType.getConstructor().getDeclarationDescriptor()
            val klass = if (classifierDescriptor is ClassDescriptor) {
                getClass(classifierDescriptor)
            } else null
            return KType(aType, this, klass)
        }
        return null
    }

    /**
     * Returns the [[KClass]] for the fully qualified name or null if it could not be found
     */
    fun getClass(qualifiedName: String): KClass? {
        // TODO warning this only works for top level classes
        // a better algorithm is to walk down each dot path dealing with nested packages/classes
        val idx = qualifiedName.lastIndexOf('.')
        val pkgName = if (idx >= 0) qualifiedName.substring(0, idx) ?: "" else ""
        val pkg = getPackage(pkgName)
        if (pkg != null) {
            val simpleName = if (idx >= 0) qualifiedName.substring(idx + 1) ?: "" else qualifiedName
            return pkg.classMap.get(simpleName)
        }
        return null
    }

    fun getClass(classElement: ClassDescriptor): KClass? {
        val name = classElement.getName()
        val container = classElement.getContainingDeclaration()
        if (name != null && container is NamespaceDescriptor) {
            val pkg = getPackage(container)
            return pkg.getClass(name, classElement)
        } else {
            warning("no package found for $container and class $name")
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

class TemplateLinkRenderer(val annotated: KAnnotated, val template: KDocTemplate): LinkRenderer() {

    override fun render(node: WikiLinkNode?): Rendering? {
        val answer = super.render(node)
        if (answer != null) {
            val text = answer.text
            if (text != null) {
                val qualified = resolveToQualifiedName(text)

                var href = resolveClassNameLink(qualified)
                if (href != null) {
                    answer.href = href
                } else {

                    // TODO really dirty hack alert!!!
                    // until the resolver is working, lets try adding a few prefixes :)
                    for (prefix in arrayList("java.lang", "java.util", "java.util.regex", "java.io", "jet"))
                        if (href == null) {
                            href = resolveClassNameLink(prefix + "." + qualified)
                        }
                    /** TODO use break when KT-1523 is fixed
                    if (href != null) {
                        break
                    }
                    */
                }
                if (href != null) {
                    answer.href = href
                } else {
                    warning("could not resolve expression: $qualified into a wiki link")
                }
            }
        }
        return answer
    }

    /**
     * Try to resolve a fully qualified class name as a link
     */
    protected fun resolveClassNameLink(qualifiedName: String): String? {
        val model = annotated.model
        val pkg = model.getPackage(qualifiedName)
        if (pkg != null) {
            return template.href(pkg)
        }
        val klass = model.getClass(qualifiedName)
        if (klass != null) {
            return template.href(klass)
        } else {
            // is it a method?
            val idx = qualifiedName.lastIndexOf('.')
            if (idx > 0) {
                val className = qualifiedName.substring(0, idx)
                val c = model.getClass(className)
                if (c != null) {
                    // lets try find method...
                    val remaining = qualifiedName.substring(idx + 1)
                    // lets try find the function
                    val fn = c.findFunction(remaining)
                    if (fn != null) {
                        return template.href(fn)
                    }
                    val p = c.findProperty(remaining)
                    if (p != null) {
                        return template.href(p)
                    }
                }
            }
            return null
        }
    }
    /**
     * Attempts to resolve the class, method or property expression using the
     * current imports and declaraiton
     */
    protected fun resolveToQualifiedName(text: String): String {
        // TODO use the CompletionContributors maybe to figure out what local names are imported???
        return text
        /*
                val scope = findWritableScope(annotated.declarationDescriptor)
                if (scope != null) {
                    val classifierDescriptor = scope.getClassifier(text)
                    if (classifierDescriptor == null) {
                        val o = scope.getObjectDescriptor(text)
                        println("Attempt to resolve HREF: $text Found objectDescriptor $o")
                    } else {
                        println("Attempt to resolve HREF: $text Found classifierDescriptor $classifierDescriptor")
                    }
                }
            }

            protected fun findWritableScope(declarationDescriptor: DeclarationDescriptor) : WritableScopeImpl? {
                val container = declarationDescriptor.getContainingDeclaration()
                if (container is NamespaceDescriptor) {
                    val scope = container.getMemberScope()
                    if (scope is WritableScopeImpl) {
                        return scope
                    }
                } else if (container != null) {
                    return findWritableScope(container)
                }
                return null

        */
    }

    override fun render(node: RefLinkNode?, url: String?, title: String?, text: String?): Rendering? {
        return super.render(node, url, title, text)
    }

    override fun render(node: AutoLinkNode?): Rendering? {
        return super.render(node)
    }

    override fun render(node: ExpLinkNode?, text: String?): Rendering? {
        return super.render(node, text)
    }


}

abstract class KAnnotated(val model: KModel, val declarationDescriptor: DeclarationDescriptor) {
    public open var wikiDescription: String = ""

    public open var deprecated: Boolean = false

    fun description(template: KDocTemplate): String {
        val detailedText = detailedDescription(template)
        val idx = detailedText.indexOf("</p>")
        return if (idx > 0) {
            detailedText.substring(0, idx).trimLeading("<p>")
        } else {
            detailedText
        }
    }

    fun detailedDescription(template: KDocTemplate): String {
        val file = model.fileFor(declarationDescriptor)
        return model.wikiConvert(wikiDescription, TemplateLinkRenderer(this, template), file)
    }
}

abstract class KNamed(val name: String, model: KModel, declarationDescriptor: DeclarationDescriptor): KAnnotated(model, declarationDescriptor), Comparable<KNamed> {

    override fun compareTo(other: KNamed): Int = name.compareTo(other.name)

    open fun equals(other: KPackage) = name == other.name

    open fun toString() = name
}


class KPackage(model: KModel, val descriptor: NamespaceDescriptor,
        val name: String,
        var local: Boolean = false): KClassOrPackage(model, descriptor), Comparable<KPackage> {

    override fun compareTo(other: KPackage): Int = name.compareTo(other.name)

    fun equals(other: KPackage) = name == other.name

    fun toString() = "KPackage($name)"

    fun getClass(name: String, descriptor: ClassDescriptor): KClass {
        var created = false
        val klass = classMap.getOrPut(name) {
            created = true
            KClass(this, descriptor, name)
        }
        if (created) {
            model.configureComments(klass, descriptor)
            val typeConstructor = descriptor.getTypeConstructor()
            val superTypes = typeConstructor.getSupertypes()
            for (st in superTypes) {
                val sc = model.getType(st)
                if (sc != null) {
                    klass.baseClasses.add(sc)
                }
            }
            val scope = descriptor.getDefaultType().getMemberScope()
            model.addFunctions(klass, scope)
            model.addTypeParameters(klass.typeParameters, typeConstructor.getParameters())
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

    // TODO generates java.lang.NoSuchMethodError: kotlin.util.namespace.hashMap(Ljet/TypeInfo;Ljet/TypeInfo;)Ljava/util/HashMap;
    //val classes = sortedMap<String,KClass>()
    public val classMap: SortedMap<String, KClass> = TreeMap<String, KClass>()

    public val classes: Collection<KClass>
    get() = classMap.values().sure().filter{ it.isApi() }

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

class KType(val jetType: JetType, model: KModel, val klass: KClass?, val arguments: List<KType> = ArrayList<KType>())
: KNamed(klass?.name ?: jetType.toString(), model, jetType.getConstructor().getDeclarationDescriptor().sure()) {
    {
        if (klass != null) {
            this.wikiDescription = klass.wikiDescription
        }
        for (arg in jetType.getArguments()) {
            if (arg != null) {
                val argJetType = arg.getType()
                val t = model.getType(argJetType)
                if (t != null) {
                    $arguments.add(t)
                }
            }
        }
    }

    override fun toString() = if (nullable) "$name?" else name

    val nullable: Boolean
    get() = jetType.isNullable()
}

class KClass(val pkg: KPackage, val descriptor: ClassDescriptor,
        val simpleName: String,
        var group: String = "Other",
        var annotations: List<KAnnotation> = arrayList<KAnnotation>(),
        var typeParameters: List<KTypeParameter> = arrayList<KTypeParameter>(),
        var since: String = "",
        var authors: List<String> = arrayList<String>(),
        var baseClasses: List<KType> = arrayList<KType>(),
        var nestedClasses: List<KClass> = arrayList<KClass>(),
        var sourceLine: Int = 2): KClassOrPackage(pkg.model, descriptor), Comparable<KClass> {

    override fun compareTo(other: KClass): Int = name.compareTo(other.name)

    fun equals(other: KClass) = name == other.name

    fun toString() = "$kind($name)"

    fun isApi(): Boolean {
        val visibility = descriptor.getVisibility()
        return visibility.isPublicAPI()
    }

    val kind: String
    get() {
        val k = descriptor.getKind()
        return if (k == ClassKind.TRAIT) "trait"
        else if (k == ClassKind.OBJECT) "object"
        else if (k == ClassKind.ENUM_CLASS || k == ClassKind.ENUM_ENTRY) "enum"
        else if (k == ClassKind.ANNOTATION_CLASS) "annotation"
        else "class"
    }

    val kindCode: String
    get() {
        val k = descriptor.getKind()
        return if (k == ClassKind.TRAIT) "trait"
        else if (k == ClassKind.OBJECT) "object"
        else if (k == ClassKind.ENUM_CLASS || k == ClassKind.ENUM_ENTRY) "enum class"
        else if (k == ClassKind.ANNOTATION_CLASS) "class"
        else "class"
    }

    val visibility: String
    get() {
        val v = descriptor.getVisibility()
        return if (v == Visibilities.PUBLIC) "public"
        else if (v == Visibilities.PROTECTED) "protected"
        else if (v == Visibilities.PRIVATE) "private"
        else ""
    }

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
        for (b in baseClasses) {
            val c = b.klass
            if (c != null) {
                answer.add(c)
                c.descendants(answer)
            }
        }
        return answer
    }
}

class KFunction(val descriptor: CallableDescriptor, val owner: KClassOrPackage, val name: String,
        var returnType: KType,
        var parameters: List<KParameter>,
        var receiverType: KType? = null,
        var extensionClass: KClass? = null,
        var modifiers: List<String> = arrayList<String>(),
        var typeParameters: List<KTypeParameter> = arrayList<KTypeParameter>(),
        var exceptions: List<KClass> = arrayList<KClass>(),
        var annotations: List<KAnnotation> = arrayList<KAnnotation>(),
        var sourceLine: Int = 2): KAnnotated(owner.model, descriptor), Comparable<KFunction> {

    public val parameterTypeText: String = parameters.map{ it.aType.name }.join(", ")

    override fun compareTo(other: KFunction): Int {
        var answer = name.compareTo(other.name)
        if (answer == 0) {
            answer = parameterTypeText.compareTo(other.parameterTypeText)
            if (answer == 0) {
                val ec1 = extensionClass?.name ?: ""
                val ec2 = other.extensionClass?.name ?: ""
                answer = ec1.compareTo(ec2)
            }
        }
        return answer
    }

    fun equals(other: KFunction) = name == other.name && this.parameterTypeText == other.parameterTypeText &&
    this.extensionClass == other.extensionClass && this.owner == other.owner

    fun toString() = "fun $name($parameterTypeText): $returnType"

    public val link: String = "$name($parameterTypeText)"

    /** Returns a list of generic type parameter names kinds like "A, I" */
    public val typeParametersText: String
    get() = typeParameters.map{ it.name }.join(", ")
}

class KProperty(val owner: KClassOrPackage, val descriptor: PropertyDescriptor, val name: String,
        val returnType: KType, val extensionClass: KClass?): KAnnotated(owner.model, descriptor), Comparable<KProperty> {

    override fun compareTo(other: KProperty): Int = name.compareTo(other.name)

    public val link: String = "$name"

    fun equals(other: KFunction) = name == other.name

    fun isVar(): Boolean = descriptor.isVar()

    fun toString() = "property $name"
}

class KParameter(val descriptor: ValueParameterDescriptor, val name: String,
        var aType: KType): KAnnotated(aType.model, aType.declarationDescriptor)  {

    fun toString() = "$name: ${aType.name}"

    fun isVarArg(): Boolean = descriptor.getVarargElementType() != null

    fun hasDefaultValue(): Boolean = descriptor.hasDefaultValue()

    fun varArgType(): KType? {
        val varType = descriptor.getVarargElementType()
        return if (varType != null) {
            aType.model.getType(varType)
        } else null
    }
}

class KTypeParameter(val name: String,
        val descriptor: TypeParameterDescriptor,
        model: KModel,
        var extends: List<KClass> = arrayList<KClass>()): KAnnotated(model, descriptor) {

    fun toString() = "$name"
}

class KAnnotation(var klass: KClass): KAnnotated(klass.model, klass.descriptor)  {

    // TODO add some parameter values?

    fun toString() = "@$klass.simpleName"
}
