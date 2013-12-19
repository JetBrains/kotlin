package org.jetbrains.kotlin.doc.model

import java.io.File
import java.util.*
import com.intellij.openapi.vfs.local.CoreLocalVirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileSystemItem
import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.descriptors.ClassKind
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor
import org.jetbrains.jet.lang.descriptors.Visibilities
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils.LineAndColumn
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.resolve.BindingContextUtils
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.kotlin.doc.*
import org.jetbrains.kotlin.doc.highlighter.SyntaxHighligher
import org.jetbrains.kotlin.doc.templates.KDocTemplate
import org.pegdown.Extensions
import org.pegdown.LinkRenderer
import org.pegdown.LinkRenderer.Rendering
import org.pegdown.PegDownProcessor
import org.pegdown.ast.AutoLinkNode
import org.pegdown.ast.ExpLinkNode
import org.pegdown.ast.RefLinkNode
import org.pegdown.ast.WikiLinkNode
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor

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
    }
    else if (descriptor is PackageFragmentDescriptor) {
        return descriptor.getFqName().asString()
    }
    else {
        val parent = containerName(descriptor)
        var name = descriptor.getName().asString()
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

fun info(message: String) {
    // println("info: $message")
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
    val map = TreeMap<KClass, MutableList<KFunction>>()
    functions.filter{ it.extensionClass != null }.groupByTo(map){ it.extensionClass!! }
    return map
}

// TODO for some reason the SortedMap causes kotlin to freak out a little :)
fun extensionProperties(properties: Collection<KProperty>): Map<KClass, List<KProperty>> {
    val map = TreeMap<KClass, MutableList<KProperty>>()
    properties.filter{ it.extensionClass != null }.groupByTo(map){ it.extensionClass!! }
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

// htmlPath does not include "html-src" prefix
class SourceInfo(val psi: JetFile, val relativePath: String, val htmlPath: String)

class KModel(val context: BindingContext, val config: KDocConfig, val sourceDirs: List<File>, val sources: List<JetFile>) {
    // TODO generates java.lang.NoSuchMethodError: kotlin.util.UtilPackage.hashMap(Ljet/TypeInfo;Ljet/TypeInfo;)Ljava/util/HashMap;
    //val packages = sortedMap<String,KPackage>()
    public val packageMap: SortedMap<String, KPackage> = TreeMap<String, KPackage>()

    public val allPackages: Collection<KPackage>
    get() = packageMap.values()!!

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

    private var _projectRootDir: String? = null

    /**
     * File names we look for in a package directory for the overall description of a package for KDoc
     */
    val packageDescriptionFiles = arrayList("readme.md", "ReadMe.md, readme.html, ReadMe.html")

    private val readMeDirsScanned = HashSet<String>()



    val sourcesInfo: List<SourceInfo>
    ;{

        val normalizedSourceDirs: List<String> =
        sourceDirs.map { file -> file.getCanonicalPath()!! }

        fun relativePath(psiFile: PsiFile): String {
            val file = File((psiFile.getVirtualFile() as CoreLocalVirtualFile).getPath()!!).getCanonicalFile()!!
            val filePath = file.getPath()!!
            for (sourceDirPath in normalizedSourceDirs) {
                if (filePath.startsWith(sourceDirPath) && filePath.length() > sourceDirPath.length()) {
                    return filePath.substring(sourceDirPath.length + 1)
                }
            }
            throw Exception("$file is not a child of any source roots $normalizedSourceDirs")
        }

        sourcesInfo = sources.map { source ->
            val relativePath = relativePath(source)
            val htmlPath = relativePath.replaceFirst("\\.kt$", "") + ".html"
            SourceInfo(source, relativePath, htmlPath)
        }
    }

    private val sourceInfoByFile = sourcesInfo.toHashMapMappingToKey<JetFile, SourceInfo> { sourceInfo -> sourceInfo.psi }

    fun sourceInfoByFile(file: JetFile) = sourceInfoByFile.get(file)!!


    ;{
        /** Loads the model from the given set of source files */
        val allPackageFragments = HashSet<PackageFragmentDescriptor>()
        for (source in sources) {
            // We retrieve a descriptor by a PSI element from the context
            val packageFragment = context.get(BindingContext.FILE_TO_PACKAGE_FRAGMENT, source)
            if (packageFragment != null) {
                allPackageFragments.add(packageFragment);
            } else {
                warning("No NamespaceDescriptor for source $source")
            }
        }
        val allClasses = HashSet<KClass>()
        for (packageFragment in allPackageFragments) {
            getPackage(packageFragment)
            for (descriptor in packageFragment.getMemberScope().getAllDescriptors()) {
                if (descriptor is ClassDescriptor) {
                    val klass = getClass(descriptor)
                    if (klass != null) {
                        allClasses.add(klass)
                    }
                }
            }
        }
    }




    /**
    * Returns the root project directory for calculating relative source links
    */
    fun projectRootDir(): String {
        if (_projectRootDir == null) {
            val rootDir = config.projectRootDir
            _projectRootDir = if (rootDir == null) {
                warning("KDocConfig does not have a projectRootDir defined so we cannot generate relative source Hrefs")
                ""
            } else {
                File(rootDir).getCanonicalPath() ?: ""
            }
        }
        return _projectRootDir ?: ""
    }


    /* Returns the package for the given name or null if it does not exist */
    fun getPackage(name: String): KPackage? = packageMap.get(name)

    /** Returns the package for the given descriptor, creating one if its not available */
    fun getPackage(descriptor: PackageFragmentDescriptor): KPackage {
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
            pkg.useExternalLink = pkg.model.config.resolveLink(pkg.name, false).isNotEmpty()

            if (pkg.wikiDescription.isEmpty()) {
                // lets try find a custom doc
                var file = config.packageDescriptionFiles[name]
                loadWikiDescription(pkg, file)
            }
        }
        return pkg;
    }

    protected fun loadWikiDescription(pkg: KPackage, file: String?): Unit {
        if (file != null) {
            try {
                pkg.wikiDescription = File(file).readText()
            } catch (e: Throwable) {
                warning("Failed to load package ${pkg.name} documentation file $file. Reason $e")
            }
        }
    }

    /**
     * If a package has no detailed description lets try load it from the descriptors
     * source directory if we've not checked that directory before
     */
    fun tryLoadReadMe(pkg: KPackage, descriptor: DeclarationDescriptor): Unit {
        if (pkg.wikiDescription.isEmpty()) {
            // lets try find the package.html or package.md file
            val srcPath =  pkg.model.filePath(descriptor)
            if (srcPath != null) {
                val srcFile = File(srcPath)
                val dir = if (srcFile.isDirectory()) srcFile else srcFile.getParentFile()
                if (dir != null && readMeDirsScanned.add(dir.getPath()!!)) {
                    val f = packageDescriptionFiles.map{ File(dir, it) }.find{ it.exists() }
                    if (f != null) {
                        val file = f.getCanonicalPath()
                        loadWikiDescription(pkg, file)
                    }
                    else {
                        info("package ${pkg.name} has no ReadMe.(html|md) in $dir")
                    }
                }
            }
        }
    }

    fun wikiConvert(text: String, linkRenderer: LinkRenderer, fileName: String?): String {
        return markdownProcessor.markdownToHtml(text, linkRenderer)!!
    }

    fun sourceLinkFor(filePath: String, sourceLine: Int, lineLinkText: String = "#L"): String? {
        val root = config.sourceRootHref
        if (root != null) {
            // lets remove the root project directory
            val rootDir = projectRootDir()
            val canonicalFile = File(filePath).getCanonicalPath() ?: ""
            //println("=========== root dir for filePath: $canonicalFile is $rootDir")
            val relativeFile =
                if (canonicalFile.startsWith(rootDir))
                    canonicalFile.substring(rootDir.length())
                else
                    canonicalFile
            val cleanRoot = root.trimTrailing("/")
            val cleanPath = relativeFile.trimLeading("/")
            return "$cleanRoot/$cleanPath$lineLinkText$sourceLine"
        }
        return null
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
                    val name = descriptor.getName().asString()
                    val returnType = getType(descriptor.getReturnType())
                    if (returnType != null) {
                        val receiver = descriptor.getReceiverParameter()
                        val extensionClass = if (receiver != null) {
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
            val name = descriptor.getName().asString()
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
            if (receiver != null) {
                val receiverType = getType(receiver.getType())
                function.receiverType = receiverType
                function.extensionClass = receiverType?.klass
            }
            return function
        }
        return null
    }

    fun addTypeParameters(answer: MutableList<KTypeParameter>, descriptors: List<TypeParameterDescriptor?>): Unit {
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
        val name = descriptor.getName().asString()
        val answer = KTypeParameter(name, descriptor, this)
        configureComments(answer, descriptor)
        return answer
    }

    protected fun createParameter(descriptor: ValueParameterDescriptor): KParameter? {
        val returnType = getType(descriptor.getReturnType())
        if (returnType != null) {
            val name = descriptor.getName().asString()
            val answer = KParameter(descriptor, name, returnType)
            configureComments(answer, descriptor)
            return answer
        }
        return null
    }

    fun locationFor(descriptor: DeclarationDescriptor): LineAndColumn? {
        val psiElement =  getPsiElement(descriptor)
        if (psiElement != null) {
            val document = psiElement.getContainingFile()?.getViewProvider()?.getDocument()
            if (document != null) {
                val offset = psiElement.getTextOffset()
                return DiagnosticUtils.offsetToLineAndColumn(document, offset)
            }
        }
        return null
    }

    fun fileFor(descriptor: DeclarationDescriptor): String? {
        val psiElement = getPsiElement(descriptor)
        return psiElement?.getContainingFile()?.getName()
    }

    fun filePath(descriptor: DeclarationDescriptor): String? {
        val psiElement = getPsiElement(descriptor)
        val file = psiElement?.getContainingFile()
        return filePath(file)
    }


    fun getPsiElement(descriptor: DeclarationDescriptor): PsiElement? {
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
            // lets remove the /** ... * ... */ tokens
            val buffer = StringBuilder()
            val last = lines.size - 1
            for (i in 0.rangeTo(last)) {
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
                text = processMacros(text, psiElement)
                buffer.append(text)
            }
            return buffer.toString() ?: ""
        }
        return ""
    }

    protected fun processMacros(textWithWhitespace: String, psiElement: PsiElement): String {
        val text = textWithWhitespace.trim()
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
                if (words.size > 1) {
                    val includeFile = words[0]!!
                    val fnName = words[1]!!
                    val content = findFunctionInclude(psiElement, includeFile, fnName)
                    if (content != null) {
                        return content
                    } else {
                        warning("could not find function $fnName in file $includeFile from source file ${psiElement.getContainingFile()}")
                    }
                }
            } else {
                warning("Unknown kdoc macro @$remaining")
            }
        }
        return textWithWhitespace
    }

    protected fun findFunctionInclude(psiElement: PsiElement, includeFile: String, functionName: String): String? {
        var dir = psiElement.getContainingFile()?.getParent()
        if (dir != null) {
            val file = relativeFile(dir!!, includeFile)
            if (file != null) {
                val text = file.getText()
                if (text != null) {
                    // lets find the function definition
                    val regex = """fun\s+$functionName\(.*\)""".toRegex()
                    val matcher = regex.matcher(text)!!
                    if (matcher.find()) {
                        val idx = matcher.end()
                        val remaining = text.substring(idx)
                        val content = extractBlock(remaining)
                        if (content != null) {
                            val highlight = highlighter.highlight(content)
                            val filePath = filePath(file)
                            val sourceLine = text.substring(0, idx).count{ it == '\n'} + 1
                            val link = if (filePath != null) sourceLinkFor(filePath, sourceLine) else  null
                            return if (link != null)
                                """<div class="source-detail"><a href="$link" target="_top"  class="repoSourceCode">source</a></div>
$highlight"""
                            else highlight
                        }
                    }
                }
            }
        }
        return null
    }

    protected fun filePath(file: PsiFileSystemItem?): String? {
        if (file != null) {
            var dir = file.getParent()
            if (dir != null) {
                val parentName = filePath(dir) ?: ""
                return parentName + "/" + file.getName()
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
            for (i in 0.rangeTo(remaining.size - 1)) {
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
        val size = paths.size
        for (i in 0.rangeTo(size - 2)) {
            val path = paths[i]
            if (path == ".") continue
            else if (path == "..") dir = dir?.getParent()
            else dir = dir?.findSubdirectory(path)
        }
        val name = paths[size - 1]
        if (dir != null) {
            val file = dir?.findFile(name)
            if (file != null) {
                return file
            } else {
                warning("could not find file $relativeName in $dir with name $name")
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
        val pkgName = if (idx >= 0) qualifiedName.substring(0, idx) else ""
        val pkg = getPackage(pkgName)
        if (pkg != null) {
            val simpleName = if (idx >= 0) qualifiedName.substring(idx + 1) else qualifiedName
            return pkg.classMap.get(simpleName)
        }
        return null
    }

    fun getClass(classElement: ClassDescriptor): KClass? {
        val name = classElement.getName().asString()
        var dec: DeclarationDescriptor? = classElement.getContainingDeclaration()
        while (dec != null) {
            val container = dec
            if (container is PackageFragmentDescriptor) {
                val pkg = getPackage(container)
                return pkg.getClass(classElement)
            } else {
                dec = dec?.getContainingDeclaration()
            }
        }
        warning("no package found for class $name")
        return null
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
    // TODO dirty hack - remove when this issue is fixed
    // http://youtrack.jetbrains.com/issue/KT-1524
    val hackedLinks = hashMap(
            Pair("IllegalArgumentException", Pair("java.lang", "java/lang/IllegalArgumentException.html")),
            Pair("IllegalStateException", Pair("java.lang", "java/lang/IllegalStateException.html")),
            Pair("Map.Entry", Pair("java.util", "java/util/Map.Entry.html")),
            Pair("System.in", Pair("java.lang", "java/lang/System.html#in")),
            Pair("System.out", Pair("java.lang", "java/lang/System.html#in")),
            Pair("#equals()", Pair("java.lang", "java/lang/Object.html#equals(java.lang.Object)")),
            Pair("#hashCode()", Pair("java.lang", "java/lang/Object.html#hashCode()"))
    )


    public override fun render(node: WikiLinkNode?): Rendering? {
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
                    for (prefix in arrayList("java.lang", "java.util", "java.util.concurrent", "java.util.regex", "java.io",
                            "jet", "java.awt", "java.awt.event", "java.sql", "java.beans",
                            "javax.swing", "javax.swing.event",
                            "org.w3c.dom",
                            "kotlin.template")) {
                        if (href == null) {
                            href = resolveClassNameLink(prefix + "." + qualified)
                            if (href != null) {
                                break
                            }
                        }
                    }
                }
                if (href == null) {
                    // TODO even hacker than the above hack!
                    val link = hackedLinks.get(text)
                    if (link != null) {
                        href = annotated.model.config.resolveLink(link.first) + link.second
                    }
                }
                if (href != null) {
                    answer.href = href
                } else {
                    answer.href = "#NotImplementedYet"
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

    public override fun render(node: RefLinkNode?, url: String?, title: String?, text: String?): Rendering? {
        return super.render(node, url, title, text)
    }

    public override fun render(node: AutoLinkNode?): Rendering? {
        return super.render(node)
    }

    public override fun render(node: ExpLinkNode?, text: String?): Rendering? {
        return super.render(node, text)
    }


}

abstract class KAnnotated(val model: KModel, val declarationDescriptor: DeclarationDescriptor) {
    public open var wikiDescription: String = ""

    public open var deprecated: Boolean = false

    open fun description(template: KDocTemplate): String {
        val detailedText = detailedDescription(template)
        val idx = detailedText.indexOf("</p>")
        return if (idx > 0) {
            detailedText.substring(0, idx).trimLeading("<p>")
        } else {
            detailedText
        }
    }

    fun detailedDescription(template: KDocTemplate): String {
        val wiki = wikiDescription
        return wikiConvert(wiki, template)
    }

    protected fun wikiConvert(wiki: String, template: KDocTemplate): String {
        val file = model.fileFor(declarationDescriptor)
        return model.wikiConvert(wiki, TemplateLinkRenderer(this, template), file)
    }

    fun isLinkToSourceRepo(): Boolean {
        return model.config.sourceRootHref != null
    }

    fun sourceTargetAttribute(): String {
        return if (isLinkToSourceRepo()) " target=\"_top\" class=\"repoSourceCode\"" else ""
    }

    fun sourceLink(): String {
        val file = filePath()
        if (file != null) {
            val link = model.sourceLinkFor(file, sourceLine)
            if (link != null) return link
        }
        return ""
    }

    fun filePath(): String? = model.filePath(declarationDescriptor)

    fun location(): LineAndColumn? = model.locationFor(declarationDescriptor)

    val sourceLine: Int
    get() {
        val loc = location()
        return if (loc != null) loc.getLine() else 1
    }
}

abstract class KNamed(val name: String, model: KModel, declarationDescriptor: DeclarationDescriptor): KAnnotated(model, declarationDescriptor), Comparable<KNamed> {

    public override fun compareTo(other: KNamed): Int = name.compareTo(other.name)

    open fun equals(other: KPackage) = name == other.name

    open fun toString() = name
}


class KPackage(model: KModel, val descriptor: PackageFragmentDescriptor,
        val name: String,
        var local: Boolean = false,
        var useExternalLink: Boolean = false): KClassOrPackage(model, descriptor), Comparable<KPackage> {


    // TODO generates java.lang.NoSuchMethodError: kotlin.util.UtilPackage.hashMap(Ljet/TypeInfo;Ljet/TypeInfo;)Ljava/util/HashMap;
    //val classes = sortedMap<String,KClass>()
    public val classMap: SortedMap<String, KClass> = TreeMap<String, KClass>()

    public val classes: Collection<KClass>
    get() = classMap.values()!!.filter{ it.isApi() }

    public val annotations: Collection<KClass> = ArrayList<KClass>()

    public override fun compareTo(other: KPackage): Int = name.compareTo(other.name)

    fun equals(other: KPackage) = name == other.name

    fun toString() = "KPackage($name)"

    fun getClass(descriptor: ClassDescriptor): KClass {
        val name = descriptor.getName().asString()
        var created = false
        val klass = classMap.getOrPut(name) {
            created = true
            val psiFile = model.getPsiElement(descriptor)?.getContainingFile()
            val jetFile = psiFile as? JetFile
            val sourceInfo = if (jetFile != null) model.sourceInfoByFile(jetFile) else null
            KClass(this, descriptor, sourceInfo)
        }
        if (created) {
            // sometimes we may have source files for a package in different source directories
            // such as the kotlin package in generated directory; so lets always check if we can find
            // the readme
            model.tryLoadReadMe(this, descriptor)
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
            answer.add(n)
        }
        return answer;
    }

    /** Returns a relative path like ../.. for each path in the name */
    public val nameAsRelativePath: String
    get() {
        val answer = namePaths.map{ ".." }.makeString("/")
        return if (answer.length == 0) "" else answer + "/"
    }

    override fun description(template: KDocTemplate): String {
        // lets see if we can find a custom summary
        val text = model.config.packageSummaryText[name]
        return if (text != null)
            wikiConvert(text, template).trimLeading("<p>").trimTrailing("</p>")
        else
            super<KClassOrPackage>.description(template)
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
        return classes.groupByTo(TreeMap<String, MutableList<KClass>>()){it.group}
    }

    fun packageFunctions() = functions.filter{ it.extensionClass == null }

    fun packageProperties() = properties.filter{ it.extensionClass == null && it.isPublic() }
}

class KType(val jetType: JetType, model: KModel, val klass: KClass?, val arguments: MutableList<KType> = ArrayList<KType>())
: KNamed(klass?.name ?: jetType.toString()!!, model, jetType.getConstructor().getDeclarationDescriptor()!!) {
    {
        if (klass != null) {
            this.wikiDescription = klass.wikiDescription
        }
        for (arg in jetType.getArguments()) {
            if (arg != null) {
                val argJetType = arg.getType()
                val t = model.getType(argJetType)
                if (t != null) {
                    arguments.add(t)
                }
            }
        }
    }

    override fun toString() = if (nullable) "$name?" else name

    val nullable: Boolean
    get() = jetType.isNullable()
}

class KClass(
        val pkg: KPackage,
        val descriptor: ClassDescriptor,
        val sourceInfo: SourceInfo?)
    : KClassOrPackage(pkg.model, descriptor), Comparable<KClass>
{
    val simpleName = descriptor.getName().asString()
    var group: String = "Other"
    var annotations: List<KAnnotation> = arrayList<KAnnotation>()
    var typeParameters: MutableList<KTypeParameter> = arrayList<KTypeParameter>()
    var since: String = ""
    var authors: List<String> = arrayList<String>()
    var baseClasses: MutableList<KType> = arrayList<KType>()
    var nestedClasses: List<KClass> = arrayList<KClass>()

    public override fun compareTo(other: KClass): Int = name.compareTo(other.name)

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

    public val name: String = pkg.qualifiedName(descriptor.getName().asString())

    public val packageName: String = pkg.name

    /** Returns the name as a directory using '/' instead of '.' */
    public val nameAsPath: String
    get() = name.replace('.', '/')


    fun isAnnotation() = kind == "annotation"
    fun isInterface() = kind == "interface"

    /** Returns all of the base classes and all of their descendants */
    fun descendants(answer: MutableSet<KClass> = LinkedHashSet<KClass>()): Set<KClass> {
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
        var typeParameters: MutableList<KTypeParameter> = arrayList<KTypeParameter>(),
        var exceptions: List<KClass> = arrayList<KClass>(),
        var annotations: List<KAnnotation> = arrayList<KAnnotation>()): KAnnotated(owner.model, descriptor), Comparable<KFunction> {

    public val parameterTypeText: String = parameters.map{ it.aType.name }.makeString(", ")

    public override fun compareTo(other: KFunction): Int {
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
    get() = typeParameters.map{ it.name }.makeString(", ")
}

class KProperty(val owner: KClassOrPackage, val descriptor: PropertyDescriptor, val name: String,
        val returnType: KType, val extensionClass: KClass?): KAnnotated(owner.model, descriptor), Comparable<KProperty> {

    public override fun compareTo(other: KProperty): Int = name.compareTo(other.name)

    public val link: String = "$name"

    fun equals(other: KFunction) = name == other.name

    fun isVar(): Boolean = descriptor.isVar()

    fun kind(): String = if (isVar()) "var" else "val"

    fun isPublic(): Boolean {
        val visibility = descriptor.getVisibility()
        return visibility.isPublicAPI()
    }

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
