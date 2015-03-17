/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.codegen.FunctionCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.JetTypeMapper
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.lang.resolve.android.AndroidConst
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaClassDescriptor
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor
import org.jetbrains.kotlin.resolve.scopes.receivers.ClassReceiver
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.types.Flexibility
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes.ACC_PRIVATE
import org.jetbrains.org.objectweb.asm.Opcodes.ACC_PUBLIC
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

private enum class AndroidClassType(val internalClassName: String, val supportsCache: Boolean = false) {
    ACTIVITY : AndroidClassType("android/app/Activity", true)
    FRAGMENT : AndroidClassType("android/app/Fragment", true)
    VIEW : AndroidClassType("android/view/View")
    UNKNOWN : AndroidClassType("")
}

public class AndroidExpressionCodegenExtension : ExpressionCodegenExtension {
    default object {
        private val PROPERTY_NAME = "_\$_findViewCache"
        private val CACHED_FIND_VIEW_BY_ID_METHOD_NAME = "_\$_findCachedViewById"
        private val CLEAR_CACHE_METHOD_NAME = "_\$_clearFindViewByIdCache"
    }

    private class SyntheticPartsGenerateContext(
            val classBuilder: ClassBuilder,
            val state: GenerationState,
            val descriptor: ClassDescriptor,
            val classOrObject: JetClassOrObject,
            val androidClassType: AndroidClassType) {

    }

    override fun apply(receiver: StackValue, resolvedCall: ResolvedCall<*>, c: ExpressionCodegenExtension.Context): Boolean {
        val resultingDescriptor = resolvedCall.getResultingDescriptor()
        return when (resultingDescriptor) {
            is PropertyDescriptor -> generateSyntheticPropertyCall(receiver, resolvedCall, c, resultingDescriptor)
            is FunctionDescriptor -> generateSyntheticFunctionCall(receiver, resolvedCall, c, resultingDescriptor)
            else -> false
        }
    }

    private fun generateSyntheticFunctionCall(
            receiver: StackValue,
            resolvedCall: ResolvedCall<*>,
            c: ExpressionCodegenExtension.Context,
            descriptor: FunctionDescriptor
    ): Boolean {
        if (descriptor.getAndroidPackage() == null) return false
        if (descriptor.getName().asString() != AndroidConst.CLEAR_FUNCTION_NAME) return false

        val declarationDescriptor = resolvedCall.getReceiverDeclarationDescriptor() ?: return false
        if (!isCacheSupported(declarationDescriptor)) return true

        val androidClassType = getClassType(declarationDescriptor)
        if (androidClassType == AndroidClassType.UNKNOWN) return false

        val bytecodeClassName = DescriptorUtils.getFqName(declarationDescriptor).asString().replace('.', '/')

        receiver.put(c.typeMapper.mapType(declarationDescriptor), c.v)
        c.v.invokevirtual(bytecodeClassName, CLEAR_CACHE_METHOD_NAME, "()V", false)

        return true
    }

    private fun generateSyntheticPropertyCall(
            receiver: StackValue,
            resolvedCall: ResolvedCall<*>,
            c: ExpressionCodegenExtension.Context,
            descriptor: PropertyDescriptor
    ): Boolean {
        val androidPackage = descriptor.getAndroidPackage() ?: return false
        val declarationDescriptor = resolvedCall.getReceiverDeclarationDescriptor() ?: return false

        val androidClassType = getClassType(declarationDescriptor)
        if (androidClassType.supportsCache && isCacheSupported(declarationDescriptor)) {
            val className = DescriptorUtils.getFqName(declarationDescriptor).toString()
            val bytecodeClassName = className.replace('.', '/')

            receiver.put(c.typeMapper.mapType(declarationDescriptor), c.v)
            c.v.getstatic(androidPackage.replace(".", "/") + "/R\$id", descriptor.getName().asString(), "I")
            c.v.invokevirtual(bytecodeClassName, CACHED_FIND_VIEW_BY_ID_METHOD_NAME, "(I)Landroid/view/View;", false)
        }
        else {
            when (androidClassType) {
                AndroidClassType.ACTIVITY, AndroidClassType.VIEW -> {
                    receiver.put(Type.getType("L${androidClassType.internalClassName};"), c.v)
                    c.v.getstatic(androidPackage.replace(".", "/") + "/R\$id", descriptor.getName().asString(), "I")
                    c.v.invokevirtual(androidClassType.internalClassName, "findViewById", "(I)Landroid/view/View;", false)
                }
                AndroidClassType.FRAGMENT -> {
                    receiver.put(Type.getType("L${androidClassType.internalClassName};"), c.v)
                    c.v.invokevirtual(androidClassType.internalClassName, "getView", "()Landroid/view/View;", false)
                    c.v.getstatic(androidPackage.replace(".", "/") + "/R\$id", descriptor.getName().asString(), "I")
                    c.v.invokevirtual("android/view/View", "findViewById", "(I)Landroid/view/View;", false)
                }
                else -> return false
            }


        }

        val retType = c.typeMapper.mapType(descriptor.getReturnType()!!)
        c.v.checkcast(retType)
        return true
    }

    private fun CallableDescriptor.getAndroidPackage(): String? {
        return DescriptorToSourceUtils.getContainingFile(this)?.getUserData<String>(AndroidConst.ANDROID_USER_PACKAGE)
    }

    private fun ResolvedCall<*>.getReceiverDeclarationDescriptor(): ClassifierDescriptor? {
        return getExtensionReceiver().getType().getConstructor().getDeclarationDescriptor()
    }

    private fun isCacheSupported(descriptor: ClassifierDescriptor) = descriptor.getSource() is KotlinSourceElement

    private fun getClassType(descriptor: ClassifierDescriptor): AndroidClassType {
        fun getClassTypeInternal(name: String): AndroidClassType? = when (name) {
            "android.app.Activity" -> AndroidClassType.ACTIVITY
            "android.app.Fragment" -> AndroidClassType.FRAGMENT
            "android.view.View" -> AndroidClassType.VIEW
            else -> null
        }

        if (descriptor is LazyJavaClassDescriptor) {
            val androidClassType = getClassTypeInternal(descriptor.fqName.asString())
            if (androidClassType != null) return androidClassType
        }
        else if (descriptor is LazyClassDescriptor) { // For tests (FakeActivity)
            val androidClassType = getClassTypeInternal(DescriptorUtils.getFqName(descriptor).toString())
            if (androidClassType != null) return androidClassType
        }

        for (supertype in descriptor.getTypeConstructor().getSupertypes()) {
            val declarationDescriptor = supertype.getConstructor().getDeclarationDescriptor()
            if (declarationDescriptor != null) {
                val androidClassType = getClassType(declarationDescriptor)
                if (androidClassType != AndroidClassType.UNKNOWN) return androidClassType
            }
        }

        return AndroidClassType.UNKNOWN
    }

    override fun generateClassSyntheticParts(
            classBuilder: ClassBuilder,
            state: GenerationState,
            classOrObject: JetClassOrObject,
            descriptor: ClassDescriptor
    ) {
        if (descriptor.getKind() != ClassKind.CLASS || descriptor.isInner() || DescriptorUtils.isLocal(descriptor)) return

        // Do not generate anything if class is not supported
        val androidClassType = getClassType(descriptor)
        if (androidClassType == AndroidClassType.UNKNOWN) return

        val context = SyntheticPartsGenerateContext(classBuilder, state, descriptor, classOrObject, androidClassType)
        context.generateCachedFindViewByIdFunction()
        context.generateClearCacheFunction()

        classBuilder.newField(JvmDeclarationOrigin.Default.NO_ORIGIN, ACC_PRIVATE, PROPERTY_NAME, "Ljava/util/HashMap;", null, null)
    }

    private fun SyntheticPartsGenerateContext.generateClearCacheFunction() {
        val methodVisitor = classBuilder.newMethod(
                JvmDeclarationOrigin.Default.NO_ORIGIN, ACC_PUBLIC, CLEAR_CACHE_METHOD_NAME, "()V", null, null)
        methodVisitor.visitCode()
        val iv = InstructionAdapter(methodVisitor)

        val classType = state.getTypeMapper().mapClass(descriptor)
        val className = classType.getInternalName()

        fun loadCache() {
            iv.load(0, classType)
            iv.getfield(className, PROPERTY_NAME, "Ljava/util/HashMap;")
        }

        loadCache()
        val lCacheIsNull = Label()
        iv.ifnull(lCacheIsNull)

        loadCache()
        iv.invokevirtual("java/util/HashMap", "clear", "()V", false)

        iv.visitLabel(lCacheIsNull)
        iv.areturn(Type.VOID_TYPE)
        FunctionCodegen.endVisit(methodVisitor, CLEAR_CACHE_METHOD_NAME, classOrObject)
    }

    private fun SyntheticPartsGenerateContext.generateCachedFindViewByIdFunction() {
        val classType = state.getTypeMapper().mapClass(descriptor)
        val className = classType.getInternalName()

        val viewType = Type.getObjectType("android/view/View")

        val methodVisitor = classBuilder.newMethod(
                JvmDeclarationOrigin.Default.NO_ORIGIN, ACC_PUBLIC, CACHED_FIND_VIEW_BY_ID_METHOD_NAME, "(I)Landroid/view/View;", null, null)
        methodVisitor.visitCode()
        val iv = InstructionAdapter(methodVisitor)

        fun loadCache() {
            iv.load(0, classType)
            iv.getfield(className, PROPERTY_NAME, "Ljava/util/HashMap;")
        }

        fun loadId() = iv.load(1, Type.INT_TYPE)

        // Get cache property
        loadCache()

        val lCacheNonNull = Label()
        iv.ifnonnull(lCacheNonNull)

        // Init cache if null
        iv.load(0, classType)
        iv.anew(Type.getType("Ljava/util/HashMap;"))
        iv.dup()
        iv.invokespecial("java/util/HashMap", "<init>", "()V", false)
        iv.putfield(className, PROPERTY_NAME, "Ljava/util/HashMap;")

        // Get View from cache
        iv.visitLabel(lCacheNonNull)
        loadCache()
        loadId()
        iv.invokestatic("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false)
        iv.invokevirtual("java/util/HashMap", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", false)
        iv.checkcast(viewType)
        iv.store(2, viewType)

        val lViewNonNull = Label()
        iv.load(2, viewType)
        iv.ifnonnull(lViewNonNull)

        // Resolve View via findViewById if not in cache
        iv.load(0, classType)
        when (androidClassType) {
            AndroidClassType.ACTIVITY, AndroidClassType.VIEW -> {
                loadId()
                iv.invokevirtual(className, "findViewById", "(I)Landroid/view/View;", false)
            }
            AndroidClassType.FRAGMENT -> {
                iv.invokevirtual(className, "getView", "()Landroid/view/View;", false)
                loadId()
                iv.invokevirtual("android/view/View", "findViewById", "(I)Landroid/view/View;", false)
            }
        }
        iv.store(2, viewType)

        // Store resolved View in cache
        loadCache()
        loadId()
        iv.invokestatic("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false)
        iv.load(2, viewType)
        iv.invokevirtual("java/util/HashMap", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false)
        iv.pop()

        iv.visitLabel(lViewNonNull)
        iv.load(2, viewType)
        iv.areturn(viewType)

        FunctionCodegen.endVisit(methodVisitor, CACHED_FIND_VIEW_BY_ID_METHOD_NAME, classOrObject)
    }
}