/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.compiler.backwardRefs;

import com.intellij.compiler.CompilerReferenceService;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFunctionalExpression;
import com.intellij.psi.impl.source.Constants;
import com.intellij.psi.impl.source.PsiFileWithStubSupport;
import com.intellij.psi.impl.source.StubbedSpine;
import com.intellij.psi.impl.source.tree.JavaElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.TIntHashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class JavaCompilerElementRetriever {
  private static final Logger LOG = Logger.getInstance(JavaCompilerElementRetriever.class);

  private final static TokenSet FUN_EXPR = TokenSet.create(JavaElementType.LAMBDA_EXPRESSION, JavaElementType.METHOD_REF_EXPRESSION);

  @NotNull
  static PsiFunctionalExpression[] retrieveFunExpressionsByIndices(@NotNull TIntHashSet indices,
                                                                   @NotNull PsiFileWithStubSupport psiFile) {
    StubbedSpine spine = psiFile.getStubbedSpine();

    PsiFunctionalExpression[] result = new PsiFunctionalExpression[indices.size()];
    int resIdx = 0;
    int funExprIdx = 0;
    for (int i = 0; i < spine.getStubCount(); i++) {
      if (FUN_EXPR.contains(spine.getStubType(i)) && indices.contains(funExprIdx++)) {
        result[resIdx++] = (PsiFunctionalExpression)spine.getStubPsi(i);
      }
    }

    if (result.length != resIdx) {
      final CompilerReferenceServiceImpl compilerReferenceService =
        (CompilerReferenceServiceImpl)CompilerReferenceService.getInstance(psiFile.getProject());
      final Set<Module> state = compilerReferenceService.getDirtyScopeHolder().getAllDirtyModules();
      final VirtualFile file = psiFile.getVirtualFile();
      final Module moduleForFile = ProjectFileIndex.getInstance(psiFile.getProject()).getModuleForFile(file);
      LOG.error("Compiler functional expression index doesn't match to stub index.\n" +
                "Functional expression indices: " + indices + "\n" +
                "Does the file belong to dirty scope?: " + state.contains(moduleForFile),
                new Attachment(psiFile.getName(), psiFile.getText()));

      return ContainerUtil.filter(result, Objects::nonNull).toArray(PsiFunctionalExpression.EMPTY_ARRAY);
    }

    return result;
  }

  @NotNull
  static PsiClass[] retrieveClassesByInternalIds(@NotNull SearchId[] internalIds,
                                                 @NotNull PsiFileWithStubSupport psiFile) {
    ClassMatcher matcher = ClassMatcher.create(internalIds);
    return ReadAction.compute(() -> matcher.retrieveClasses(psiFile));
  }

  private interface InternalNameMatcher {
    boolean matches(PsiClass psiClass);

    class ByName implements InternalNameMatcher {
      private final String myName;

      public ByName(String name) {myName = name;}

      @Override
      public boolean matches(PsiClass psiClass) {
        return myName.equals(psiClass.getName());
      }
    }

    class ByQualifiedName implements InternalNameMatcher {
      private final String myQName;

      public ByQualifiedName(String name) {myQName = name;}

      @Override
      public boolean matches(PsiClass psiClass) {
        return myQName.equals(psiClass.getQualifiedName());
      }
    }
  }

  private static class ClassMatcher {
    @Nullable
    private final TIntHashSet myAnonymousIndices;
    @NotNull
    private final Collection<? extends InternalNameMatcher> myClassNameMatchers;

    private ClassMatcher(@Nullable TIntHashSet anonymousIndices,
                         @NotNull Collection<? extends InternalNameMatcher> nameMatchers) {
      myAnonymousIndices = anonymousIndices;
      myClassNameMatchers = nameMatchers;
    }


    private PsiClass[] retrieveClasses(PsiFileWithStubSupport file) {
      StubbedSpine spine = file.getStubbedSpine();

      List<PsiClass> result = new ArrayList<>(myClassNameMatchers.size() + (myAnonymousIndices == null ? 0 : myAnonymousIndices.size()));
      int anonymousId = 0;
      for (int i = 0; i < spine.getStubCount(); i++) {
        if (Constants.CLASS_BIT_SET.contains(spine.getStubType(i))) {
          PsiClass element = (PsiClass)spine.getStubPsi(i);
          if (element instanceof PsiAnonymousClass) {
            if (myAnonymousIndices != null && !myAnonymousIndices.isEmpty()) {
              if (myAnonymousIndices.contains(anonymousId)) {
                result.add(element);
              }
              anonymousId++;
            }
          }
          else if (match(element, myClassNameMatchers)) {
            result.add(element);
          }
        }
      }
      return result.toArray(PsiClass.EMPTY_ARRAY);
    }

    private static boolean match(PsiClass psiClass, Collection<? extends InternalNameMatcher> matchers) {
      for (InternalNameMatcher matcher : matchers) {
        if (matcher.matches(psiClass)) {
          //qualified name is unique among file's classes
          if (matcher instanceof InternalNameMatcher.ByQualifiedName) {
            matchers.remove(matcher);
          }
          return true;
        }
      }
      return false;
    }

    private static ClassMatcher create(@NotNull SearchId[] internalIds) {
      List<InternalNameMatcher> nameMatchers = new SmartList<>();
      TIntHashSet anonymousIndices = null;
      for (SearchId internalId : internalIds) {
        if (internalId.getId() != -1) {
          if (anonymousIndices == null) {
            anonymousIndices = new TIntHashSet();
          }
          anonymousIndices.add(internalId.getId());
        }
        else {
          String internalName = internalId.getDeserializedName();
          int curLast = internalName.length() - 1;
          while (true) {
            int lastIndex = internalName.lastIndexOf('$', curLast);
            if (lastIndex > -1 && lastIndex < internalName.length() - 1) {
              final int followingIndex = lastIndex + 1;
              final boolean digit = Character.isDigit(internalName.charAt(followingIndex));
              if (digit) {
                if (curLast == internalName.length() - 1) {
                  final int nextNonDigit = getNextNonDigitIndex(internalName, followingIndex);
                  if (nextNonDigit != -1) {
                    //declared inside method
                    nameMatchers.add(new InternalNameMatcher.ByName(internalName.substring(nextNonDigit)));
                  }
                  else {
                    throw new IllegalStateException();
                  }
                }
                else {
                  //declared in anonymous
                  nameMatchers.add(new InternalNameMatcher.ByName(StringUtil.getShortName(internalName, '$')));
                  break;
                }
              }
            }
            else {
              nameMatchers.add(new InternalNameMatcher.ByQualifiedName(StringUtil.replace(internalName, "$", ".")));
              break;
            }
            curLast = lastIndex - 1;
          }
        }
      }
      return new ClassMatcher(anonymousIndices, nameMatchers);
    }

    private static int getNextNonDigitIndex(String name, int digitIndex) {
      for (int i = digitIndex + 1; i < name.length(); i++) {
        if (!Character.isDigit(name.charAt(i))) {
          return i;
        }
      }
      return -1;
    }
  }
}
