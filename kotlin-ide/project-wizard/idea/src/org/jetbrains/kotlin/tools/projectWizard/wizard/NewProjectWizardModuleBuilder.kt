package org.jetbrains.kotlin.tools.projectWizard.wizard

import com.intellij.ide.RecentProjectsManager
import com.intellij.ide.actions.NewProjectAction
import com.intellij.ide.impl.NewProjectUtil
import com.intellij.ide.util.projectWizard.*
import com.intellij.ide.wizard.AbstractWizard
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.module.ModifiableModuleModel
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import com.intellij.openapi.ui.Messages
import com.intellij.util.SystemProperties
import org.jetbrains.kotlin.idea.framework.KotlinTemplatesFactory
import org.jetbrains.kotlin.idea.projectWizard.WizardStatsService
import org.jetbrains.kotlin.idea.projectWizard.WizardStatsService.UiEditorUsageStats
import org.jetbrains.kotlin.idea.projectWizard.WizardStatsService.ProjectCreationStats
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.tools.projectWizard.core.buildList
import org.jetbrains.kotlin.tools.projectWizard.core.div
import org.jetbrains.kotlin.tools.projectWizard.core.entity.StringValidators
import org.jetbrains.kotlin.tools.projectWizard.core.entity.ValidationResult
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.reference
import org.jetbrains.kotlin.tools.projectWizard.core.isSuccess
import org.jetbrains.kotlin.tools.projectWizard.core.onFailure
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.Plugins
import org.jetbrains.kotlin.tools.projectWizard.plugins.StructurePlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemType
import org.jetbrains.kotlin.tools.projectWizard.projectTemplates.ProjectTemplate
import org.jetbrains.kotlin.tools.projectWizard.wizard.service.IdeaJpsWizardService
import org.jetbrains.kotlin.tools.projectWizard.wizard.service.IdeaServices
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.asHtml
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.firstStep.FirstWizardStepComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.runWithProgressBar
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep.SecondStepWizardComponent
import java.io.File
import javax.swing.JButton
import javax.swing.JComponent
import com.intellij.openapi.module.Module as IdeaModule

/*
Have to override EmptyModuleBuilder here instead of just ModuleBuilder
As EmptyModuleBuilder has not expert panel option which are redundant
 */
class NewProjectWizardModuleBuilder : EmptyModuleBuilder() {
    private val wizard = IdeWizard(Plugins.allPlugins, IdeaServices.PROJECT_INDEPENDENT, isUnitTestMode = false)
    private val uiEditorUsagesStats = UiEditorUsageStats()

    override fun isOpenProjectSettingsAfter(): Boolean = false
    override fun canCreateModule(): Boolean = false
    override fun getPresentableName(): String = moduleType.name
    override fun getDescription(): String? = moduleType.description
    override fun getGroupName(): String? = moduleType.name
    override fun isTemplateBased(): Boolean = false

    companion object {
        const val MODULE_BUILDER_ID = "kotlin.newProjectWizard.builder"
        private val projectNameValidator = StringValidators.shouldBeValidIdentifier("Project name", setOf('-', '_'))
        private const val INVALID_PROJECT_NAME_MESSAGE = "Invalid project name"
    }

    override fun isAvailable(): Boolean = isCreatingNewProject()

    private var wizardContext: WizardContext? = null
    private var finishButtonClicked: Boolean = false

    override fun getModuleType(): ModuleType<*> = NewProjectWizardModuleType()
    override fun getParentGroup(): String = KotlinTemplatesFactory.KOTLIN_PARENT_GROUP_NAME

    override fun createWizardSteps(
        wizardContext: WizardContext,
        modulesProvider: ModulesProvider
    ): Array<ModuleWizardStep> {
        this.wizardContext = wizardContext
        return arrayOf(ModuleNewWizardSecondStep(wizard, uiEditorUsagesStats, wizardContext))
    }

    override fun commit(
        project: Project,
        model: ModifiableModuleModel?,
        modulesProvider: ModulesProvider?
    ): List<IdeaModule>? {
        runWriteAction {
            wizard.jdk?.let { jdk -> NewProjectUtil.applyJdkToProject(project, jdk) }
        }
        val modulesModel = model ?: ModuleManager.getInstance(project).modifiableModel
        val success = wizard.apply(
            services = buildList {
                +IdeaServices.createScopeDependent(project)
                +IdeaServices.PROJECT_INDEPENDENT
                +IdeaJpsWizardService(project, modulesModel, this@NewProjectWizardModuleBuilder, wizard)
            },
            phases = GenerationPhase.startingFrom(GenerationPhase.FIRST_STEP)
        ).onFailure { errors ->
            val errorMessages = errors.joinToString(separator = "\n") { it.message }
            Messages.showErrorDialog(project, errorMessages, KotlinNewProjectWizardUIBundle.message("error.generation"))
        }.isSuccess
        if (success) {
            val projectCreationStats = ProjectCreationStats(
                KotlinTemplatesFactory.KOTLIN_GROUP_NAME,
                wizard.projectTemplate!!.id,
                wizard.buildSystemType!!.id
            )
            WizardStatsService.logDataOnProjectGenerated(
                projectCreationStats,
                uiEditorUsagesStats
            )
        }
        return when {
            !success -> null
            wizard.buildSystemType == BuildSystemType.Jps -> runWriteAction {
                modulesModel.modules.toList().onEach { setupModule(it) }
            }
            else -> emptyList()
        }
    }

    private fun clickFinishButton() {
        if (finishButtonClicked) return
        finishButtonClicked = true
        wizardContext?.getNextButton()?.doClick()
    }

    override fun modifySettingsStep(settingsStep: SettingsStep): ModuleWizardStep? {
        settingsStep.moduleNameLocationSettings?.apply {
            moduleName = wizard.projectName!!
            moduleContentRoot = wizard.projectPath!!.toString()
        }
        clickFinishButton()
        return null
    }

    override fun validateModuleName(moduleName: String): Boolean {
        when (val validationResult = wizard.context.read {
            projectNameValidator.validate(this, moduleName)
        }) {
            ValidationResult.OK -> return true
            is ValidationResult.ValidationError -> {
                val message = validationResult.messages.firstOrNull() ?: INVALID_PROJECT_NAME_MESSAGE
                throw ConfigurationException(message, INVALID_PROJECT_NAME_MESSAGE)
            }
        }
    }

    internal fun selectProjectTemplate(template: ProjectTemplate) {
        wizard.buildSystemType = BuildSystemType.GradleKotlinDsl
        wizard.projectTemplate = template
    }

    private val firstStep = ModuleNewWizardFirstStep(wizard)

    override fun getCustomOptionsStep(context: WizardContext?, parentDisposable: Disposable?) =
        firstStep
}

abstract class WizardStep(protected val wizard: IdeWizard, private val phase: GenerationPhase) : ModuleWizardStep() {
    override fun updateDataModel() = Unit // model is updated on every UI action
    override fun validate(): Boolean =
        when (val result = wizard.context.read { with(wizard) { validate(setOf(phase)) } }) {
            ValidationResult.OK -> true
            is ValidationResult.ValidationError -> {
                handleErrors(result)
                false
            }
        }

    protected open fun handleErrors(error: ValidationResult.ValidationError) {
        throw ConfigurationException(error.asHtml(), "Validation Error")
    }
}

class ModuleNewWizardFirstStep(wizard: IdeWizard) : WizardStep(wizard, GenerationPhase.FIRST_STEP) {
    private val component = FirstWizardStepComponent(wizard)
    override fun getComponent(): JComponent = component.component

    init {
        runPreparePhase()
        initDefaultValues()
        component.onInit()
    }

    private fun runPreparePhase() = runWithProgressBar(title = "") {
        wizard.apply(emptyList(), setOf(GenerationPhase.PREPARE)) { task ->
            ProgressManager.getInstance().progressIndicator.text = task.title ?: ""
        }
    }

    override fun handleErrors(error: ValidationResult.ValidationError) {
        component.navigateTo(error)
    }

    private fun initDefaultValues() {
        val suggestedProjectParentLocation = suggestProjectLocation()
        val suggestedProjectName = ProjectWizardUtil.findNonExistingFileName(suggestedProjectParentLocation, "untitled", "")
        wizard.context.writeSettings {
            StructurePlugin::name.reference.setValue(suggestedProjectName)
            StructurePlugin::projectPath.reference.setValue(suggestedProjectParentLocation / suggestedProjectName)
            StructurePlugin::artifactId.reference.setValue(suggestedProjectName)

            if (StructurePlugin::groupId.reference.notRequiredSettingValue == null) {
                StructurePlugin::groupId.reference.setValue(suggestGroupId())
            }
        }
    }

    private fun suggestGroupId(): String {
        val username = SystemProperties.getUserName() ?: return DEFAULT_GROUP_ID
        if (!username.matches("[\\w\\s]+".toRegex())) return DEFAULT_GROUP_ID
        val usernameAsGroupId = username.trim().toLowerCase().split("\\s+".toRegex()).joinToString(separator = ".")
        return "me.$usernameAsGroupId"
    }

    // copied from com.intellij.ide.util.projectWizard.WizardContext.getProjectFileDirectory
    private fun suggestProjectLocation(): String {
        val lastProjectLocation = RecentProjectsManager.getInstance().lastProjectCreationLocation
        if (lastProjectLocation != null) {
            return lastProjectLocation.replace('/', File.separatorChar)
        }
        val userHome = SystemProperties.getUserHome()
        val productName = ApplicationNamesInfo.getInstance().lowercaseProductName
        return userHome.replace('/', File.separatorChar) + File.separator + productName.replace(" ", "") + "Projects"
    }

    companion object {
        private const val DEFAULT_GROUP_ID = "me.user"
    }
}

class ModuleNewWizardSecondStep(
    wizard: IdeWizard,
    uiEditorUsagesStats: UiEditorUsageStats,
    private val wizardContext: WizardContext
) : WizardStep(wizard, GenerationPhase.SECOND_STEP) {
    private val component = SecondStepWizardComponent(wizard, uiEditorUsagesStats)
    override fun getComponent(): JComponent = component.component

    override fun _init() {
        component.onInit()
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        wizardContext.getNextButton()?.text = "Finish"
        return super.getPreferredFocusedComponent()
    }

    override fun handleErrors(error: ValidationResult.ValidationError) {
        component.navigateTo(error)
    }
}

private fun isCreatingNewProject() = Thread.currentThread().stackTrace.any { element ->
    element.className == NewProjectAction::class.java.name
}

private fun WizardContext.getNextButton() = try {
    AbstractWizard::class.java.getDeclaredMethod("getNextButton")
        .also { it.isAccessible = true }
        .invoke(wizard) as? JButton
} catch (_: Throwable) {
    null
}
