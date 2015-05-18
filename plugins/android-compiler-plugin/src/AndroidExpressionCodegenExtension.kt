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
import org.jetbrains.kotlin.types.lowerIfFlexible
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes.ACC_PRIVATE
import org.jetbrains.org.objectweb.asm.Opcodes.ACC_PUBLIC
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

private enum class AndroidClassType(val internalClassName: String, val supportsCache: Boolean = false) {
    ACTIVITY(AndroidConst.ACTIVITY_FQNAME.innerName, true),
    FRAGMENT(AndroidConst.FRAGMENT_FQNAME.innerName, true),
    SUPPORT_FRAGMENT_ACTIVITY(AndroidConst.SUPPORT_FRAGMENT_ACTIVITY_FQNAME.innerName, true),
    SUPPORT_FRAGMENT(AndroidConst.SUPPORT_FRAGMENT_FQNAME.innerName, true),
    VIEW(AndroidConst.VIEW_FQNAME.innerName),
    UNKNOWN("")
}

private val String.innerName: String
    get() = replace('.', '/')

public class AndroidExpressionCodegenExtension : ExpressionCodegenExtension {
    companion object {
        private val PROPERTY_NAME = "_\$_findViewCache"
        private val CACHED_FIND_VIEW_BY_ID_METHOD_NAME = "_\$_findCachedViewById"
        private val CLEAR_CACHE_METHOD_NAME = "_\$_clearFindViewByIdCache"

        fun isCacheSupported(descriptor: ClassifierDescriptor) = descriptor.getSource() is KotlinSourceElement
    }

    private class SyntheticPartsGenerateContext(
            val classBuilder: ClassBuilder,
            val state: GenerationState,
            val descriptor: ClassDescriptor,
            val classOrObject: JetClassOrObject,
            val androidClassType: AndroidClassType) {

    }

    override fun applyProperty(receiver: StackValue, resolvedCall: ResolvedCall<*>, c: ExpressionCodegenExtension.Context): StackValue? {
        val resultingDescriptor = resolvedCall.getResultingDescriptor()
        return if (resultingDescriptor is PropertyDescriptor) {
            return generateSyntheticPropertyCall(receiver, resolvedCall, c, resultingDescriptor)
        }
        else null
    }

    override fun applyFunction(receiver: StackValue, resolvedCall: ResolvedCall<*>, c: ExpressionCodegenExtension.Context): Boolean {
        val resultingDescriptor = resolvedCall.getResultingDescriptor()
        return if (resultingDescriptor is FunctionDescriptor) {
            return generateSyntheticFunctionCall(receiver, resolvedCall, c, resultingDescriptor)
        }
        else false
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
    ): StackValue? {
        val androidPackage = descriptor.getAndroidPackage() ?: return null
        val declarationDescriptor = resolvedCall.getReceiverDeclarationDescriptor() ?: return null
        val androidClassType = getClassType(declarationDescriptor)


        return SyntheticProperty(receiver, c.typeMapper, descriptor, declarationDescriptor, androidClassType, androidPackage)
    }

    private class SyntheticProperty(
            val receiver: StackValue,
            val typeMapper: JetTypeMapper,
            val descriptor: PropertyDescriptor,
            val declarationDescriptor: ClassifierDescriptor,
            val androidClassType: AndroidClassType,
            val androidPackage: String
    ) : StackValue(typeMapper.mapType(descriptor.getReturnType()!!)) {

        override fun putSelector(type: Type, v: InstructionAdapter) {
            val returnTypeString = typeMapper.mapType(descriptor.getType().lowerIfFlexible()).getClassName()
            if (AndroidConst.FRAGMENT_FQNAME == returnTypeString || AndroidConst.SUPPORT_FRAGMENT_FQNAME == returnTypeString) {
                return putSelectorForFragment(v)
            }

            if (androidClassType.supportsCache && isCacheSupported(declarationDescriptor)) {
                val declarationDescriptorType = typeMapper.mapType(declarationDescriptor)
                receiver.put(declarationDescriptorType, v)
                v.getstatic(androidPackage.replace(".", "/") + "/R\$id", descriptor.getName().asString(), "I")
                v.invokevirtual(declarationDescriptorType.getInternalName(), CACHED_FIND_VIEW_BY_ID_METHOD_NAME, "(I)Landroid/view/View;", false)
            }
            else {
                when (androidClassType) {
                    AndroidClassType.ACTIVITY, AndroidClassType.SUPPORT_FRAGMENT_ACTIVITY, AndroidClassType.VIEW -> {
                        receiver.put(Type.getType("L${androidClassType.internalClassName};"), v)
                        getResourceId(v)
                        v.invokevirtual(androidClassType.internalClassName, "findViewById", "(I)Landroid/view/View;", false)
                    }
                    AndroidClassType.FRAGMENT, AndroidClassType.SUPPORT_FRAGMENT -> {
                        receiver.put(Type.getType("L${androidClassType.internalClassName};"), v)
                        v.invokevirtual(androidClassType.internalClassName, "getView", "()Landroid/view/View;", false)
                        getResourceId(v)
                        v.invokevirtual("android/view/View", "findViewById", "(I)Landroid/view/View;", false)
                    }
                    else -> throw IllegalStateException("Invalid Android class type: $androidClassType") // Should never occur
                }
            }

            v.checkcast(this.type)
        }

        private fun putSelectorForFragment(v: InstructionAdapter) {
            receiver.put(Type.getType("L${androidClassType.internalClassName};"), v)

            when (androidClassType) {
                AndroidClassType.ACTIVITY, AndroidClassType.FRAGMENT -> {
                    v.invokevirtual(androidClassType.internalClassName, "getFragmentManager", "()Landroid/app/FragmentManager;", false)
                    getResourceId(v)
                    v.invokevirtual("android/app/FragmentManager", "findFragmentById", "(I)Landroid/app/Fragment;", false)
                }
                AndroidClassType.SUPPORT_FRAGMENT -> {
                    v.invokevirtual(androidClassType.internalClassName, "getFragmentManager", "()Landroid/support/v4/app/FragmentManager;", false)
                    getResourceId(v)
                    v.invokevirtual("android/support/v4/app/FragmentManager", "findFragmentById", "(I)Landroid/support/v4/app/Fragment;", false)
                }
                AndroidClassType.SUPPORT_FRAGMENT_ACTIVITY -> {
                    v.invokevirtual(androidClassType.internalClassName, "getSupportFragmentManager", "()Landroid/support/v4/app/FragmentManager;", false)
                    getResourceId(v)
                    v.invokevirtual("android/support/v4/app/FragmentManager", "findFragmentById", "(I)Landroid/support/v4/app/Fragment;", false)
                }
                else -> throw IllegalStateException("Invalid Android class type: $androidClassType") // Should never occur
            }

            v.checkcast(this.type)
        }

        fun getResourceId(v: InstructionAdapter) {
            v.getstatic(androidPackage.replace(".", "/") + "/R\$id", descriptor.getName().asString(), "I")
        }
    }

    private fun CallableDescriptor.getAndroidPackage(): String? {
        return DescriptorToSourceUtils.getContainingFile(this)?.getUserData<String>(AndroidConst.ANDROID_USER_PACKAGE)
    }

    private fun ResolvedCall<*>.getReceiverDeclarationDescriptor(): ClassifierDescriptor? {
        return getExtensionReceiver().getType().getConstructor().getDeclarationDescriptor()
    }

    private fun getClassType(descriptor: ClassifierDescriptor): AndroidClassType {
        fun getClassTypeInternal(name: String): AndroidClassType? = when (name) {
            AndroidConst.ACTIVITY_FQNAME -> AndroidClassType.ACTIVITY
            AndroidConst.FRAGMENT_FQNAME -> AndroidClassType.FRAGMENT
            AndroidConst.SUPPORT_FRAGMENT_ACTIVITY_FQNAME -> AndroidClassType.SUPPORT_FRAGMENT_ACTIVITY
            AndroidConst.SUPPORT_FRAGMENT_FQNAME -> AndroidClassType.SUPPORT_FRAGMENT
            AndroidConst.VIEW_FQNAME -> AndroidClassType.VIEW
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

        classBuilder.newField(JvmDeclarationOrigin.NO_ORIGIN, ACC_PRIVATE, PROPERTY_NAME, "Ljava/util/HashMap;", null, null)
    }

    private fun SyntheticPartsGenerateContext.generateClearCacheFunction() {
        val methodVisitor = classBuilder.newMethod(
                JvmDeclarationOrigin.NO_ORIGIN, ACC_PUBLIC, CLEAR_CACHE_METHOD_NAME, "()V", null, null)
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
                JvmDeclarationOrigin.NO_ORIGIN, ACC_PUBLIC, CACHED_FIND_VIEW_BY_ID_METHOD_NAME, "(I)Landroid/view/View;", null, null)
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
            AndroidClassType.ACTIVITY, AndroidClassType.SUPPORT_FRAGMENT_ACTIVITY, AndroidClassType.VIEW -> {
                loadId()
                iv.invokevirtual(className, "findViewById", "(I)Landroid/view/View;", false)
            }
            AndroidClassType.FRAGMENT, AndroidClassType.SUPPORT_FRAGMENT -> {
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