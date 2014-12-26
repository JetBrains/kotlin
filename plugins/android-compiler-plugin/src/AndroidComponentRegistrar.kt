/*
 * Copyright 2010-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.android

import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.jet.config.CompilerConfiguration
import com.intellij.mock.MockProject
import org.jetbrains.jet.config.CompilerConfigurationKey
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.jet.extensions.ExternalDeclarationsProvider
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.resolve.android.*
import com.intellij.openapi.components.ServiceManager
import org.jetbrains.jet.utils.emptyOrSingletonList
import com.intellij.openapi.project.Project
import org.jetbrains.jet.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall
import org.jetbrains.jet.codegen.StackValue
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.jet.lang.resolve.DescriptorToSourceUtils
import org.jetbrains.jet.codegen.ClassBuilder
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.jet.lang.resolve.java.diagnostics.JvmDeclarationOrigin
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.jet.lang.psi.JetClassOrObject
import org.jetbrains.jet.codegen.FunctionCodegen
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.jet.lang.psi.JetClass
import org.jetbrains.jet.lang.psi.JetClassBody
import org.jetbrains.jet.lang.resolve.lazy.descriptors.LazyClassDescriptor
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor
import org.jetbrains.jet.lang.resolve.java.lazy.descriptors.LazyJavaClassDescriptor
import org.jetbrains.jet.lang.resolve.DescriptorUtils

public object AndroidConfigurationKeys {

    public val ANDROID_RES_PATH: CompilerConfigurationKey<String> = CompilerConfigurationKey.create<String>("android resources search path")

    public val ANDROID_MANIFEST: CompilerConfigurationKey<String> = CompilerConfigurationKey.create<String>("android manifest file")
}

public class AndroidCommandLineProcessor : CommandLineProcessor {
    class object {
        public val ANDROID_COMPILER_PLUGIN_ID: String = "org.jetbrains.kotlin.android"

        public val RESOURCE_PATH_OPTION: CliOption = CliOption("androidRes", "<path>", "Android resources path")
        public val MANIFEST_FILE_OPTION: CliOption = CliOption("androidManifest", "<path>", "Android manifest file")
    }

    override val pluginId: String = ANDROID_COMPILER_PLUGIN_ID

    override val pluginOptions: Collection<CliOption> = listOf(RESOURCE_PATH_OPTION, MANIFEST_FILE_OPTION)

    override fun processOption(option: CliOption, value: String, configuration: CompilerConfiguration) {
        when (option) {
            RESOURCE_PATH_OPTION -> configuration.put(AndroidConfigurationKeys.ANDROID_RES_PATH, value)
            MANIFEST_FILE_OPTION -> configuration.put(AndroidConfigurationKeys.ANDROID_MANIFEST, value)
            else -> throw CliOptionProcessingException("Unknown option: ${option.name}")
        }
    }
}

public class AndroidDeclarationsProvider(private val project: Project) : ExternalDeclarationsProvider {
    override fun getExternalDeclarations(): Collection<JetFile> {
        val parser = ServiceManager.getService<AndroidUIXmlProcessor>(project, javaClass<AndroidUIXmlProcessor>())
        return emptyOrSingletonList(parser?.parseToPsi(project))
    }
}

public class AndroidExpressionCodegen : ExpressionCodegenExtension {
    val propertyName = "_\$_findViewByIdCache"
    val methodName = "_\$_findCachedViewById"

    override fun apply(receiver: StackValue, resolvedCall: ResolvedCall<*>, c: ExpressionCodegenExtension.Context): StackValue? {
        if (resolvedCall.getResultingDescriptor() !is PropertyDescriptor) return null

        val propertyDescriptor = resolvedCall.getResultingDescriptor() as PropertyDescriptor

        val file = DescriptorToSourceUtils.getContainingFile(propertyDescriptor)
        if (file == null) return null

        val androidPackage = file.getUserData<String>(AndroidConst.ANDROID_USER_PACKAGE)
        if (androidPackage == null) return null

        val className = DescriptorUtils.getFqName(
                resolvedCall.getExtensionReceiver().getType().getConstructor().getDeclarationDescriptor()).toString()
        val bytecodeClassName = className.replace('.', '/')

        val retType = c.typeMapper.mapType(propertyDescriptor.getReturnType()!!)
        receiver.put(Type.getType("L$bytecodeClassName;"), c.v)
        c.v.getstatic(androidPackage.replace(".", "/") + "/R\$id", propertyDescriptor.getName().asString(), "I")
        c.v.invokevirtual(bytecodeClassName, methodName, "(I)Landroid/view/View;", false)
        c.v.checkcast(retType)

        return StackValue.onStack(retType)
    }

    private fun isClassSupported(descriptor: ClassifierDescriptor): Boolean {
        fun classNameSupported(name: String): Boolean = when (name) {
            "android.app.Activity" -> true
            else -> false
        }

        if (descriptor is LazyJavaClassDescriptor) {
            if (classNameSupported(descriptor.fqName.asString()))
                return true
        } else if (descriptor is LazyClassDescriptor) { // For tests (FakeActivity)
            if (classNameSupported(DescriptorUtils.getFqName(descriptor).toString()))
                return true
        }

        return descriptor.getTypeConstructor().getSupertypes().any {
            isClassSupported(it.getConstructor().getDeclarationDescriptor())
        }
    }

    override fun generateClassSyntheticParts(codegen: ClassBuilder, clazz: JetClassOrObject, descriptor: DeclarationDescriptor) {
        if (clazz !is JetClass || (clazz.getParent() is JetClassBody) || descriptor !is LazyClassDescriptor) return

        // Do not generate anything if class is not supported
        if (clazz.isEnum() || clazz.isTrait() || clazz.isAnnotation() || clazz.isInner() || !isClassSupported(descriptor))
            return

        val className = clazz.getFqName().toString().replace('.', '/')

        val classType = Type.getType(className)
        val viewType = Type.getType("Landroid/view/View;")

        codegen.newField(JvmDeclarationOrigin.NO_ORIGIN, ACC_PRIVATE, propertyName, "Ljava/util/HashMap;", null, null)

        val methodVisitor = codegen.newMethod(
                JvmDeclarationOrigin.NO_ORIGIN, ACC_PUBLIC, methodName, "(I)Landroid/view/View;", null, null)
        methodVisitor.visitCode()
        val iv = InstructionAdapter(methodVisitor)

        fun getCache() {
            iv.load(0, classType)
            iv.getfield(className, propertyName, "Ljava/util/HashMap;")
        }

        fun getId() = iv.load(1, Type.INT_TYPE)

        // Get cache property
        iv.visitLabel(Label())
        getCache()

        val lCacheIsNull = Label()
        val lCacheNonNull = Label()
        iv.ifnonnull(lCacheNonNull)

        // Init cache if null
        iv.visitLabel(lCacheIsNull)
        iv.load(0, classType)
        iv.anew(Type.getType("Ljava/util/HashMap;"))
        iv.dup()
        iv.invokespecial("java/util/HashMap", "<init>", "()V", false)
        iv.putfield(className, propertyName, "Ljava/util/HashMap;")

        // Get View from cache
        iv.visitLabel(lCacheNonNull)
        getCache()
        getId()
        iv.invokestatic("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false)
        iv.invokevirtual("java/util/HashMap", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", false)
        iv.checkcast(viewType)
        iv.store(2, viewType)

        val lViewIsNull = Label()
        val lViewNonNull = Label()
        iv.load(2, viewType)
        iv.ifnonnull(lViewNonNull)

        // Resolve View via findViewById if not in cache
        iv.visitLabel(lViewIsNull)
        iv.load(0, classType)
        getId()
        iv.invokevirtual(className, "findViewById", "(I)Landroid/view/View;", false)
        iv.store(2, viewType)

        // Store resolved View in cache
        getCache()
        getId()
        iv.invokestatic("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false)
        iv.load(2, viewType)
        iv.invokevirtual("java/util/HashMap", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false)
        iv.pop()

        iv.visitLabel(lViewNonNull)
        iv.load(2, viewType)
        iv.areturn(viewType)

        FunctionCodegen.endVisit(methodVisitor, methodName, clazz)
    }
}

public class AndroidComponentRegistrar : ComponentRegistrar {

    public override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        val androidResPath = configuration.get(AndroidConfigurationKeys.ANDROID_RES_PATH)
        val androidManifest = configuration.get(AndroidConfigurationKeys.ANDROID_MANIFEST)
        project.registerService(javaClass<AndroidUIXmlProcessor>(), CliAndroidUIXmlProcessor(project, androidResPath, androidManifest))

        ExternalDeclarationsProvider.registerExtension(project, AndroidDeclarationsProvider(project))
        ExpressionCodegenExtension.registerExtension(project, AndroidExpressionCodegen())
    }
}