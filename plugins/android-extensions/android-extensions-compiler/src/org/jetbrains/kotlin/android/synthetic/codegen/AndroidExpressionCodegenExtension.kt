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

package org.jetbrains.kotlin.android.synthetic.codegen

import org.jetbrains.kotlin.android.synthetic.AndroidConst
import org.jetbrains.kotlin.android.synthetic.descriptors.AndroidSyntheticPackageFragmentDescriptor
import org.jetbrains.kotlin.android.synthetic.res.AndroidSyntheticFunction
import org.jetbrains.kotlin.android.synthetic.res.AndroidSyntheticProperty
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.FunctionCodegen
import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes.ACC_PRIVATE
import org.jetbrains.org.objectweb.asm.Opcodes.ACC_PUBLIC
import org.jetbrains.org.objectweb.asm.Opcodes.ACC_SYNTHETIC
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

class AndroidExpressionCodegenExtension : ExpressionCodegenExtension {
    companion object {
        private val PROPERTY_NAME = "_\$_findViewCache"
        val CACHED_FIND_VIEW_BY_ID_METHOD_NAME = "_\$_findCachedViewById"
        val CLEAR_CACHE_METHOD_NAME = "_\$_clearFindViewByIdCache"
        val ON_DESTROY_METHOD_NAME = "onDestroyView"

        fun doesContainerSupportCache(container: ClassDescriptor): Boolean {
            return container.source is KotlinSourceElement
        }

        fun shouldCacheResource(container: ClassDescriptor, resource: PropertyDescriptor): Boolean {
            if (!doesContainerSupportCache(container)) {
                return false
            }

            return (resource as? AndroidSyntheticProperty)?.shouldBeCached ?: false
        }
    }

    private class SyntheticPartsGenerateContext(
            val classBuilder: ClassBuilder,
            val state: GenerationState,
            val descriptor: ClassDescriptor,
            val classOrObject: KtClassOrObject,
            val androidClassType: AndroidClassType)

    override fun applyProperty(receiver: StackValue, resolvedCall: ResolvedCall<*>, c: ExpressionCodegenExtension.Context): StackValue? {
        val resultingDescriptor = resolvedCall.resultingDescriptor
        return if (resultingDescriptor is PropertyDescriptor) {
            return generateResourcePropertyCall(receiver, resolvedCall, c, resultingDescriptor)
        }
        else null
    }

    override fun applyFunction(receiver: StackValue, resolvedCall: ResolvedCall<*>, c: ExpressionCodegenExtension.Context): StackValue? {
        val targetCallable = resolvedCall.resultingDescriptor
        resolvedCall.resultingDescriptor as? AndroidSyntheticFunction ?: return null

        return if (targetCallable.name.asString() == AndroidConst.CLEAR_FUNCTION_NAME) {
            val container = resolvedCall.getReceiverDeclarationDescriptor() as? ClassDescriptor ?: return null
            generateClearFindViewByIdCacheFunctionCall(receiver, container, c)
        }
        else {
            null
        }
    }

    private fun generateClearFindViewByIdCacheFunctionCall(
            receiver: StackValue,
            container: ClassDescriptor,
            c: ExpressionCodegenExtension.Context
    ): StackValue? {
        if (!doesContainerSupportCache(container)) {
            return StackValue.functionCall(Type.VOID_TYPE) {}
        }

        val androidClassType = AndroidClassType.getClassType(container)
        if (androidClassType == AndroidClassType.UNKNOWN) return null

        return StackValue.functionCall(Type.VOID_TYPE) {
            val bytecodeClassName = c.typeMapper.mapType(container).internalName

            receiver.put(c.typeMapper.mapType(container), it)
            it.invokevirtual(bytecodeClassName, CLEAR_CACHE_METHOD_NAME, "()V", false)
        }
    }

    private fun generateResourcePropertyCall(
            receiver: StackValue,
            resolvedCall: ResolvedCall<*>,
            c: ExpressionCodegenExtension.Context,
            descriptor: PropertyDescriptor
    ): StackValue? {
        if (descriptor !is AndroidSyntheticProperty) return null
        val packageFragment = descriptor.containingDeclaration as? AndroidSyntheticPackageFragmentDescriptor ?: return null
        val androidPackage = packageFragment.packageData.moduleData.module.applicationPackage
        val receiverDescriptor = resolvedCall.getReceiverDeclarationDescriptor() as? ClassDescriptor ?: return null
        val androidClassType = AndroidClassType.getClassType(receiverDescriptor)

        return ResourcePropertyStackValue(receiver, c.typeMapper, descriptor, receiverDescriptor, androidClassType, androidPackage)
    }

    private fun ResolvedCall<*>.getReceiverDeclarationDescriptor(): ClassifierDescriptor? {
        return (extensionReceiver as ReceiverValue).type.constructor.declarationDescriptor
    }

    override fun generateClassSyntheticParts(codegen: ImplementationBodyCodegen) {
        val classBuilder = codegen.v
        val targetClass = codegen.myClass as? KtClassOrObject ?: return

        val descriptor = codegen.descriptor
        if (descriptor.kind != ClassKind.CLASS || descriptor.isInner || DescriptorUtils.isLocal(descriptor)) return

        // Do not generate anything if class is not supported
        val androidClassType = AndroidClassType.getClassType(descriptor)
        if (androidClassType == AndroidClassType.UNKNOWN) return

        val context = SyntheticPartsGenerateContext(classBuilder, codegen.state, descriptor, targetClass, androidClassType)
        context.generateCachedFindViewByIdFunction()
        context.generateClearCacheFunction()

        if (androidClassType.fragment) {
            val classMembers = descriptor.unsubstitutedMemberScope.getContributedDescriptors()
            val onDestroy = classMembers.firstOrNull { it is FunctionDescriptor && it.isOnDestroyFunction() }
            if (onDestroy == null) {
                context.generateOnDestroyFunctionForFragment()
            }
        }

        classBuilder.newField(JvmDeclarationOrigin.NO_ORIGIN, ACC_PRIVATE, PROPERTY_NAME, "Ljava/util/HashMap;", null, null)
    }

    private fun FunctionDescriptor.isOnDestroyFunction(): Boolean {
        return kind == CallableMemberDescriptor.Kind.DECLARATION
               && name.asString() == ON_DESTROY_METHOD_NAME
               && (visibility == Visibilities.INHERITED || visibility == Visibilities.PUBLIC)
               && (valueParameters.isEmpty())
               && (typeParameters.isEmpty())
    }

    // This generates a simple onDestroy(): Unit = super.onDestroy() function.
    // CLEAR_CACHE_METHOD_NAME() method call will be inserted in ClassBuilder interceptor.
    private fun SyntheticPartsGenerateContext.generateOnDestroyFunctionForFragment() {
        val methodVisitor = classBuilder.newMethod(JvmDeclarationOrigin.NO_ORIGIN, ACC_PUBLIC or ACC_SYNTHETIC, ON_DESTROY_METHOD_NAME, "()V", null, null)
        methodVisitor.visitCode()
        val iv = InstructionAdapter(methodVisitor)

        val classType = state.typeMapper.mapClass(descriptor)

        iv.load(0, classType)
        iv.invokespecial(state.typeMapper.mapClass(descriptor.getSuperClassOrAny()).internalName, ON_DESTROY_METHOD_NAME, "()V", false)
        iv.areturn(Type.VOID_TYPE)

        FunctionCodegen.endVisit(methodVisitor, ON_DESTROY_METHOD_NAME, classOrObject)
    }

    private fun SyntheticPartsGenerateContext.generateClearCacheFunction() {
        val methodVisitor = classBuilder.newMethod(JvmDeclarationOrigin.NO_ORIGIN, ACC_PUBLIC, CLEAR_CACHE_METHOD_NAME, "()V", null, null)
        methodVisitor.visitCode()
        val iv = InstructionAdapter(methodVisitor)

        val classType = state.typeMapper.mapClass(descriptor)
        val className = classType.internalName

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
        val classType = state.typeMapper.mapClass(descriptor)
        val className = classType.internalName

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
            AndroidClassType.ACTIVITY, AndroidClassType.SUPPORT_FRAGMENT_ACTIVITY, AndroidClassType.VIEW, AndroidClassType.DIALOG -> {
                loadId()
                iv.invokevirtual(className, "findViewById", "(I)Landroid/view/View;", false)
            }
            AndroidClassType.FRAGMENT, AndroidClassType.SUPPORT_FRAGMENT -> {
                iv.invokevirtual(className, "getView", "()Landroid/view/View;", false)
                iv.dup()
                val lgetViewNotNull = Label()
                iv.ifnonnull(lgetViewNotNull)

                // Return if getView() is null
                iv.pop()
                iv.aconst(null)
                iv.areturn(viewType)

                // Else return getView().findViewById(id)
                iv.visitLabel(lgetViewNotNull)
                loadId()
                iv.invokevirtual("android/view/View", "findViewById", "(I)Landroid/view/View;", false)
            }
            else -> throw IllegalStateException("Can't generate code for $androidClassType")
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