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

import kotlinx.android.extensions.CacheImplementation
import kotlinx.android.extensions.CacheImplementation.NO_CACHE
import org.jetbrains.kotlin.android.synthetic.AndroidConst
import org.jetbrains.kotlin.android.synthetic.codegen.AndroidContainerType.LAYOUT_CONTAINER
import org.jetbrains.kotlin.android.synthetic.descriptors.ContainerOptionsProxy
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
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes.ACC_PRIVATE
import org.jetbrains.org.objectweb.asm.Opcodes.ACC_PUBLIC
import org.jetbrains.org.objectweb.asm.Opcodes.ACC_SYNTHETIC
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

abstract class AbstractAndroidExtensionsExpressionCodegenExtension : ExpressionCodegenExtension {
    companion object {
        val PROPERTY_NAME = "_\$_findViewCache"
        val CACHED_FIND_VIEW_BY_ID_METHOD_NAME = "_\$_findCachedViewById"
        val CLEAR_CACHE_METHOD_NAME = "_\$_clearFindViewByIdCache"
        val ON_DESTROY_METHOD_NAME = "onDestroyView"

        fun shouldCacheResource(resource: PropertyDescriptor) = (resource as? AndroidSyntheticProperty)?.shouldBeCached == true
    }

    private class SyntheticPartsGenerateContext(
            val classBuilder: ClassBuilder,
            val state: GenerationState,
            val container: ClassDescriptor,
            val classOrObject: KtClassOrObject,
            val containerOptions: ContainerOptionsProxy)

    protected abstract fun isEnabled(element: KtElement?): Boolean
    protected abstract fun isExperimental(element: KtElement?): Boolean
    protected abstract fun getGlobalCacheImpl(element: KtElement?): CacheImplementation

    private fun ContainerOptionsProxy.getCacheOrDefault(element: KtElement?) = this.cache ?: getGlobalCacheImpl(element)

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
            generateClearFindViewByIdCacheFunctionCall(receiver, resolvedCall, container, c)
        }
        else {
            null
        }
    }

    private fun generateClearFindViewByIdCacheFunctionCall(
            receiver: StackValue,
            resolvedCall: ResolvedCall<*>,
            container: ClassDescriptor,
            c: ExpressionCodegenExtension.Context
    ): StackValue? {
        val containerOptions = ContainerOptionsProxy.create(container)

        if (!containerOptions.getCacheOrDefault(resolvedCall.call.calleeExpression).hasCache) {
            return StackValue.functionCall(Type.VOID_TYPE) {}
        }

        if (containerOptions.containerType == AndroidContainerType.UNKNOWN) return null
        val actualReceiver = StackValue.receiver(resolvedCall, receiver, c.codegen, null)

        return StackValue.functionCall(Type.VOID_TYPE) {
            val bytecodeClassName = c.typeMapper.mapType(container).internalName

            actualReceiver.put(c.typeMapper.mapType(container), it)
            it.invokevirtual(bytecodeClassName, CLEAR_CACHE_METHOD_NAME, "()V", false)
        }
    }

    private fun generateResourcePropertyCall(
            receiver: StackValue,
            resolvedCall: ResolvedCall<*>,
            c: ExpressionCodegenExtension.Context,
            resource: PropertyDescriptor
    ): StackValue? {
        if (resource !is AndroidSyntheticProperty) return null
        val packageFragment = resource.containingDeclaration as? AndroidSyntheticPackageFragmentDescriptor ?: return null
        val androidPackage = packageFragment.packageData.moduleData.module.applicationPackage
        val container = resolvedCall.getReceiverDeclarationDescriptor() as? ClassDescriptor ?: return null

        val containerOptions = ContainerOptionsProxy.create(container)
        return ResourcePropertyStackValue(receiver, c.typeMapper, resource, container,
                                          containerOptions, androidPackage, getGlobalCacheImpl(resolvedCall.call.calleeExpression))
    }

    private fun ResolvedCall<*>.getReceiverDeclarationDescriptor(): ClassifierDescriptor? {
        val fromDeclarationSite = resultingDescriptor.extensionReceiverParameter?.type?.constructor?.declarationDescriptor
        val fromCallSite = (extensionReceiver as ReceiverValue).type.constructor.declarationDescriptor

        if (fromDeclarationSite == null && fromCallSite == null)
            return null
        else if (fromDeclarationSite != null && fromCallSite == null)
            return fromDeclarationSite
        else if (fromDeclarationSite == null && fromCallSite != null)
            return fromCallSite

        fromDeclarationSite as ClassifierDescriptor
        fromCallSite as ClassifierDescriptor

        return if (fromCallSite.defaultType.isSubtypeOf(fromDeclarationSite.defaultType))
            fromCallSite
        else
            fromDeclarationSite
    }

    override fun generateClassSyntheticParts(codegen: ImplementationBodyCodegen) {
        val classBuilder = codegen.v
        val targetClass = codegen.myClass as? KtClass ?: return

        if (!isEnabled(targetClass)) return

        val container = codegen.descriptor
        if (container.kind != ClassKind.CLASS && container.kind != ClassKind.OBJECT) return

        val containerOptions = ContainerOptionsProxy.create(container)
        if (containerOptions.getCacheOrDefault(targetClass) == NO_CACHE) return

        if (containerOptions.containerType == LAYOUT_CONTAINER && !isExperimental(targetClass)) {
            return
        }

        val context = SyntheticPartsGenerateContext(classBuilder, codegen.state, container, targetClass, containerOptions)
        context.generateCachedFindViewByIdFunction()
        context.generateClearCacheFunction()
        context.generateCacheField()

        if (containerOptions.containerType.isFragment) {
            val classMembers = container.unsubstitutedMemberScope.getContributedDescriptors()
            val onDestroy = classMembers.firstOrNull { it is FunctionDescriptor && it.isOnDestroyFunction() }
            if (onDestroy == null) {
                context.generateOnDestroyFunctionForFragment()
            }
        }
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

        val containerType = state.typeMapper.mapClass(container)

        iv.load(0, containerType)
        iv.invokespecial(state.typeMapper.mapClass(container.getSuperClassOrAny()).internalName, ON_DESTROY_METHOD_NAME, "()V", false)
        iv.areturn(Type.VOID_TYPE)

        FunctionCodegen.endVisit(methodVisitor, ON_DESTROY_METHOD_NAME, classOrObject)
    }

    private fun SyntheticPartsGenerateContext.generateClearCacheFunction() {
        val methodVisitor = classBuilder.newMethod(JvmDeclarationOrigin.NO_ORIGIN, ACC_PUBLIC, CLEAR_CACHE_METHOD_NAME, "()V", null, null)
        methodVisitor.visitCode()
        val iv = InstructionAdapter(methodVisitor)

        val containerType = state.typeMapper.mapClass(container)
        val cacheImpl = CacheMechanism.get(containerOptions.getCacheOrDefault(classOrObject), iv, containerType)

        cacheImpl.loadCache()
        val lCacheIsNull = Label()
        iv.ifnull(lCacheIsNull)

        cacheImpl.loadCache()
        cacheImpl.clearCache()

        iv.visitLabel(lCacheIsNull)
        iv.areturn(Type.VOID_TYPE)
        FunctionCodegen.endVisit(methodVisitor, CLEAR_CACHE_METHOD_NAME, classOrObject)
    }

    private fun SyntheticPartsGenerateContext.generateCacheField() {
        val cacheImpl = CacheMechanism.getType(containerOptions.getCacheOrDefault(classOrObject))
        classBuilder.newField(JvmDeclarationOrigin.NO_ORIGIN, ACC_PRIVATE, PROPERTY_NAME, cacheImpl.descriptor, null, null)
    }

    private fun SyntheticPartsGenerateContext.generateCachedFindViewByIdFunction() {
        val containerAsmType = state.typeMapper.mapClass(container)

        val viewType = Type.getObjectType("android/view/View")

        val methodVisitor = classBuilder.newMethod(
                JvmDeclarationOrigin.NO_ORIGIN, ACC_PUBLIC, CACHED_FIND_VIEW_BY_ID_METHOD_NAME, "(I)Landroid/view/View;", null, null)
        methodVisitor.visitCode()
        val iv = InstructionAdapter(methodVisitor)

        val cacheImpl = CacheMechanism.get(containerOptions.getCacheOrDefault(classOrObject), iv, containerAsmType)

        fun loadId() = iv.load(1, Type.INT_TYPE)

        // Get cache property
        cacheImpl.loadCache()

        val lCacheNonNull = Label()
        iv.ifnonnull(lCacheNonNull)

        // Init cache if null
        cacheImpl.initCache()

        // Get View from cache
        iv.visitLabel(lCacheNonNull)
        cacheImpl.loadCache()
        loadId()
        cacheImpl.getViewFromCache()
        iv.checkcast(viewType)
        iv.store(2, viewType)

        val lViewNonNull = Label()
        iv.load(2, viewType)
        iv.ifnonnull(lViewNonNull)

        // Resolve View via findViewById if not in cache
        iv.load(0, containerAsmType)

        val containerType = containerOptions.containerType
        when (containerType) {
            AndroidContainerType.ACTIVITY, AndroidContainerType.SUPPORT_FRAGMENT_ACTIVITY, AndroidContainerType.VIEW, AndroidContainerType.DIALOG -> {
                loadId()
                iv.invokevirtual(containerType.internalClassName, "findViewById", "(I)Landroid/view/View;", false)
            }
            AndroidContainerType.FRAGMENT, AndroidContainerType.SUPPORT_FRAGMENT, LAYOUT_CONTAINER -> {
                if (containerType == LAYOUT_CONTAINER) {
                    iv.invokeinterface(containerType.internalClassName, "getContainerView", "()Landroid/view/View;")
                } else {
                    iv.invokevirtual(containerType.internalClassName, "getView", "()Landroid/view/View;", false)
                }

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
            else -> throw IllegalStateException("Can't generate code for $containerType")
        }
        iv.store(2, viewType)

        // Store resolved View in cache
        cacheImpl.loadCache()
        loadId()
        cacheImpl.putViewToCache { iv.load(2, viewType) }

        iv.visitLabel(lViewNonNull)
        iv.load(2, viewType)
        iv.areturn(viewType)

        FunctionCodegen.endVisit(methodVisitor, CACHED_FIND_VIEW_BY_ID_METHOD_NAME, classOrObject)
    }
}