package org.jetbrains.kotlin.r4a.frames.analysis

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.reportFromPlugin
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.r4a.R4AFlags
import org.jetbrains.kotlin.r4a.analysis.R4ADefaultErrorMessages
import org.jetbrains.kotlin.r4a.analysis.R4AErrors
import org.jetbrains.kotlin.r4a.frames.FrameRecordClassDescriptor
import org.jetbrains.kotlin.r4a.frames.SyntheticFramePackageDescriptor
import org.jetbrains.kotlin.r4a.frames.abstractRecordClassName
import org.jetbrains.kotlin.r4a.frames.findTopLevel
import org.jetbrains.kotlin.r4a.frames.modelClassName
import org.jetbrains.kotlin.r4a.frames.recordClassName
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.types.typeUtil.isAnyOrNullableAny
import org.jetbrains.kotlin.types.typeUtil.isInterface

class FramePackageAnalysisHandlerExtension : AnalysisHandlerExtension {
    companion object {
        fun doAnalysis(
            module: ModuleDescriptor,
            bindingTrace: BindingTrace,
            files: Collection<KtFile>,
            resolveSession: ResolveSession
        ) {
            if (!R4AFlags.FRAMED_MODEL_CLASSES) return
            for (file in files) {
                for (declaration in file.declarations) {
                    val ktClass = declaration as? KtClassOrObject ?: continue
                    val framedDescriptor = resolveSession.resolveToDescriptor(declaration) as?
                            ClassDescriptor ?: continue
                    if (!framedDescriptor.hasModelAnnotation()) continue

                    val ktType = framedDescriptor.defaultType

                    // Can only place an @Model on an object that doesn't inherit from another object
                    val baseTypes = ktType.constructor.supertypes.filter {
                        !it.isInterface() && !it.isAnyOrNullableAny()
                    }
                    if (baseTypes.isNotEmpty())
                        bindingTrace.reportFromPlugin(
                            R4AErrors.UNSUPPORTED_MODEL_INHERITANCE.on(
                                ktClass.nameIdentifier ?: ktClass
                            ),
                            R4ADefaultErrorMessages
                        )

                    val classFqName = ktClass.fqName!!
                    val recordFqName = classFqName.parent().child(Name.identifier(
                        "${classFqName.shortName()}\$Record")
                    )
                    val recordSimpleName = recordFqName.shortName()
                    val recordPackage =
                        SyntheticFramePackageDescriptor(module, recordFqName.parent())
                    val baseTypeDescriptor = module.findTopLevel(abstractRecordClassName)
                    val recordDescriptor = module.findTopLevel(recordClassName)
                    val baseType = baseTypeDescriptor.defaultType
                    val frameClass = FrameRecordClassDescriptor(
                        recordSimpleName,
                        recordPackage,
                        recordDescriptor,
                        framedDescriptor,
                        listOf(baseType),
                        bindingTrace.bindingContext
                    )

                    recordPackage.setClassDescriptor(frameClass)
                    bindingTrace.record(FrameWritableSlices.RECORD_CLASS, classFqName, frameClass)
                    bindingTrace.record(
                        FrameWritableSlices.FRAMED_DESCRIPTOR,
                        classFqName,
                        framedDescriptor
                    )
                }
            }
        }
    }

    override fun doAnalysis(
        project: Project,
        module: ModuleDescriptor,
        projectContext: ProjectContext,
        files: Collection<KtFile>,
        bindingTrace: BindingTrace,
        componentProvider: ComponentProvider
    ): AnalysisResult? {
        val resolveSession = componentProvider.get<ResolveSession>()
        doAnalysis(module, bindingTrace, files, resolveSession)
        return null
    }
}

private fun Annotated.hasModelAnnotation() = annotations.findAnnotation(modelClassName) != null