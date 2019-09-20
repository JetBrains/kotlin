// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.chainsSearch.context;

import com.intellij.compiler.CompilerReferenceService;
import com.intellij.compiler.backwardRefs.CompilerReferenceServiceEx;
import com.intellij.compiler.chainsSearch.MethodCall;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.*;
import com.intellij.psi.scope.ElementClassHint;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.scope.util.PsiScopesUtil;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.ClassUtil;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.FactoryMap;
import gnu.trove.THashSet;
import gnu.trove.TIntObjectHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.backwardRefs.CompilerRef;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ChainCompletionContext {
  private static final String[] WIDELY_USED_CLASS_NAMES = new String [] {CommonClassNames.JAVA_LANG_STRING,
                                                                  CommonClassNames.JAVA_LANG_OBJECT,
                                                                  CommonClassNames.JAVA_LANG_CLASS};
  private static final Set<String> WIDELY_USED_SHORT_NAMES = ContainerUtil.set("String", "Object", "Class");

  @NotNull
  private final ChainSearchTarget myTarget;
  @NotNull
  private final List<PsiNamedElement> myContextElements;
  @NotNull
  private final PsiElement myContext;
  @NotNull
  private final GlobalSearchScope myResolveScope;
  @NotNull
  private final Project myProject;
  @NotNull
  private final PsiResolveHelper myResolveHelper;
  @NotNull
  private final TIntObjectHashMap<PsiClass> myQualifierClassResolver;
  @NotNull
  private final Map<MethodCall, PsiMethod[]> myResolver;
  @NotNull
  private final CompilerReferenceServiceEx myRefServiceEx;

  private final NotNullLazyValue<Set<CompilerRef>> myContextClassReferences = new NotNullLazyValue<Set<CompilerRef>>() {
    @NotNull
    @Override
    protected Set<CompilerRef> compute() {
      return getContextTypes()
        .stream()
        .map(PsiUtil::resolveClassInType)
        .filter(Objects::nonNull)
        .map(c -> ClassUtil.getJVMClassName(c))
        .filter(Objects::nonNull)
        .mapToInt(c -> myRefServiceEx.getNameId(c))
        .filter(n -> n != 0)
        .mapToObj(n -> new CompilerRef.JavaCompilerClassRef(n)).collect(Collectors.toSet());
    }
  };

  public ChainCompletionContext(@NotNull ChainSearchTarget target,
                                @NotNull List<PsiNamedElement> contextElements,
                                @NotNull PsiElement context) {
    myTarget = target;
    myContextElements = contextElements;
    myContext = context;
    myResolveScope = context.getResolveScope();
    myProject = context.getProject();
    myResolveHelper = PsiResolveHelper.SERVICE.getInstance(myProject);
    myQualifierClassResolver = new TIntObjectHashMap<>();
    myResolver = FactoryMap.create(sign -> sign.resolve());
    myRefServiceEx = (CompilerReferenceServiceEx)CompilerReferenceService.getInstance(myProject);
  }

  @NotNull
  public ChainSearchTarget getTarget() {
    return myTarget;
  }

  public boolean contains(@Nullable PsiType type) {
    if (type == null) return false;
    Set<PsiType> types = getContextTypes();
    if (types.contains(type)) return true;
    for (PsiType contextType : types) {
      if (type.isAssignableFrom(contextType)) {
        return true;
      }
    }
    return false;
  }

  @NotNull
  public CompilerReferenceServiceEx getRefService() {
    return myRefServiceEx;
  }

  @NotNull
  public PsiElement getContextPsi() {
    return myContext;
  }

  public PsiFile getContextFile() {
    return myContext.getContainingFile();
  }

  @NotNull
  public Set<PsiType> getContextTypes() {
    return myContextElements.stream().map(ChainCompletionContext::getType).collect(Collectors.toSet());
  }

  @NotNull
  public Set<CompilerRef> getContextClassReferences() {
    return myContextClassReferences.getValue();
  }

  @NotNull
  public GlobalSearchScope getResolveScope() {
    return myResolveScope;
  }

  @NotNull
  public Project getProject() {
    return myProject;
  }

  public boolean hasQualifier(@Nullable PsiClass targetClass) {
    return getQualifiers(targetClass).findAny().isPresent();
  }

  public Stream<PsiNamedElement> getQualifiers(@Nullable PsiClass targetClass) {
    if (targetClass == null) return Stream.empty();
    return getQualifiers(JavaPsiFacade.getElementFactory(myProject).createType(targetClass));
  }

  public Stream<PsiNamedElement> getQualifiers(@NotNull PsiType targetType) {
    return myContextElements.stream().filter(e -> {
      PsiType elementType = getType(e);
      return elementType != null && targetType.isAssignableFrom(elementType);
    });
  }

  @Nullable
  public PsiClass resolvePsiClass(CompilerRef.CompilerClassHierarchyElementDef aClass) {
    int nameId = aClass.getName();
    if (myQualifierClassResolver.contains(nameId)) {
      return myQualifierClassResolver.get(nameId);
    } else {
      PsiClass psiClass = null;
      String name = myRefServiceEx.getName(nameId);
      PsiClass resolvedClass = JavaPsiFacade.getInstance(getProject()).findClass(name, myResolveScope);
      if (resolvedClass != null && accessValidator().test(resolvedClass)) {
        psiClass = resolvedClass;
      }
      myQualifierClassResolver.put(nameId, psiClass);
      return psiClass;
    }
  }

  @NotNull
  public PsiMethod[] resolve(MethodCall sign) {
    return myResolver.get(sign);
  }

  public Predicate<PsiMember> accessValidator() {
    return m -> myResolveHelper.isAccessible(m, myContext, null);
  }

  @Nullable
  public static ChainCompletionContext createContext(@Nullable PsiType targetType,
                                                     @Nullable PsiElement containingElement, boolean suggestIterators) {
    if (containingElement == null) return null;
    ChainSearchTarget target = ChainSearchTarget.create(targetType);
    if (target == null) return null;
    if (suggestIterators) {
      target = target.toIterators();
    }

    Set<? extends PsiVariable> excludedVariables = getEnclosingLocalVariables(containingElement);
    ContextProcessor processor = new ContextProcessor(null, containingElement.getProject(), containingElement, excludedVariables);
    PsiScopesUtil.treeWalkUp(processor, containingElement, containingElement.getContainingFile());
    List<PsiNamedElement> contextElements = processor.getContextElements();

    return new ChainCompletionContext(target, contextElements, containingElement);
  }

  @NotNull
  private static Set<? extends PsiVariable> getEnclosingLocalVariables(@NotNull PsiElement place) {
    Set<PsiLocalVariable> result = new THashSet<>();
    if (place instanceof PsiLocalVariable) result.add((PsiLocalVariable)place);
    PsiElement parent = place.getParent();
    while (parent != null) {
      if (parent instanceof PsiFileSystemItem) break;
      if (parent instanceof PsiLocalVariable && PsiTreeUtil.isAncestor(((PsiLocalVariable)parent).getInitializer(), place, false)) {
        result.add((PsiLocalVariable)parent);
      }
      parent = parent.getParent();
    }
    return result;
  }

  private static class ContextProcessor implements PsiScopeProcessor, ElementClassHint {
    private final List<PsiNamedElement> myContextElements = new SmartList<>();
    private final PsiVariable myCompletionVariable;
    private final PsiResolveHelper myResolveHelper;
    private final PsiElement myPlace;
    private final Set<? extends PsiVariable> myExcludedVariables;

    private ContextProcessor(@Nullable PsiVariable variable,
                             @NotNull Project project,
                             @NotNull PsiElement place,
                             @NotNull Set<? extends PsiVariable> excludedVariables) {
      myCompletionVariable = variable;
      myResolveHelper = PsiResolveHelper.SERVICE.getInstance(project);
      myPlace = place;
      myExcludedVariables = excludedVariables;
    }

    @Override
    public boolean shouldProcess(@NotNull DeclarationKind kind) {
      return kind == DeclarationKind.ENUM_CONST ||
             kind == DeclarationKind.FIELD ||
             kind == DeclarationKind.METHOD ||
             kind == DeclarationKind.VARIABLE;
    }

    @Override
    public boolean execute(@NotNull PsiElement element, @NotNull ResolveState state) {
      if ((!(element instanceof PsiMethod) || PropertyUtilBase.isSimplePropertyAccessor((PsiMethod)element)) &&
          (!(element instanceof PsiVariable) || !myExcludedVariables.contains(element)) &&
          (!(element instanceof PsiMember) || myResolveHelper.isAccessible((PsiMember)element, myPlace, null))) {
        PsiType type = getType(element);
        if (type == null) {
          return true;
        }
        if (isWidelyUsed(type)) {
          return true;
        }
        myContextElements.add((PsiNamedElement)element);
      }
      return true;
    }

    @Override
    public <T> T getHint(@NotNull Key<T> hintKey) {
      if (hintKey == ElementClassHint.KEY) {
        //noinspection unchecked
        return (T)this;
      }
      return null;
    }

    @NotNull
    public List<PsiNamedElement> getContextElements() {
      myContextElements.remove(myCompletionVariable);
      return myContextElements;
    }
  }

  @Nullable
  private static PsiType getType(PsiElement element) {
    if (element instanceof PsiVariable) {
      return ((PsiVariable)element).getType();
    }
    if (element instanceof PsiMethod) {
      return ((PsiMethod)element).getReturnType();
    }
    return null;
  }

  public static boolean isWidelyUsed(@NotNull PsiType type) {
    type = type.getDeepComponentType();
    if (type instanceof PsiPrimitiveType) return true;
    if (!(type instanceof PsiClassType)) return false;
    if (WIDELY_USED_SHORT_NAMES.contains(((PsiClassType)type).getClassName())) return false;
    final PsiClass resolvedClass = ((PsiClassType)type).resolve();
    if (resolvedClass == null) return false;
    final String qName = resolvedClass.getQualifiedName();
    if (qName == null) return false;
    for (String name : WIDELY_USED_CLASS_NAMES) {
      if (name.equals(qName)) {
        return true;
      }
    }
    return false;
  }
}
