package org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem

import org.jetbrains.kotlin.tools.projectWizard.GeneratedIdentificator
import org.jetbrains.kotlin.tools.projectWizard.Identificator
import org.jetbrains.kotlin.tools.projectWizard.IdentificatorOwner
import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.entity.*
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.*
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.templates.Template

@Suppress("EnumEntryName")
enum class ModuleKind {
    multiplatform,
    target,
    singleplatform
}

// TODO separate to classes
class Module(
    var name: String,
    val kind: ModuleKind,
    var configurator: ModuleConfigurator,
    var template: Template?,
    val sourcesets: List<Sourceset>,
    subModules: List<Module>,
    var parent: Module? = null,
    override val identificator: Identificator = GeneratedIdentificator(name)
) : DisplayableSettingItem, Validatable<Module>, IdentificatorOwner {

    override val validator: SettingValidator<Module> = settingValidator<Module> { module ->
        StringValidators.shouldNotBeBlank("Module name").validate(this, module.name)
    } and settingValidator { module ->
        StringValidators.shouldBeValidIdentifier("Module name `$name`").validate(this, module.name)
    } and settingValidator { module ->
        withSettingsOf(module) {
            configurator.settings.map { setting ->
                val value = setting.reference.notRequiredSettingValue
                    ?: return@map ValidationResult.ValidationError("${setting.title.capitalize()} should not be blank")
                setting.validator.validate(this@settingValidator, value)
            }.fold()
        }
    } and settingValidator { module ->
        val template = module.template ?: return@settingValidator ValidationResult.OK
        org.jetbrains.kotlin.tools.projectWizard.templates.withSettingsOf(module) {
            template.settings.map { setting ->
                val value = setting.reference.notRequiredSettingValue
                    ?: return@map ValidationResult.ValidationError("${setting.title.capitalize()} should not be blank")
                setting.validator.validate(this@settingValidator, value)
            }.fold()
        }
    } and inValidatorContext { module ->
        validateList(module.subModules)
    }

    var subModules = subModules
        set(value) {
            field = value
            value.forEach { it.parent = this }
        }

    init {
        subModules.forEach { it.parent = this }
        sourcesets.forEach { it.parent = this }
    }


    override val text: String get() = name
    override val greyText: String?
        get() = when {
            kind == ModuleKind.target -> "${configurator.text} Target"
            configurator == MppModuleConfigurator -> "MPP Module"
            configurator == AndroidSinglePlatformModuleConfigurator -> "Android Module"
            configurator == IOSSinglePlatformModuleConfigurator -> "IOS Module"
            else -> "Module"
        }

    companion object {
        val parser: Parser<Module> = mapParser { map, path ->
            val (name) = map.parseValue<String>(path, "name")
            val identificator = GeneratedIdentificator(name)
            val (kind) = map.parseValue<ModuleKind>(this, path, "kind", enumParser())
            val (configurator) = map.parseValue(this, path, "type", ModuleConfigurator.getParser(identificator))
            val template = map["template"]?.let {
                Template.parser(identificator).parse(this, it, "$path.template")
            }.nullableValue()
            val (sourcesets) = map.parseValue(
                this,
                path,
                "sourcesets",
                listParser(Sourceset.parser(configurator.moduleType))
            ) { emptyList() }
            val (submodules) = map.parseValue(this, path, "subModules", listParser(Module.parser)) { emptyList() }
            Module(name, kind, configurator, template, sourcesets, submodules, identificator = identificator)
        }
    }

}

val Module.mainSourceset: Sourceset?
    get() = sourcesets.firstOrNull { it.sourcesetType == SourcesetType.main }

val Module.testSourceset: Sourceset?
    get() = sourcesets.firstOrNull { it.sourcesetType == SourcesetType.test }


val Module.path
    get() = generateSequence(this, Module::parent)
        .map { it.name }
        .toList()
        .asReversed()
        .let(::ModulePath)

val Sourceset.path
    get() = ModulePath(parent?.path?.parts.orEmpty() + sourcesetType.name)

val Module.isRootModule
    get() = parent == null

@Suppress("FunctionName")
fun MultiplatformTargetModule(name: String, configurator: ModuleConfigurator, sourcesets: List<Sourceset>) =
    Module(
        name,
        ModuleKind.target,
        configurator,
        null,
        sourcesets,
        emptyList()
    )

@Suppress("FunctionName")
fun MultiplatformModule(name: String, targets: List<Module> = emptyList()) =
    Module(
        name,
        ModuleKind.multiplatform,
        MppModuleConfigurator,
        null,
        emptyList(),
        targets
    )

@Suppress("FunctionName")
fun SingleplatformModule(name: String, sourcesets: List<Sourceset>) =
    Module(
        name,
        ModuleKind.singleplatform,
        JvmSinglePlatformModuleConfigurator,
        null,
        sourcesets,
        emptyList()
    )