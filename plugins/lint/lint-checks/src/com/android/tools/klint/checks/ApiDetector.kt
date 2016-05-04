/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tools.klint.checks

import com.android.SdkConstants.TARGET_API
import com.android.annotations.NonNull
import com.android.sdklib.SdkVersionInfo
import com.android.tools.klint.client.api.IssueRegistry
import com.android.tools.klint.detector.api.*
import org.jetbrains.uast.*
import org.jetbrains.uast.check.UastAndroidContext
import org.jetbrains.uast.check.UastScanner
import org.jetbrains.uast.visitor.AbstractUastVisitor
import org.jetbrains.uast.visitor.UastVisitor
import java.util.*

/**
 * Looks for usages of APIs that are not supported in all the versions targeted
 * by this application (according to its minimum API requirement in the manifest).
 */
open class ApiDetector : Detector(), UastScanner {

    protected var mApiDatabase: ApiLookup? = null
    private var mWarnedMissingDb: Boolean = false

    @NonNull
    override fun getSpeed(): Speed {
        return Speed.SLOW
    }

    override fun beforeCheckProject(@NonNull context: Context) {
        mApiDatabase = ApiLookup.get(context.client)

        if (mApiDatabase == null && !mWarnedMissingDb) {
            mWarnedMissingDb = true
            context.report(IssueRegistry.LINT_ERROR, Location.create(context.file),
                           "Can't find API database; API check not performed")
        }
    }

    override fun createUastVisitor(context: UastAndroidContext): UastVisitor {
        return ApiVersionVisitor(context)
    }

    private inner class ApiVersionVisitor(val context: UastAndroidContext) : AbstractUastVisitor() {
        private var mMinApi = -1

        override fun visitCallExpression(node: UCallExpression): Boolean {
            when (node.kind) {
                UastCallKind.FUNCTION_CALL -> checkVersion(context, node, node.functionReference?.resolve(context))
                UastCallKind.CONSTRUCTOR_CALL -> checkVersion(context, node, node.classReference?.resolve(context))
            }

            return super.visitCallExpression(node)
        }

        override fun visitSimpleReferenceExpression(node: USimpleReferenceExpression): Boolean {
            checkVersion(context, node, node.resolve(context) as? UVariable)
            return super.visitSimpleReferenceExpression(node)
        }

        override fun visitClass(node: UClass): Boolean {
            val nameElement = node.nameElement

            if (nameElement != null) {
                for (type in node.superTypes) {
                    checkVersion(context, nameElement, type.resolveClass(context))
                }
            }

            return super.visitClass(node)
        }

        override fun visitClassLiteralExpression(node: UClassLiteralExpression): Boolean {
            val clazz = node.type?.resolve(context)
            if (clazz != null) {
                checkVersion(context, node, clazz)
            }
            return super.visitClassLiteralExpression(node)
        }

        override fun visitFunction(node: UFunction): Boolean {
            if (!node.hasModifier(UastModifier.OVERRIDE)) {
                val parentClass = node.parent as? UClass ?: return false

                val db = mApiDatabase ?: return false
                val desc = node.bytecodeDescriptor ?: return false

                val buildSdk = context.lintContext.getMainProject().getBuildSdk()
                if (buildSdk == -1) return false

                for (type in parentClass.superTypes) {
                    val clazz = type.resolve(context) ?: continue
                    if (!isSdkClass(clazz)) continue
                    val internalName = clazz.internalName ?: continue

                    val methodSdkLevel = db.getCallVersion(internalName, node.name, desc)
                    if (methodSdkLevel != -1 && methodSdkLevel > buildSdk) {
                        val message = "This method is not overriding anything with the current build " +
                                "target, but will in API level $methodSdkLevel (current target is $buildSdk): `${node.name}`"
                        context.report(OVERRIDE, node, context.getLocation(node.nameElement), message)
                    }
                }
            }

            return false
        }

        private fun checkIfSpecialCase(declaration: UDeclaration, owner: UClass): Boolean {
            if (declaration is UVariable && owner.fqName == "android.os.Build.VERSION_CODES") return true
            if (declaration.name == "MATCH_PARENT" && owner.fqName == "android.view.ViewGroup.LayoutParams") return true
            if ((declaration.name == "CHOICE_MODE_NONE" || declaration.name == "CHOICE_MODE_MULTIPLE"
                || declaration.name == "CHOICE_MODE_SINGLE") && owner.fqName == "android.widget.AbsListView") return true
            if ((declaration.name == "START" || declaration.name == "END") && owner.fqName == "android.view.Gravity") return true

            return false
        }

        private fun checkVersion(context: UastAndroidContext, node: UElement, declaration: UDeclaration?) {
            if (declaration == null) return
            val db = mApiDatabase ?: return

            val projectMinSdk = getMinSdk(context)
            if (projectMinSdk == -1) return

            fun check(declarationSdkLevel: Int, parentClass: UClass?) {
                if (declarationSdkLevel == -1) return
                if (parentClass != null && checkIfSpecialCase(declaration, parentClass)) return
                if (isCheckedExplicitly(context, declarationSdkLevel, node)) return

                if (projectMinSdk < declarationSdkLevel) {
                    val subject = when (declaration) {
                        is UFunction -> "Call"
                        is UVariable -> "Field"
                        is UClass -> "Class"
                        else -> "Call"
                    }
                    val message = "$subject requires API level $declarationSdkLevel" +
                                  " (current min is $projectMinSdk): `${declaration.name}`"
                    context.report(UNSUPPORTED, node, context.getLocation(node), message)
                    return
                }
            }

            fun checkAosp(clazz: UClass) = AOSP_BUILD && clazz.fqName?.startsWith("android.support.") ?: false

            if (declaration is UClass) {
                if (!isSdkClass(declaration) || checkAosp(declaration)) return
                val internalName = declaration.internalName ?: return
                check(db.getClassVersion(internalName), null)
            }

            val parentClass = declaration.parent as? UClass ?: return
            if (!isSdkClass(parentClass) || checkAosp(parentClass)) return
            val parentInternalName = parentClass.internalName ?: return

            when (declaration) {
                is UFunction -> {
                    val descriptor = declaration.bytecodeDescriptor ?: return
                    check(db.getCallVersion(parentInternalName, declaration.name, descriptor), parentClass)
                }
                is UVariable -> {
                    if (declaration.kind != UastVariableKind.MEMBER) return
                    check(db.getFieldVersion(parentInternalName, declaration.name), parentClass)
                }
            }
        }

        private fun isSdkClass(clazz: UClass): Boolean {
            val fqName = clazz.fqName ?: return false
            return fqName.startsWith("android.")
                   || fqName.startsWith("java.")
                   || fqName.startsWith("javax.")
                   || fqName.startsWith("dalvik.")
        }

        private fun getMinSdk(context: UastAndroidContext): Int {
            if (mMinApi == -1) {
                val minSdkVersion = context.lintContext.mainProject.minSdkVersion
                mMinApi = minSdkVersion.featureLevel
            }

            return mMinApi
        }
    }

    companion object {
        private val AOSP_BUILD = System.getenv("ANDROID_BUILD_TOP") != null
        private val SDK_INT_CONTAINING = "android.os.Build.VERSION"
        private val SDK_INT = "SDK_INT"

        tailrec fun getLocalMinSdk(scope: UElement?): Int {
            if (scope == null) return -1

            if (scope is UAnnotated) {
                val targetApi = getTargetApi(scope.annotations)
                if (targetApi != -1) return targetApi
            }

            return getLocalMinSdk(scope.parent)
        }

        fun getTargetApi(annotations: List<UAnnotation>): Int {
            for (annotation in annotations) {
                if (annotation.matchesName(TARGET_API)) {
                    for (element in annotation.valueArguments) {
                        val valueNode = element.expression
                        if (valueNode.isIntegralLiteral()) {
                            return (valueNode as ULiteralExpression).getLongValue().toInt()
                        } else if (valueNode.isStringLiteral()) {
                            val value = (valueNode as ULiteralExpression).value as String
                            return SdkVersionInfo.getApiByBuildCode(value, true)
                        } else if (valueNode is UQualifiedExpression) {
                            val codename = valueNode.getSelectorAsIdentifier() ?: return -1
                            return SdkVersionInfo.getApiByBuildCode(codename, true)
                        } else if (valueNode is USimpleReferenceExpression) {
                            val codename = valueNode.identifier
                            return SdkVersionInfo.getApiByBuildCode(codename, true)
                        }
                    }
                }
            }

            return -1
        }

        fun isCheckedExplicitly(context: UastAndroidContext, requiredVersion: Int, node: UElement): Boolean {
            tailrec fun UExpression.isSdkIntReference(): Boolean = when (this) {
                is UParenthesizedExpression -> expression.isSdkIntReference()
                is USimpleReferenceExpression -> resolve(context)?.matchesNameWithContaining(SDK_INT_CONTAINING, SDK_INT) ?: false
                is UQualifiedExpression -> resolve(context)?.matchesNameWithContaining(SDK_INT_CONTAINING, SDK_INT) ?: false
                else -> false
            }

            fun checkCondition(node: UExpression, invertCondition: Boolean = false): Boolean? {
                if (node is UBinaryExpression && node.operator is UastBinaryOperator.ComparationOperator) {
                    var invert: Boolean = invertCondition

                    val value: UExpression
                    if (node.leftOperand.isSdkIntReference()) {
                        value = node.rightOperand
                    }
                    else if (node.rightOperand.isSdkIntReference()) {
                        invert = !invert
                        value = node.leftOperand
                    }
                    else return false
                    fun inv(cond: Boolean) = if (invert) !cond else cond

                    tailrec fun evaluateValue(value: UExpression?): Int? {
                        if (value == null) return null

                        return (value.evaluate() as? Number)?.toInt() ?: if (value is UResolvable) {
                            val declaration = value.resolve(context) ?: return null
                            if (declaration is UVariable)
                                evaluateValue(declaration.initializer)
                            else
                                null
                        } else null
                    }

                    val sdkLevel = evaluateValue(value) ?: return null

                    return when (node.operator) {
                        UastBinaryOperator.GREATER -> inv(sdkLevel > requiredVersion)
                        UastBinaryOperator.GREATER_OR_EQUAL -> sdkLevel == requiredVersion || inv(sdkLevel > requiredVersion)
                        UastBinaryOperator.LESS -> inv(sdkLevel < requiredVersion)
                        UastBinaryOperator.LESS_OR_EQUAL -> sdkLevel == requiredVersion || inv(sdkLevel < requiredVersion)
                        UastBinaryOperator.EQUALS -> requiredVersion == sdkLevel
                        else -> null
                    }
                }

                return when (node) {
                    is UBinaryExpression -> if (node.operator is UastBinaryOperator.LogicalOperator) {
                        checkCondition(node.leftOperand) ?: checkCondition(node.rightOperand)
                    } else null
                    is UUnaryExpression -> if (node.operator == UastPrefixOperator.LOGICAL_NOT) {
                        checkCondition(node.operand, true)
                    } else null
                    is UParenthesizedExpression -> checkCondition(node.expression)
                    else -> null
                }
            }

            tailrec fun check(node: UElement?, prev: UElement?, context: UastAndroidContext): Boolean {
                return when (node) {
                    null -> false
                    is UIfExpression -> {
                        val cond = checkCondition(node.condition) ?: return false
                        if ((cond && prev == node.thenBranch) || (!cond && prev == node.elseBranch))
                            true
                        else
                            check(node.parent, node, context)
                    }
                    is USwitchClauseExpression -> {
                        if (node.caseValues?.any { checkCondition(it) == true } ?: false)
                            true
                        else
                            check(node.parent, node, context)
                    }
                    is UBinaryExpression -> {
                        if (prev == node.rightOperand
                                && node.operator is UastBinaryOperator.LogicalOperator
                                && checkCondition(node.leftOperand) == true)
                            true
                        else
                            check(node.parent, node, context)
                    }
                    is UFunction -> false
                    is UVariable -> if (node.kind == UastVariableKind.MEMBER) false else check(node.parent, node, context)
                    is UClass -> false
                    else -> check(node.parent, node, context)
                }
            }

            val minSdk = getLocalMinSdk(node)
            if (minSdk != -1 && minSdk >= requiredVersion) return true

            return check(node, null, context)
        }

        /** Accessing an unsupported API  */
        @SuppressWarnings("unchecked")
        @JvmField
        val UNSUPPORTED = Issue.create(
                "NewApi", //$NON-NLS-1$
                "Calling new methods on older versions",

                "This check scans through all the Android API calls in the application and " +
                "warns about any calls that are not available on *all* versions targeted " +
                "by this application (according to its minimum SDK attribute in the manifest).\n" +
                "\n" +
                "If you really want to use this API and don't need to support older devices just " +
                "set the `minSdkVersion` in your `build.gradle` or `AndroidManifest.xml` files.\n" +
                "\n" +
                "If your code is *deliberately* accessing newer APIs, and you have ensured " +
                "(e.g. with conditional execution) that this code will only ever be called on a " +
                "supported platform, then you can annotate your class or method with the " +
                "`@TargetApi` annotation specifying the local minimum SDK to apply, such as " +
                "`@TargetApi(11)`, such that this check considers 11 rather than your manifest " +
                "file's minimum SDK as the required API level.\n" +
                "\n" +
                "If you are deliberately setting `android:` attributes in style definitions, " +
                "make sure you place this in a `values-vNN` folder in order to avoid running " +
                "into runtime conflicts on certain devices where manufacturers have added " +
                "custom attributes whose ids conflict with the new ones on later platforms.\n" +
                "\n" +
                "Similarly, you can use tools:targetApi=\"11\" in an XML file to indicate that " +
                "the element will only be inflated in an adequate context.",
                Category.CORRECTNESS,
                6,
                Severity.ERROR,
                Implementation(
                        ApiDetector::class.java,
                        Scope.SOURCE_FILE_SCOPE))

        /** Accessing an inlined API on older platforms  */
        @JvmField
        val INLINED = Issue.create(
                "InlinedApi", //$NON-NLS-1$
                "Using inlined constants on older versions",

                "This check scans through all the Android API field references in the application " +
                "and flags certain constants, such as static final integers and Strings, " +
                "which were introduced in later versions. These will actually be copied " +
                "into the class files rather than being referenced, which means that " +
                "the value is available even when running on older devices. In some " +
                "cases that's fine, and in other cases it can result in a runtime " +
                "crash or incorrect behavior. It depends on the context, so consider " +
                "the code carefully and device whether it's safe and can be suppressed " +
                "or whether the code needs tbe guarded.\n" +
                "\n" +
                "If you really want to use this API and don't need to support older devices just " +
                "set the `minSdkVersion` in your `build.gradle` or `AndroidManifest.xml` files." +
                "\n" +
                "If your code is *deliberately* accessing newer APIs, and you have ensured " +
                "(e.g. with conditional execution) that this code will only ever be called on a " +
                "supported platform, then you can annotate your class or method with the " +
                "`@TargetApi` annotation specifying the local minimum SDK to apply, such as " +
                "`@TargetApi(11)`, such that this check considers 11 rather than your manifest " +
                "file's minimum SDK as the required API level.\n",
                Category.CORRECTNESS,
                6,
                Severity.WARNING,
                Implementation(
                        ApiDetector::class.java,
                        Scope.SOURCE_FILE_SCOPE))

        /** Accessing an unsupported API  */
        @JvmField
        val OVERRIDE = Issue.create(
                "Override", //$NON-NLS-1$
                "Method conflicts with new inherited method",

                "Suppose you are building against Android API 8, and you've subclassed Activity. " +
                "In your subclass you add a new method called `isDestroyed`(). At some later point, " +
                "a method of the same name and signature is added to Android. Your method will " +
                "now override the Android method, and possibly break its contract. Your method " +
                "is not calling `super.isDestroyed()`, since your compilation target doesn't " +
                "know about the method.\n" +
                "\n" +
                "The above scenario is what this lint detector looks for. The above example is " +
                "real, since `isDestroyed()` was added in API 17, but it will be true for *any* " +
                "method you have added to a subclass of an Android class where your build target " +
                "is lower than the version the method was introduced in.\n" +
                "\n" +
                "To fix this, either rename your method, or if you are really trying to augment " +
                "the builtin method if available, switch to a higher build target where you can " +
                "deliberately add `@Override` on your overriding method, and call `super` if " +
                "appropriate etc.\n",
                Category.CORRECTNESS,
                6,
                Severity.ERROR,
                Implementation(
                        ApiDetector::class.java,
                        Scope.SOURCE_FILE_SCOPE))
    }
}
