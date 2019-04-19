// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor;
import org.jetbrains.plugins.groovy.lang.resolve.delegatesTo.DelegatesToInfo;

import java.util.List;

/**
 * This interface narrows {@link NonCodeMembersContributor} to a closure executed inside particular method call at a gradle script.
 * <p/>
 * Example:
 * <b>build.gradle</b>
 * <pre>
 *   subprojects {
 *     repositories {
 *       mavenCentral()
 *     }
 *   }
 * </pre>
 * Here {@code 'subprojects'} should be resolved at context of a global script; {@code 'repositories'} in a context of
 * {@code 'subprojects'} and {@code 'mavenCentral'} in a context of {@code 'repositories'}. Every such context
 * is expected to be backed by corresponding implementation of the current interface.
 *
 * @author Denis Zhdanov
 */
public interface GradleMethodContextContributor {

  ExtensionPointName<GradleMethodContextContributor> EP_NAME =
    ExtensionPointName.create("org.jetbrains.plugins.gradle.resolve.contributor");

  /**
   * Tries to resolve target element.
   *
   * @param methodCallInfo information about method call hierarchy which points to the target place. Every entry is a method name
   *                       and the deepest one is assumed to be added the head
   * @param processor      the processor receiving the declarations.
   * @param state          current resolve state
   * @param place          the original element from which the tree up walk was initiated.
   * @return true if the processing should continue or false if it should be stopped.
   */
  default boolean process(@NotNull List<String> methodCallInfo,
                          @NotNull PsiScopeProcessor processor,
                          @NotNull ResolveState state,
                          @NotNull PsiElement place) {
    return true;
  }

  @Nullable
  default DelegatesToInfo getDelegatesToInfo(@NotNull GrClosableBlock closure) {
    return null;
  }
}
