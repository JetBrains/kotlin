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
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.lang.resolve.android.AndroidConst
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaClassDescriptor
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor
import org.jetbrains.kotlin.resolve.scopes.receivers.ClassReceiver
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
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
        private val METHOD_NAME = "_\$_findCachedViewById"
    }

    override fun apply(receiver: StackValue, resolvedCall: ResolvedCall<*>, c: ExpressionCodegenExtension.Context): StackValue? {
        if (resolvedCall.getResultingDescriptor() !is PropertyDescriptor) return null

        val propertyDescriptor = resolvedCall.getResultingDescriptor() as PropertyDescriptor

        val file = DescriptorToSourceUtils.getContainingFile(propertyDescriptor)
        if (file == null) return null

        val androidPackage = file.getUserData<String>(AndroidConst.ANDROID_USER_PACKAGE)
        if (androidPackage == null) return null

        val retType = c.typeMapper.mapType(propertyDescriptor.getReturnType()!!)

        val extensionReceiver = resolvedCall.getExtensionReceiver()
        val declarationDescriptor = extensionReceiver.getType().getConstructor().getDeclarationDescriptor()

        if (declarationDescriptor == null) return null

        val supportsCache = declarationDescriptor.getSource() is KotlinSourceElement

        val androidClassType = getClassType(declarationDescriptor)
        if (supportsCache && androidClassType.supportsCache) {
            val className = DescriptorUtils.getFqName(declarationDescriptor).toString()
            val bytecodeClassName = className.replace('.', '/')

            receiver.put(Type.getType("L$bytecodeClassName;"), c.v)
            c.v.getstatic(androidPackage.replace(".", "/") + "/R\$id", propertyDescriptor.getName().asString(), "I")
            c.v.invokevirtual(bytecodeClassName, METHOD_NAME, "(I)Landroid/view/View;", false)
        } else {
            when (androidClassType) {
                AndroidClassType.ACTIVITY, AndroidClassType.VIEW -> {
                    receiver.put(Type.getType("L${androidClassType.internalClassName};"), c.v)
                    c.v.getstatic(androidPackage.replace(".", "/") + "/R\$id", propertyDescriptor.getName().asString(), "I")
                    c.v.invokevirtual(androidClassType.internalClassName, "findViewById", "(I)Landroid/view/View;", false)
                }
                AndroidClassType.FRAGMENT -> {
                    receiver.put(Type.getType("L${androidClassType.internalClassName};"), c.v)
                    c.v.invokevirtual(androidClassType.internalClassName, "getView", "()Landroid/view/View;", false)
                    c.v.getstatic(androidPackage.replace(".", "/") + "/R\$id", propertyDescriptor.getName().asString(), "I")
                    c.v.invokevirtual("android/view/View", "findViewById", "(I)Landroid/view/View;", false)
                }
                else -> return null
            }


        }

        c.v.checkcast(retType)
        return StackValue.onStack(retType)
    }

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
        } else if (descriptor is LazyClassDescriptor) { // For tests (FakeActivity)
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

        val classType = state.getTypeMapper().mapClass(descriptor)
        val className = classType.getInternalName()

        val viewType = Type.getObjectType("android/view/View")

        classBuilder.newField(org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin.Default.NO_ORIGIN, ACC_PRIVATE, PROPERTY_NAME, "Ljava/util/HashMap;", null, null)

        val methodVisitor = classBuilder.newMethod(
                org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin.Default.NO_ORIGIN, ACC_PUBLIC, METHOD_NAME, "(I)Landroid/view/View;", null, null)
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

        FunctionCodegen.endVisit(methodVisitor, METHOD_NAME, classOrObject)
    }
}