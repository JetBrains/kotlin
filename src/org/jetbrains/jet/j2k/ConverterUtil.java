package org.jetbrains.jet.j2k;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;

/**
 * @author ignatov
 */
public class ConverterUtil {
  private ConverterUtil() {
  }

  @NotNull
  public static String createMainFunction(@NotNull PsiFile file) {
    List<Pair<String, PsiMethod>> classNamesWithMains = new LinkedList<Pair<String, PsiMethod>>();

    for (PsiClass c : ((PsiJavaFile) file).getClasses()) {
      PsiMethod main = findMainMethod(c);
      if (main != null) {
        classNamesWithMains.add(new Pair<String, PsiMethod>(c.getName(), main));
      }
    }
    if (classNamesWithMains.size() > 0) {
      String className = classNamesWithMains.get(0).getFirst();
      return MessageFormat.format("fun main(args : Array<String?>?) = {0}.main(args)", className);
    }
    return "";
  }

  @Nullable
  private static PsiMethod findMainMethod(@NotNull PsiClass aClass) {
    if (isMainClass(aClass)) {
      final PsiMethod[] mainMethods = aClass.findMethodsByName("main", false);
      return findMainMethod(mainMethods);
    }
    return null;
  }

  @Nullable
  private static PsiMethod findMainMethod(@NotNull PsiMethod[] mainMethods) {
    for (PsiMethod mainMethod : mainMethods) {
      if (isMainMethod(mainMethod)) return mainMethod;
    }
    return null;
  }

  private static boolean isMainClass(@NotNull PsiClass psiClass) {
    if (psiClass instanceof PsiAnonymousClass) return false;
    if (psiClass.isInterface()) return false;
    return psiClass.getContainingClass() == null || psiClass.hasModifierProperty(PsiModifier.STATIC);
  }

  public static boolean isMainMethod(@Nullable PsiMethod method) {
    if (method == null || method.getContainingClass() == null) return false;
    if (PsiType.VOID != method.getReturnType()) return false;
    if (!method.hasModifierProperty(PsiModifier.STATIC)) return false;
    if (!method.hasModifierProperty(PsiModifier.PUBLIC)) return false;
    final PsiParameter[] parameters = method.getParameterList().getParameters();
    if (parameters.length != 1) return false;
    final PsiType type = parameters[0].getType();
    if (!(type instanceof PsiArrayType)) return false;
    final PsiType componentType = ((PsiArrayType) type).getComponentType();
    return componentType.equalsToText("java.lang.String");
  }
}
