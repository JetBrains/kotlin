/*
 * Copyright (C) 2013 The Android Open Source Project
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
package org.jetbrains.android.inspections.klint;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.klint.checks.ApiDetector;
import com.android.tools.klint.checks.ApiLookup;
import com.android.tools.klint.client.api.UastLintUtils;
import com.android.tools.klint.detector.api.*;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.util.MethodSignatureUtil;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.*;
import org.jetbrains.uast.expressions.UTypeReferenceExpression;
import org.jetbrains.uast.util.UastExpressionUtils;
import org.jetbrains.uast.visitor.AbstractUastVisitor;
import org.jetbrains.uast.visitor.UastVisitor;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import static org.jetbrains.android.inspections.klint.IntellijLintUtils.SUPPRESS_LINT_FQCN;
import static org.jetbrains.android.inspections.klint.IntellijLintUtils.SUPPRESS_WARNINGS_FQCN;

/**
 * Intellij-specific version of the {@link ApiDetector} which uses the PSI structure
 * to check accesses
 * <p>
 * TODO:
 * <ul>
 *   <li> Port this part from the bytecode based check:
 * if (owner.equals("java/text/SimpleDateFormat")) {
 *   checkSimpleDateFormat(context, method, node, minSdk);
 * }
 *   </li>
 *   <li>Compare to the bytecode based results</li>
 * </ul>
 */
public class IntellijApiDetector extends ApiDetector {
  @SuppressWarnings("unchecked")
  public static final Implementation IMPLEMENTATION = new Implementation(
          IntellijApiDetector.class,
          EnumSet.of(Scope.RESOURCE_FILE, Scope.MANIFEST, Scope.JAVA_FILE),
          Scope.MANIFEST_SCOPE,
          Scope.RESOURCE_FILE_SCOPE,
          Scope.JAVA_FILE_SCOPE
  );

  @NonNls
  private static final String TARGET_API_FQCN = "android.annotation.TargetApi";

  @Override
  public List<Class<? extends UElement>> getApplicableUastTypes() {
    return Collections.<Class<? extends UElement>>singletonList(UFile.class);
  }

  // TODO: Reuse the parent ApiVisitor to share more code between these two!
  @Override
  public UastVisitor createUastVisitor(@NonNull final JavaContext context) {
    return new AbstractUastVisitor() {
      @Override
      public boolean visitFile(@NotNull UFile file) {
        List<UClass> classes = file.getClasses();
        if (!classes.isEmpty()) {
          // TODO: This is weird; I should just perform the per class checks as part of visitClass!!
          file.accept(new ApiCheckVisitor(context, classes.get(0), file));
        }
        return super.visitFile(file);
      }
    };
  }

  private static int getTargetApi(@NonNull UElement e, @NonNull UFile file) {
    UElement element = e;
    // Search upwards for target api annotations
    while (element != null && element != file) {
      if (element instanceof UAnnotated) {
        UAnnotated owner = (UAnnotated)element;
        UAnnotation annotation = owner.findAnnotation(TARGET_API_FQCN);
        if (annotation == null) {
          annotation = owner.findAnnotation(REQUIRES_API_ANNOTATION);
        }
        if (annotation != null) {
          for (UNamedExpression pair : annotation.getAttributeValues()) {
            UExpression v = pair.getExpression();

            if (v instanceof ULiteralExpression) {
              ULiteralExpression literal = (ULiteralExpression)v;
              Object value = literal.getValue();
              if (value instanceof Integer) {
                return (Integer) value;
              } else if (value instanceof String) {
                return codeNameToApi((String) value);
              }
            } else if (UastExpressionUtils.isArrayInitializer(v)) {
              UCallExpression mv = (UCallExpression)v;
              for (UExpression mmv : mv.getValueArguments()) {
                if (mmv instanceof ULiteralExpression) {
                  ULiteralExpression literal = (ULiteralExpression)mmv;
                  Object value = literal.getValue();
                  if (value instanceof Integer) {
                    return (Integer) value;
                  } else if (value instanceof String) {
                    return codeNameToApi((String) value);
                  }
                }
              }
            } else if (v instanceof UResolvable) {
              PsiElement resolved = ((UResolvable) v).resolve();
              if (resolved != null) {
                String fqcn = UastLintUtils.getQualifiedName(resolved);
                return codeNameToApi(fqcn);
              } else {
                return codeNameToApi(v.asRenderString());
              }
            }
          }
        }
      }
      element = element.getContainingElement();
    }

    return -1;
  }

  private class ApiCheckVisitor extends AbstractUastVisitor {
    private final JavaContext myContext;
    private boolean mySeenSuppress;
    private boolean mySeenTargetApi;
    private final UClass myClass;
    private final UFile myFile;
    private final boolean myCheckAccess;
    private boolean myCheckOverride;
    private String myFrameworkParent;

    public ApiCheckVisitor(JavaContext context, UClass clz, UFile file) {
      myContext = context;
      myClass = clz;
      myFile = file;

      myCheckAccess = context.isEnabled(UNSUPPORTED) || context.isEnabled(INLINED);
      myCheckOverride = context.isEnabled(OVERRIDE)
                        && context.getMainProject().getBuildSdk() >= 1;
      int depth = 0;
      if (myCheckOverride) {
        myFrameworkParent = null;
        UClass superClass = myClass.getUastSuperClass();
        while (superClass != null) {
          String fqcn = superClass.getQualifiedName();
          if (fqcn == null) {
            myCheckOverride = false;
          } else if (fqcn.startsWith("android.") //$NON-NLS-1$
                     || fqcn.startsWith("java.")        //$NON-NLS-1$
                     || fqcn.startsWith("javax.")) {    //$NON-NLS-1$
            if (!fqcn.equals(CommonClassNames.JAVA_LANG_OBJECT)) {
              myFrameworkParent = ClassContext.getInternalName(fqcn);
            }
            break;
          }
          superClass = superClass.getUastSuperClass();
          depth++;
          if (depth == 500) {
            // Shouldn't happen in practice; this prevents the IDE from
            // hanging if the user has accidentally typed in an incorrect
            // super class which creates a cycle.
            break;
          }
        }
        if (myFrameworkParent == null) {
          myCheckOverride = false;
        }
      }
    }

    @Override
    public boolean visitAnnotation(@NotNull UAnnotation annotation) {
      String fqcn = annotation.getQualifiedName();
      if (TARGET_API_FQCN.equals(fqcn) || REQUIRES_API_ANNOTATION.equals(fqcn)) {
        mySeenTargetApi = true;
      }
      else if (SUPPRESS_LINT_FQCN.equals(fqcn) || SUPPRESS_WARNINGS_FQCN.equals(fqcn)) {
        mySeenSuppress = true;
      }

      return super.visitAnnotation(annotation);
    }

    @Override
    public boolean visitMethod(@NotNull UMethod method) {

      // API check for default methods
      if (method.getModifierList().hasExplicitModifier(PsiModifier.DEFAULT)) {
        int api = 24; // minSdk for default methods
        int minSdk = getMinSdk(myContext);

        if (!isSuppressed(api, method, minSdk)) {
          Location location = IntellijLintUtils.getUastLocation(myContext.file, method);
          String message = String.format("Default method requires API level %1$d (current min is %2$d)", api, minSdk);
          myContext.report(UNSUPPORTED, location, message);
        }
      }

      if (!myCheckOverride) {
        return super.visitMethod(method);
      }

      int buildSdk = myContext.getMainProject().getBuildSdk();
      String name = method.getName();
      assert myFrameworkParent != null;
      String desc = IntellijLintUtils.getInternalDescription(method, false, false);
      if (desc == null) {
        // Couldn't compute description of method for some reason; probably
        // failure to resolve parameter types
        return super.visitMethod(method);
      }
      int api = mApiDatabase.getCallVersion(myFrameworkParent, name, desc);
      if (api > buildSdk && buildSdk != -1) {
        if (mySeenSuppress &&
            IntellijLintUtils.isSuppressed(method, myFile.getPsi(), OVERRIDE)) {
          return super.visitMethod(method);
        }

        // TODO: Don't complain if it's annotated with @Override; that means
        // somehow the build target isn't correct.

        String fqcn;
        PsiClass containingClass = method.getContainingClass();
        if (containingClass != null) {
          String className = containingClass.getName();
          String fullClassName = containingClass.getQualifiedName();
          if (fullClassName != null) {
            className = fullClassName;
          }
          fqcn = className + '#' + name;
        } else {
          fqcn = name;
        }

        String message = String.format(
                "This method is not overriding anything with the current build " +
                "target, but will in API level %1$d (current target is %2$d): %3$s",
                api, buildSdk, fqcn);

        PsiElement locationNode = method.getNameIdentifier();
        if (locationNode == null) {
          locationNode = method;
        }
        Location location = IntellijLintUtils.getLocation(myContext.file, locationNode);
        myContext.report(OVERRIDE, location, message);
      }

      return super.visitMethod(method);
    }

    @Override
    public boolean visitClass(@NotNull UClass aClass) {
      if (!myCheckAccess) {
        return super.visitClass(aClass);
      }

      if (aClass.isAnnotationType()) {
        PsiModifierList modifierList = aClass.getModifierList();
        if (modifierList != null) {
          for (PsiAnnotation annotation : modifierList.getAnnotations()) {
            String name = annotation.getQualifiedName();
            if ("java.lang.annotation.Repeatable".equals(name)) {
              int api = 24; // minSdk for repeatable annotations
              int minSdk = getMinSdk(myContext);
              if (!isSuppressed(api, aClass, minSdk)) {
                Location location = IntellijLintUtils.getLocation(myContext.file, annotation);
                String message = String.format("Repeatable annotation requires API level %1$d (current min is %2$d)", api, minSdk);
                myContext.report(UNSUPPORTED, location, message);
              }
            }
          }
        }
      }

      for (PsiClassType type : aClass.getSuperTypes()) {
        String signature = IntellijLintUtils.getInternalName(type);
        if (signature == null) {
          continue;
        }

        int api = mApiDatabase.getClassVersion(signature);
        if (api == -1) {
          continue;
        }
        int minSdk = getMinSdk(myContext);
        if (api <= minSdk) {
          continue;
        }
        if (mySeenTargetApi) {
          int target = getTargetApi(aClass, myFile);
          if (target != -1) {
            if (api <= target) {
              continue;
            }
          }
        }
        if (mySeenSuppress && IntellijLintUtils.isSuppressed(aClass, myFile.getPsi(), UNSUPPORTED)) {
          continue;
        }

        Location location;
        if (type instanceof PsiClassReferenceType) {
          PsiReference reference = ((PsiClassReferenceType)type).getReference();
          PsiElement element = reference.getElement();
          if (isWithinVersionCheckConditional(aClass, api, myContext)) {
            continue;
          }
          if (isPrecededByVersionCheckExit(aClass, api, myContext)) {
            continue;
          }
          location = IntellijLintUtils.getLocation(myContext.file, element);
        } else {
          location = IntellijLintUtils.getLocation(myContext.file, aClass); //TODO to super type reference
        }
        String fqcn = type.getClassName();
        String message = String.format("Class requires API level %1$d (current min is %2$d): %3$s", api, minSdk, fqcn);
        myContext.report(UNSUPPORTED, location, message);
      }

      return super.visitClass(aClass);
    }

    @Override
    public boolean visitSimpleNameReferenceExpression(@NotNull USimpleNameReferenceExpression expression) {
      if (!myCheckAccess) {
        return super.visitSimpleNameReferenceExpression(expression);
      }

      PsiElement resolved = expression.resolve();
      if (resolved != null) {
        if (resolved instanceof PsiField) {
          PsiField field = (PsiField)resolved;
          PsiClass containingClass = field.getContainingClass();
          if (containingClass == null) {
            return super.visitSimpleNameReferenceExpression(expression);
          }
          String owner = IntellijLintUtils.getInternalName(containingClass);
          if (owner == null) {
            return super.visitSimpleNameReferenceExpression(expression); // Couldn't resolve type
          }
          String name = field.getName();
          if (name == null) {
            return super.visitSimpleNameReferenceExpression(expression);
          }

          int api = mApiDatabase.getFieldVersion(owner, name);
          if (api == -1) {
            return super.visitSimpleNameReferenceExpression(expression);
          }
          int minSdk = getMinSdk(myContext);
          if (isSuppressed(api, expression, minSdk)) {
            return super.visitSimpleNameReferenceExpression(expression);
          }

          Location location = IntellijLintUtils.getUastLocation(myContext.file, expression);
          String fqcn = containingClass.getQualifiedName();
          String message = String.format(
                  "Field requires API level %1$d (current min is %2$d): %3$s",
                  api, minSdk, fqcn + '#' + name);

          Issue issue = UNSUPPORTED;
          // When accessing primitive types or Strings, the values get copied into
          // the class files (e.g. get inlined) which has a separate issue type:
          // INLINED.
          PsiType type = field.getType();
          if (PsiType.INT.equals(type) || PsiType.CHAR.equals(type) || PsiType.BOOLEAN.equals(type)
              || PsiType.DOUBLE.equals(type) || PsiType.FLOAT.equals(type) || PsiType.BYTE.equals(type)
              || type.equalsToText(CommonClassNames.JAVA_LANG_STRING)) {
            issue = INLINED;

            // Some usages of inlined constants are okay:
            if (isBenignConstantUsage(expression, name, owner)) {
              return super.visitSimpleNameReferenceExpression(expression);
            }
          }

          myContext.report(issue, location, message);
        }
      }

      return super.visitSimpleNameReferenceExpression(expression);
    }

    @Override
    public boolean visitTryExpression(@NotNull UTryExpression statement) {
      if (statement.isResources()) {
        int api = 19; // minSdk for try with resources
        int minSdk = getMinSdk(myContext);

        if (isSuppressed(api, statement, minSdk)) {
          return super.visitTryExpression(statement);
        }
        Location location = IntellijLintUtils.getUastLocation(myContext.file, statement.getTryClause());
        String message = String.format("Try-with-resources requires API level %1$d (current min is %2$d)", api, minSdk);
        myContext.report(UNSUPPORTED, location, message);
      }

      for (UCatchClause catchClause : statement.getCatchClauses()) {

        // Special case reflective operation exception which can be implicitly used
        // with multi-catches: see issue 153406
        int minSdk = getMinSdk(myContext);
        if(minSdk < 19 && isMultiCatchReflectiveOperationException(catchClause)) {
          String message = String.format("Multi-catch with these reflection exceptions requires API level 19 (current min is %d) " +
                                         "because they get compiled to the common but new super type `ReflectiveOperationException`. " +
                                         "As a workaround either create individual catch statements, or catch `Exception`.",
                                         minSdk);

          myContext.report(UNSUPPORTED, getCatchParametersLocation(myContext, catchClause), message);
          continue;
        }

        for (UTypeReferenceExpression typeReference : catchClause.getTypeReferences()) {
          checkCatchTypeElement(statement, typeReference, typeReference.getType());
        }
      }

      return super.visitTryExpression(statement);
    }

    private void checkCatchTypeElement(@NonNull UTryExpression statement,
            @NonNull UTypeReferenceExpression typeReference,
            @Nullable PsiType type) {
      PsiClass resolved = null;
      if (type instanceof PsiClassType) {
        resolved = ((PsiClassType) type).resolve();
      }
      if (resolved != null) {
        String signature = myContext.getEvaluator().getInternalName(resolved);
        int api = mApiDatabase.getClassVersion(signature);
        if (api == -1) {
          return;
        }
        int minSdk = getMinSdk(myContext);
        if (api <= minSdk) {
          return;
        }
        int target = getTargetApi(statement);
        if (target != -1 && api <= target) {
          return;
        }

        Location location = myContext.getUastLocation(typeReference);
        String fqcn = resolved.getQualifiedName();
        String message = String.format("Class requires API level %1$d (current min is %2$d): %3$s",
                                       api, minSdk, fqcn);
        myContext.report(UNSUPPORTED, location, message);
      }
    }

    private boolean isSuppressed(int api, UElement element, int minSdk) {
      if (api <= minSdk) {
        return true;
      }
      if (mySeenTargetApi) {
        int target = getTargetApi(element, myFile);
        if (target != -1) {
          if (api <= target) {
            return true;
          }
        }
      }
      if (mySeenSuppress &&
          (IntellijLintUtils.isSuppressed(element, myFile, UNSUPPORTED) || IntellijLintUtils.isSuppressed(element, myFile, INLINED))) {
        return true;
      }

      if (isWithinVersionCheckConditional(element, api, myContext)) {
        return true;
      }
      if (isPrecededByVersionCheckExit(element, api, myContext)) {
        return true;
      }

      return false;
    }

    @Override
    public boolean visitBinaryExpressionWithType(@NotNull UBinaryExpressionWithType expression) {
      if (myCheckAccess) {
        UExpression operand = expression.getOperand();
        PsiType operandType = operand.getExpressionType();
        PsiType castType = expression.getType();
        if (castType.equals(operandType)) {
          return super.visitBinaryExpressionWithType(expression);
        }
        if (!(operandType instanceof PsiClassType)) {
          return super.visitBinaryExpressionWithType(expression);
        }
        if (!(castType instanceof PsiClassType)) {
          return super.visitBinaryExpressionWithType(expression);
        }
        PsiClassType classType = (PsiClassType)operandType;
        PsiClassType interfaceType = (PsiClassType)castType;
        checkCast(expression, classType, interfaceType);
      }

      return super.visitBinaryExpressionWithType(expression);
    }

    private void checkCast(@NotNull UElement node, @NotNull PsiClassType classType, @NotNull PsiClassType interfaceType) {
      if (classType.equals(interfaceType)) {
        return;
      }
      String classTypeInternal = IntellijLintUtils.getInternalName(classType);
      String interfaceTypeInternal = IntellijLintUtils.getInternalName(interfaceType);
      if (classTypeInternal == null || interfaceTypeInternal == null || "java/lang/Object".equals(interfaceTypeInternal)) {
        return; // Couldn't resolve type
      }

      int api = mApiDatabase.getValidCastVersion(classTypeInternal, interfaceTypeInternal);
      if (api == -1) {
        return;
      }

      int minSdk = getMinSdk(myContext);
      if (api <= minSdk) {
        return;
      }

      if (isSuppressed(api, node, minSdk)) {
        return;
      }

      Location location = IntellijLintUtils.getUastLocation(myContext.file, node);
      String message = String.format("Cast from %1$s to %2$s requires API level %3$d (current min is %4$d)",
                                     classType.getClassName(), interfaceType.getClassName(), api, minSdk);
      myContext.report(UNSUPPORTED, location, message);
    }

    @Override
    public boolean visitVariable(@NotNull UVariable variable) {
      if (variable instanceof ULocalVariable) {
        if (!myCheckAccess) {
          return super.visitVariable(variable);
        }

        UExpression initializer = variable.getUastInitializer();
        if (initializer == null) {
          return super.visitVariable(variable);
        }

        PsiType initializerType = initializer.getExpressionType();
        if (initializerType == null || !(initializerType instanceof PsiClassType)) {
          return super.visitVariable(variable);
        }

        PsiType interfaceType = variable.getType();
        if (initializerType.equals(interfaceType)) {
          return super.visitVariable(variable);
        }

        if (!(interfaceType instanceof PsiClassType)) {
          return super.visitVariable(variable);
        }

        checkCast(initializer, (PsiClassType)initializerType, (PsiClassType)interfaceType);
      }

      return super.visitVariable(variable);
    }

    @Override
    public boolean visitBinaryExpression(@NotNull UBinaryExpression expression) {
      if (expression.getOperator() instanceof UastBinaryOperator.AssignOperator) {
        if (!myCheckAccess) {
          return super.visitBinaryExpression(expression);
        }

        UExpression rExpression = expression.getRightOperand();
        PsiType rhsType = rExpression.getExpressionType();
        if (rhsType == null || !(rhsType instanceof PsiClassType)) {
          return super.visitBinaryExpression(expression);
        }

        PsiType interfaceType = expression.getLeftOperand().getExpressionType();
        if (rhsType.equals(interfaceType)) {
          return super.visitBinaryExpression(expression);
        }

        if (!(interfaceType instanceof PsiClassType)) {
          return super.visitBinaryExpression(expression);
        }

        checkCast(rExpression, (PsiClassType)rhsType, (PsiClassType)interfaceType);
      }
      
      return super.visitBinaryExpression(expression);
    }

    @Override
    public boolean visitForEachExpression(@NotNull UForEachExpression statement) {
      // The for each method will implicitly call iterator() on the
      // Iterable that is used in the for each loop; make sure that
      // the API level for that
      if (!myCheckAccess) {
        return super.visitForEachExpression(statement);
      }

      UExpression value = statement.getIteratedValue();
      PsiType type = value.getExpressionType();

      if (type instanceof PsiClassType) {
        String expressionOwner = IntellijLintUtils.getInternalName((PsiClassType)type);
        if (expressionOwner != null) {
          int api = mApiDatabase.getClassVersion(expressionOwner);
          if (api == -1) {
            return super.visitForEachExpression(statement);
          }
          int minSdk = getMinSdk(myContext);
          if (api <= minSdk) {
            return super.visitForEachExpression(statement);
          }
          if (mySeenTargetApi) {
            int target = getTargetApi(statement, myFile);
            if (target != -1) {
              if (api <= target) {
                return super.visitForEachExpression(statement);
              }
            }
          }
          if (mySeenSuppress && IntellijLintUtils.isSuppressed(statement, myFile, UNSUPPORTED)) {
            return super.visitForEachExpression(statement);
          }

          if (isWithinVersionCheckConditional(statement, api, myContext)) {
            return super.visitForEachExpression(statement);
          }
          if (isPrecededByVersionCheckExit(statement, api, myContext)) {
            return super.visitForEachExpression(statement);
          }

          Location location = IntellijLintUtils.getUastLocation(myContext.file, value);
          String message = String.format("The type of the for loop iterated value is %1$s, which requires API level %2$d" +
                                         " (current min is %3$d)", type.getCanonicalText(), api, minSdk);

          // Add specific check ConcurrentHashMap#keySet and add workaround text.
          // This was an unfortunate incompatible API change in Open JDK 8, which is
          // not an issue for the Android SDK but is relevant if you're using a
          // Java library.
          if (value instanceof PsiMethodCallExpression) {
            PsiMethodCallExpression valueCall = (PsiMethodCallExpression)value;
            if ("keySet".equals(valueCall.getMethodExpression().getReferenceName())) {
              PsiMethod keySet = valueCall.resolveMethod();
              if (keySet != null && keySet.getContainingClass() != null &&
                  "java.util.concurrent.ConcurrentHashMap".equals(keySet.getContainingClass().getQualifiedName())) {
                message += "; to work around this, add an explicit cast to (Map) before the `keySet` call.";
              }
            }
          }
          myContext.report(UNSUPPORTED, location, message);
        }
      }
      
      return super.visitForEachExpression(statement);
    }

    @Override
    public boolean visitCallExpression(@NotNull UCallExpression node) {
      checkMethodCallExpression(node);
      return super.visitCallExpression(node);
    }

    private void checkMethodCallExpression(@NotNull UCallExpression expression) {
      super.visitCallExpression(expression);

      if (!myCheckAccess) {
        return;
      }

      PsiMethod method = expression.resolve();
      if (method != null) {
        PsiClass containingClass = method.getContainingClass();
        if (containingClass == null) {
          return;
        }

        PsiParameterList parameterList = method.getParameterList();
        if (parameterList.getParametersCount() > 0) {
          PsiParameter[] parameters = parameterList.getParameters();

          List<UExpression> arguments = expression.getValueArguments();
          for (int i = 0; i < parameters.length; i++) {
            PsiType parameterType = parameters[i].getType();
            if (parameterType instanceof PsiClassType) {
              if (i >= arguments.size()) {
                // We can end up with more arguments than parameters when
                // there is a varargs call.
                break;
              }
              UExpression argument = arguments.get(i);
              PsiType argumentType = argument.getExpressionType();
              if (argumentType == null || parameterType.equals(argumentType) || !(argumentType instanceof PsiClassType)) {
                continue;
              }
              checkCast(argument, (PsiClassType)argumentType, (PsiClassType)parameterType);
            }
          }
        }

        String fqcn = containingClass.getQualifiedName();
        String owner = IntellijLintUtils.getInternalName(containingClass);
        if (owner == null) {
          return; // Couldn't resolve type
        }
        String name = IntellijLintUtils.getInternalMethodName(method);
        String desc = IntellijLintUtils.getInternalDescription(method, false, false);
        if (desc == null) {
          // Couldn't compute description of method for some reason; probably
          // failure to resolve parameter types
          return;
        }

        int api = mApiDatabase.getCallVersion(owner, name, desc);
        if (api == -1) {
          return;
        }
        int minSdk = getMinSdk(myContext);
        if (api <= minSdk) {
          return;
        }

        // The lint API database contains two optimizations:
        // First, all members that were available in API 1 are omitted from the database, since that saves
        // about half of the size of the database, and for API check purposes, we don't need to distinguish
        // between "doesn't exist" and "available in all versions".
        // Second, all inherited members were inlined into each class, so that it doesn't have to do a
        // repeated search up the inheritance chain.
        //
        // Unfortunately, in this custom PSI detector, we look up the real resolved method, which can sometimes
        // have a different minimum API.
        //
        // For example, SQLiteDatabase had a close() method from API 1. Therefore, calling SQLiteDatabase is supported
        // in all versions. However, it extends SQLiteClosable, which in API 16 added "implements Closable". In
        // this detector, if we have the following code:
        //     void test(SQLiteDatabase db) { db.close }
        // here the call expression will be the close method on type SQLiteClosable. And that will result in an API
        // requirement of API 16, since the close method it now resolves to is in API 16.
        //
        // To work around this, we can now look up the type of the call expression ("db" in the above, but it could
        // have been more complicated), and if that's a different type than the type of the method, we look up
        // *that* method from lint's database instead. Furthermore, it's possible for that method to return "-1"
        // and we can't tell if that means "doesn't exist" or "present in API 1", we then check the package prefix
        // to see whether we know it's an API method whose members should all have been inlined.
        if (UastExpressionUtils.isMethodCall(expression)) {
          UExpression qualifier = expression.getReceiver();
          if (qualifier != null && !(qualifier instanceof UThisExpression) && !(qualifier instanceof USuperExpression)) {
            PsiType type = qualifier.getExpressionType();
            if (type != null && type instanceof PsiClassType) {
              String expressionOwner = IntellijLintUtils.getInternalName((PsiClassType)type);
              if (expressionOwner != null && !expressionOwner.equals(owner)) {
                int specificApi = mApiDatabase.getCallVersion(expressionOwner, name, desc);
                if (specificApi == -1) {
                  if (ApiLookup.isRelevantOwner(expressionOwner)) {
                    return;
                  }
                } else if (specificApi <= minSdk) {
                  return;
                } else {
                  // For example, for Bundle#getString(String,String) the API level is 12, whereas for
                  // BaseBundle#getString(String,String) the API level is 21. If the code specified a Bundle instead of
                  // a BaseBundle, reported the Bundle level in the error message instead.
                  if (specificApi < api) {
                    api = specificApi;
                    fqcn = expressionOwner.replace('/', '.');
                  }
                  api = Math.min(specificApi, api);
                }
              }
            }
          } else {
            // Unqualified call; need to search in our super hierarchy
            PsiClass cls = UastUtils.getContainingClass(expression);

            //noinspection ConstantConditions
            if (qualifier instanceof UThisExpression || qualifier instanceof USuperExpression) {
              UInstanceExpression pte = (UInstanceExpression) qualifier;
              PsiElement resolved = pte.resolve();
              if (resolved instanceof PsiClass) {
                cls = (PsiClass)resolved;
              }
            }

            while (cls != null) {
              if (cls instanceof PsiAnonymousClass) {
                // If it's an unqualified call in an anonymous class, we need to rely on the
                // resolve method to find out whether the method is picked up from the anonymous
                // class chain or any outer classes
                boolean found = false;
                PsiClassType anonymousBaseType = ((PsiAnonymousClass)cls).getBaseClassType();
                PsiClass anonymousBase = anonymousBaseType.resolve();
                if (anonymousBase != null && anonymousBase.isInheritor(containingClass, true)) {
                  cls = anonymousBase;
                  found = true;
                } else {
                  PsiClass surroundingBaseType = PsiTreeUtil.getParentOfType(cls, PsiClass.class, true);
                  if (surroundingBaseType != null && surroundingBaseType.isInheritor(containingClass, true)) {
                    cls = surroundingBaseType;
                    found = true;
                  }
                }
                if (!found) {
                  break;
                }
              }
              String expressionOwner = IntellijLintUtils.getInternalName(cls);
              if (expressionOwner == null) {
                break;
              }
              int specificApi = mApiDatabase.getCallVersion(expressionOwner, name, desc);
              if (specificApi == -1) {
                if (ApiLookup.isRelevantOwner(expressionOwner)) {
                  return;
                }
              } else if (specificApi <= minSdk) {
                return;
              } else {
                if (specificApi < api) {
                  api = specificApi;
                  fqcn = expressionOwner.replace('/', '.');
                }
                api = Math.min(specificApi, api);
                break;
              }
              cls = cls.getSuperClass();
            }
          }
        }

        if (isSuppressed(api, expression, minSdk)) {
          return;
        }

        // If you're simply calling super.X from method X, even if method X is in a higher API level than the minSdk, we're
        // generally safe; that method should only be called by the framework on the right API levels. (There is a danger of
        // somebody calling that method locally in other contexts, but this is hopefully unlikely.)
        if (UastExpressionUtils.isMethodCall(expression)) {
          if (expression.getReceiver() instanceof USuperExpression) {
            PsiMethod containingMethod = UastUtils.getContainingMethod(expression);
            if (containingMethod != null && name.equals(containingMethod.getName())
                && MethodSignatureUtil.areSignaturesEqual(method, containingMethod)
                // We specifically exclude constructors from this check, because we do want to flag constructors requiring the
                // new API level; it's highly likely that the constructor is called by local code so you should specifically
                // investigate this as a developer
                && !method.isConstructor()) {
              return;
            }
          }
        }

        UElement locationNode = expression.getMethodIdentifier();
        if (locationNode == null) {
          locationNode = expression;
        }
        Location location = IntellijLintUtils.getUastLocation(myContext.file, locationNode);
        String message = String.format("Call requires API level %1$d (current min is %2$d): %3$s", api, minSdk,
                                       fqcn + '#' + method.getName());

        myContext.report(UNSUPPORTED, location, message);
      }
    }
  }
}
