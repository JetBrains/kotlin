/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

import com.intellij.util.PathUtil
import com.intellij.util.xmlb.SerializationFilter
import com.intellij.util.xmlb.SkipDefaultsSerializationFilter
import com.intellij.util.xmlb.XmlSerializer
import org.jdom.DataConversionException
import org.jdom.Element
import org.jdom.Text
import org.jetbrains.kotlin.arguments.COMPILER_ARGUMENTS_ELEMENT_NAME
import org.jetbrains.kotlin.arguments.CompilerArgumentsDeserializerV5
import org.jetbrains.kotlin.arguments.CompilerArgumentsSerializerV5
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.platform.*
import org.jetbrains.kotlin.platform.impl.FakeK2NativeCompilerArguments
import org.jetbrains.kotlin.platform.impl.JvmIdePlatformKind
import org.jetbrains.kotlin.platform.js.isJs
import org.jetbrains.kotlin.platform.jvm.JdkPlatform
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.*
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

fun Element.getOption(name: String) = getChildren("option").firstOrNull { name == it?.getAttribute("name")?.value }

private fun Element.getOptionValue(name: String) = getOption(name)?.getAttribute("value")?.value

private fun Element.getOptionBody(name: String) = getOption(name)?.children?.firstOrNull()

fun TargetPlatform.createArguments(init: (CommonCompilerArguments).() -> Unit = {}): CommonCompilerArguments {
    return when {
        isCommon() -> K2MetadataCompilerArguments().apply { init() }
        isJvm() -> K2JVMCompilerArguments().apply {
            init()
            // TODO(dsavvinov): review this
            jvmTarget = (single() as? JdkPlatform)?.targetVersion?.description ?: JvmTarget.DEFAULT.description
        }
        isJs() -> K2JSCompilerArguments().apply { init() }
        isNative() -> FakeK2NativeCompilerArguments().apply { init() }
        else -> error("Unknown platform $this")
    }
}

private fun readV1Config(element: Element): KotlinFacetSettings {
    return KotlinFacetSettings().apply {
        val useProjectSettings = element.getOptionValue("useProjectSettings")?.toBoolean()

        val versionInfoElement = element.getOptionBody("versionInfo")
        val targetPlatformName = versionInfoElement?.getOptionValue("targetPlatformName")
        val languageLevel = versionInfoElement?.getOptionValue("languageLevel")
        val apiLevel = versionInfoElement?.getOptionValue("apiLevel")
        val targetPlatform = CommonPlatforms.allSimplePlatforms.union(setOf(CommonPlatforms.defaultCommonPlatform))
            .firstOrNull { it.oldFashionedDescription == targetPlatformName }
            ?: JvmIdePlatformKind.defaultPlatform // FIXME(dsavvinov): choose proper default

        val compilerInfoElement = element.getOptionBody("compilerInfo")

        val compilerSettings = CompilerSettings().apply {
            compilerInfoElement?.getOptionBody("compilerSettings")?.let { compilerSettingsElement ->
                XmlSerializer.deserializeInto(this, compilerSettingsElement)
            }
        }

        val commonArgumentsElement = compilerInfoElement?.getOptionBody("_commonCompilerArguments")
        val jvmArgumentsElement = compilerInfoElement?.getOptionBody("k2jvmCompilerArguments")
        val jsArgumentsElement = compilerInfoElement?.getOptionBody("k2jsCompilerArguments")

        val compilerArguments = targetPlatform.createArguments { freeArgs = arrayListOf() }

        commonArgumentsElement?.let { XmlSerializer.deserializeInto(compilerArguments, it) }
        when (compilerArguments) {
            is K2JVMCompilerArguments -> jvmArgumentsElement?.let { XmlSerializer.deserializeInto(compilerArguments, it) }
            is K2JSCompilerArguments -> jsArgumentsElement?.let { XmlSerializer.deserializeInto(compilerArguments, it) }
        }

        if (languageLevel != null) {
            compilerArguments.languageVersion = languageLevel
        }

        if (apiLevel != null) {
            compilerArguments.apiVersion = apiLevel
        }

        compilerArguments.detectVersionAutoAdvance()

        if (useProjectSettings != null) {
            this.useProjectSettings = useProjectSettings
        } else {
            // Migration problem workaround for pre-1.1-beta releases (mainly 1.0.6) -> 1.1-rc+
            // Problematic cases: 1.1-beta/1.1-beta2 -> 1.1-rc+ (useProjectSettings gets reset to false)
            // This heuristic detects old enough configurations:
            if (jvmArgumentsElement == null) {
                this.useProjectSettings = false
            }
        }

        this.compilerSettings = compilerSettings
        this.compilerArguments = compilerArguments
        this.targetPlatform = IdePlatformKind.platformByCompilerArguments(compilerArguments)
    }
}

// TODO: Introduce new version of facet serialization. See https://youtrack.jetbrains.com/issue/KT-38235
//  This is necessary to avoid having too much custom logic for platform serialization.
fun Element.getFacetPlatformByConfigurationElement(): TargetPlatform {
    getAttributeValue("allPlatforms").deserializeTargetPlatformByComponentPlatforms()?.let { return it }

    // failed to read list of all platforms. Fallback to legacy algorithm
    val platformName = getAttributeValue("platform") ?: return DefaultIdeTargetPlatformKindProvider.defaultPlatform

    return CommonPlatforms.allSimplePlatforms.firstOrNull {
        // first, look for exact match through all simple platforms
        it.oldFashionedDescription == platformName
    } ?: CommonPlatforms.defaultCommonPlatform.takeIf {
        // then, check exact match for the default common platform
        it.oldFashionedDescription == platformName
    } ?: NativePlatforms.unspecifiedNativePlatform.takeIf {
        // if none of the above succeeded, check if it's an old-style record about native platform (without suffix with target name)
        it.oldFashionedDescription.startsWith(platformName)
    }.orDefault() // finally, fallback to the default platform
}

private fun readV2AndLaterConfig(
    element: Element,
    argumentReader: (Element, CommonToolArguments) -> Unit = { el, arg -> XmlSerializer.deserializeInto(arg, el) }
): KotlinFacetSettings {
    return KotlinFacetSettings().apply {
        element.getAttributeValue("useProjectSettings")?.let { useProjectSettings = it.toBoolean() }
        val targetPlatform = element.getFacetPlatformByConfigurationElement()
        this.targetPlatform = targetPlatform
        readElementsList(element, "implements", "implement")?.let { implementedModuleNames = it }
        readElementsList(element, "dependsOnModuleNames", "dependsOn")?.let { dependsOnModuleNames = it }
        element.getChild("externalSystemTestTasks")?.let {
            val testRunTasks = it.getChildren("externalSystemTestTask")
                .mapNotNull { (it.content.firstOrNull() as? Text)?.textTrim }
                .mapNotNull { ExternalSystemTestRunTask.fromStringRepresentation(it) }
            val nativeMainRunTasks = it.getChildren("externalSystemNativeMainRunTask")
                .mapNotNull { (it.content.firstOrNull() as? Text)?.textTrim }
                .mapNotNull { ExternalSystemNativeMainRunTask.fromStringRepresentation(it) }

            externalSystemRunTasks = testRunTasks + nativeMainRunTasks
        }

        element.getChild("sourceSets")?.let {
            val items = it.getChildren("sourceSet")
            sourceSetNames = items.mapNotNull { (it.content.firstOrNull() as? Text)?.textTrim }
        }
        kind = element.getChild("newMppModelJpsModuleKind")?.let {
            val kindName = (it.content.firstOrNull() as? Text)?.textTrim
            if (kindName != null) {
                try {
                    KotlinModuleKind.valueOf(kindName)
                } catch (e: Exception) {
                    null
                }
            } else null
        } ?: KotlinModuleKind.DEFAULT
        isTestModule = element.getAttributeValue("isTestModule")?.toBoolean() ?: false
        externalProjectId = element.getAttributeValue("externalProjectId") ?: ""
        isHmppEnabled = element.getAttribute("isHmppProject")?.booleanValue ?: false
        pureKotlinSourceFolders = element.getAttributeValue("pureKotlinSourceFolders")?.split(";")?.toList() ?: emptyList()
        element.getChild("compilerSettings")?.let {
            compilerSettings = CompilerSettings()
            XmlSerializer.deserializeInto(compilerSettings!!, it)
        }
        element.getChild(COMPILER_ARGUMENTS_ELEMENT_NAME)?.let {
            compilerArguments = targetPlatform.createArguments {
                freeArgs = mutableListOf()
                internalArguments = mutableListOf()
            }
            argumentReader(it, compilerArguments!!)
            compilerArguments!!.detectVersionAutoAdvance()
        }
        productionOutputPath = element.getChild("productionOutputPath")?.let {
            PathUtil.toSystemDependentName((it.content.firstOrNull() as? Text)?.textTrim)
        } ?: (compilerArguments as? K2JSCompilerArguments)?.outputFile
        testOutputPath = element.getChild("testOutputPath")?.let {
            PathUtil.toSystemDependentName((it.content.firstOrNull() as? Text)?.textTrim)
        } ?: (compilerArguments as? K2JSCompilerArguments)?.outputFile
    }
}

private fun readElementsList(element: Element, rootElementName: String, elementName: String): List<String>? {
    element.getChild(rootElementName)?.let {
        val items = it.getChildren(elementName)
        return if (items.isNotEmpty()) {
            items.mapNotNull { (it.content.firstOrNull() as? Text)?.textTrim }
        } else {
            listOfNotNull((it.content.firstOrNull() as? Text)?.textTrim)
        }
    }
    return null
}

private fun readV2Config(element: Element): KotlinFacetSettings {
    return readV2AndLaterConfig(element)
}

private fun readLatestConfig(element: Element): KotlinFacetSettings {
    return readV2AndLaterConfig(element) { el, bean -> CompilerArgumentsDeserializerV5(bean).deserializeFrom(el) }
}

fun deserializeFacetSettings(element: Element): KotlinFacetSettings {
    val version = try {
        element.getAttribute("version")?.intValue
    } catch (e: DataConversionException) {
        null
    } ?: KotlinFacetSettings.DEFAULT_VERSION
    return when (version) {
        1 -> readV1Config(element)
        2, 3, 4 -> readV2Config(element)
        KotlinFacetSettings.CURRENT_VERSION -> readLatestConfig(element)
        else -> return KotlinFacetSettings() // Reset facet configuration if versions don't match
    }.apply { this.version = version }
}

fun CommonCompilerArguments.convertPathsToSystemIndependent() {
    pluginClasspaths?.forEachIndexed { index, s -> pluginClasspaths!![index] = PathUtil.toSystemIndependentName(s) }

    when (this) {
        is K2JVMCompilerArguments -> {
            destination = PathUtil.toSystemIndependentName(destination)
            classpath = PathUtil.toSystemIndependentName(classpath)
            jdkHome = PathUtil.toSystemIndependentName(jdkHome)
            kotlinHome = PathUtil.toSystemIndependentName(kotlinHome)
            friendPaths?.forEachIndexed { index, s -> friendPaths!![index] = PathUtil.toSystemIndependentName(s) }
            declarationsOutputPath = PathUtil.toSystemIndependentName(declarationsOutputPath)
        }

        is K2JSCompilerArguments -> {
            outputFile = PathUtil.toSystemIndependentName(outputFile)
            libraries = PathUtil.toSystemIndependentName(libraries)
        }

        is K2MetadataCompilerArguments -> {
            destination = PathUtil.toSystemIndependentName(destination)
            classpath = PathUtil.toSystemIndependentName(classpath)
        }
    }
}

fun CompilerSettings.convertPathsToSystemIndependent() {
    scriptTemplatesClasspath = PathUtil.toSystemIndependentName(scriptTemplatesClasspath)
    outputDirectoryForJsLibraryFiles = PathUtil.toSystemIndependentName(outputDirectoryForJsLibraryFiles)
}

private fun KClass<*>.superClass() = superclasses.firstOrNull { !it.java.isInterface }

private fun Class<*>.computeNormalPropertyOrdering(): Map<String, Int> {
    val result = LinkedHashMap<String, Int>()
    var count = 0
    generateSequence(this) { it.superclass }.forEach { clazz ->
        for (field in clazz.declaredFields) {
            if (field.modifiers and Modifier.STATIC != 0) continue

            var propertyName = field.name
            if (propertyName.endsWith(JvmAbi.DELEGATED_PROPERTY_NAME_SUFFIX)) {
                propertyName = propertyName.dropLast(JvmAbi.DELEGATED_PROPERTY_NAME_SUFFIX.length)
            }

            result[propertyName] = count++
        }
    }
    return result
}

private val allNormalOrderings = HashMap<Class<*>, Map<String, Int>>()

private val Class<*>.normalOrdering
    get() = synchronized(allNormalOrderings) { allNormalOrderings.getOrPut(this) { computeNormalPropertyOrdering() } }

// Replacing fields with delegated properties leads to unexpected reordering of entries in facet configuration XML
// It happens due to XmlSerializer using different orderings for field- and method-based accessors
// This code restores the original ordering
internal fun Element.restoreNormalOrdering(bean: Any) {
    val normalOrdering = bean.javaClass.normalOrdering
    val elementsToReorder = this.getContent<Element> { it is Element && it.getAttribute("name")?.value in normalOrdering }
    elementsToReorder.sortedBy { normalOrdering[it.getAttribute("name")?.value!!] }
        .forEachIndexed { index, element -> elementsToReorder[index] = element.clone() }
}

private fun buildChildElement(element: Element, tag: String, bean: Any, filter: SerializationFilter): Element {
    return Element(tag).apply {
        XmlSerializer.serializeInto(bean, this, filter)
        restoreNormalOrdering(bean)
        element.addContent(this)
    }
}

private fun KotlinFacetSettings.writeConfig(element: Element) {
    val filter = SkipDefaultsSerializationFilter()

    // TODO: Introduce new version of facet serialization. See https://youtrack.jetbrains.com/issue/KT-38235
    //  This is necessary to avoid having too much custom logic for platform serialization.
    targetPlatform?.let { targetPlatform ->
        element.setAttribute("platform", targetPlatform.oldFashionedDescription)
        element.setAttribute("allPlatforms", targetPlatform.serializeComponentPlatforms())
    }
    if (!useProjectSettings) {
        element.setAttribute("useProjectSettings", useProjectSettings.toString())
    }
    saveElementsList(element, implementedModuleNames, "implements", "implement")
    saveElementsList(element, dependsOnModuleNames, "dependsOnModuleNames", "dependsOn")

    if (sourceSetNames.isNotEmpty()) {
        element.addContent(
            Element("sourceSets").apply {
                sourceSetNames.map { addContent(Element("sourceSet").apply { addContent(it) }) }
            }
        )
    }
    if (kind != KotlinModuleKind.DEFAULT) {
        element.addContent(Element("newMppModelJpsModuleKind").apply { addContent(kind.name) })
        element.setAttribute("isTestModule", isTestModule.toString())
    }
    if (externalProjectId.isNotEmpty()) {
        element.setAttribute("externalProjectId", externalProjectId)
    }
    if (mppVersion.isHmpp) {
        element.setAttribute("isHmppProject", mppVersion.isHmpp.toString())
    }
    if (externalSystemRunTasks.isNotEmpty()) {
        element.addContent(
            Element("externalSystemTestTasks").apply {
                externalSystemRunTasks.forEach { task ->
                    when(task) {
                        is ExternalSystemTestRunTask -> {
                            addContent(
                                Element("externalSystemTestTask").apply { addContent(task.toStringRepresentation()) }
                            )
                        }
                        is ExternalSystemNativeMainRunTask -> {
                            addContent(
                                Element("externalSystemNativeMainRunTask").apply { addContent(task.toStringRepresentation()) }
                            )
                        }
                    }
                }
            }
        )
    }
    if (pureKotlinSourceFolders.isNotEmpty()) {
        element.setAttribute("pureKotlinSourceFolders", pureKotlinSourceFolders.joinToString(";"))
    }
    productionOutputPath?.let {
        if (it != (compilerArguments as? K2JSCompilerArguments)?.outputFile) {
            element.addContent(Element("productionOutputPath").apply { addContent(PathUtil.toSystemIndependentName(it)) })
        }
    }
    testOutputPath?.let {
        if (it != (compilerArguments as? K2JSCompilerArguments)?.outputFile) {
            element.addContent(Element("testOutputPath").apply { addContent(PathUtil.toSystemIndependentName(it)) })
        }
    }
    compilerSettings?.let { copyBean(it) }?.let {
        it.convertPathsToSystemIndependent()
        buildChildElement(element, "compilerSettings", it, filter)
    }
}

private fun KotlinFacetSettings.writeV2toV4Config(element: Element) = writeConfig(element).apply {
    compilerArguments?.let { copyBean(it) }?.let {
        it.convertPathsToSystemIndependent()
        val compilerArgumentsXml = buildChildElement(element, "compilerArguments", it, SkipDefaultsSerializationFilter())
        compilerArgumentsXml.dropVersionsIfNecessary(it)
    }
}

private fun KotlinFacetSettings.writeLatestConfig(element: Element) = writeConfig(element).apply {
    compilerArguments?.let { copyBean(it) }?.let {
        it.convertPathsToSystemIndependent()
        val compilerArgumentsXml = CompilerArgumentsSerializerV5(it).serializeTo(element)
        compilerArgumentsXml.dropVersionsIfNecessary(it)
    }
}

private fun saveElementsList(element: Element, elementsList: List<String>, rootElementName: String, elementName: String) {
    if (elementsList.isNotEmpty()) {
        element.addContent(
            Element(rootElementName).apply {
                val singleModule = elementsList.singleOrNull()
                if (singleModule != null) {
                    addContent(singleModule)
                } else {
                    elementsList.map { addContent(Element(elementName).apply { addContent(it) }) }
                }
            }
        )
    }
}

fun CommonCompilerArguments.detectVersionAutoAdvance() {
    autoAdvanceLanguageVersion = languageVersion == null
    autoAdvanceApiVersion = apiVersion == null
}

fun Element.dropVersionsIfNecessary(settings: CommonCompilerArguments) {
    // Do not serialize language/api version if they correspond to the default language version
    if (settings.autoAdvanceLanguageVersion) {
        getOption("languageVersion")?.detach()
    }

    if (settings.autoAdvanceApiVersion) {
        getOption("apiVersion")?.detach()
    }
}

fun KotlinFacetSettings.serializeFacetSettings(element: Element) = when (version) {
    2, 3, 4 -> {
        element.setAttribute("version", version.toString())
        writeV2toV4Config(element)
    }
    else -> {
        element.setAttribute("version", KotlinFacetSettings.CURRENT_VERSION.toString())
        writeLatestConfig(element)
    }
}


private fun TargetPlatform.serializeComponentPlatforms(): String {
    val componentPlatforms = componentPlatforms
    val componentPlatformNames = componentPlatforms.mapTo(ArrayList()) { it.serializeToString() }

    // workaround for old Kotlin IDE plugins, KT-38634
    if (componentPlatforms.any { it is NativePlatform })
        componentPlatformNames.add(NativePlatformUnspecifiedTarget.legacySerializeToString())

    return componentPlatformNames.sorted().joinToString("/")
}

private fun String?.deserializeTargetPlatformByComponentPlatforms(): TargetPlatform? {
    val componentPlatformNames = this?.split('/')?.toSet()?.takeIf { it.isNotEmpty() } ?: return null

    val knownComponentPlatforms = HashMap<String, SimplePlatform>() // "serialization presentation" to "simple platform name"

    // first, collect serialization presentations for every known simple platform
    CommonPlatforms.allSimplePlatforms
        .flatMap { it.componentPlatforms }
        .forEach { knownComponentPlatforms[it.serializeToString()] = it }

    // next, add legacy aliases for some of the simple platforms; ex: unspecifiedNativePlatform
    NativePlatformUnspecifiedTarget.let { knownComponentPlatforms[it.legacySerializeToString()] = it }

    val componentPlatforms = componentPlatformNames.mapNotNull(knownComponentPlatforms::get).toSet()
    return when (componentPlatforms.size) {
        0 -> {
            // empty set of component platforms is not allowed, in such case fallback to legacy algorithm
            null
        }
        1 -> TargetPlatform(componentPlatforms)
        else -> {
            // workaround for old Kotlin IDE plugins, KT-38634
            if (componentPlatforms.any { it is NativePlatformUnspecifiedTarget }
                && componentPlatforms.any { it is NativePlatformWithTarget }
            ) {
                TargetPlatform(componentPlatforms - NativePlatformUnspecifiedTarget)
            } else {
                TargetPlatform(componentPlatforms)
            }
        }
    }
}
