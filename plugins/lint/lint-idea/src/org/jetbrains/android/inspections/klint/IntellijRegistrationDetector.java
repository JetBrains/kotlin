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

import com.android.tools.klint.checks.RegistrationDetector;
import com.android.tools.klint.detector.api.ClassContext;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Location;
import com.android.tools.klint.detector.api.Scope;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UFile;
import org.jetbrains.uast.UastModifier;
import org.jetbrains.uast.UastUtils;
import org.jetbrains.uast.check.UastAndroidContext;
import org.jetbrains.uast.check.UastAndroidUtils;
import org.jetbrains.uast.check.UastScanner;
import org.jetbrains.uast.kinds.UastClassKind;
import org.jetbrains.uast.visitor.UastVisitor;

import java.util.Collection;
import java.util.EnumSet;

import static com.android.SdkConstants.*;

/**
 * Intellij-specific version of the {@link RegistrationDetector} which uses the PSI structure
 * to check classes.
 * <p>
 * <ul>
 *   <li>Unit tests, and compare to the bytecode based results</li>
 * </ul>
 */
public class IntellijRegistrationDetector extends RegistrationDetector implements UastScanner {
  static final Implementation IMPLEMENTATION = new Implementation(
    IntellijRegistrationDetector.class,
    EnumSet.of(Scope.MANIFEST, Scope.SOURCE_FILE));

  @Override
  public UastVisitor createUastVisitor(final UastAndroidContext context) {
    return new UastVisitor() {
      @Override
      public boolean visitFile(@NotNull UFile file) {
        check(context, file);
        return true;
      }
    };
  }

  private void check(final UastAndroidContext context, final UFile file) {
    ApplicationManager.getApplication().runReadAction(new Runnable() {
      @Override
      public void run() {
        for (UClass clz : file.getClasses()) {
          check(context, clz);
        }
      }
    });
  }

  private void check(UastAndroidContext context, UClass clz) {
    for (UClass current = clz.getSuperClass(context); current != null; current = current.getSuperClass(context)) {
      // Ignore abstract classes
      if (clz.hasModifier(UastModifier.ABSTRACT) || clz.getKind() != UastClassKind.CLASS) {
        continue;
      }
      String fqcn = current.getFqName();
      if (fqcn == null) {
        continue;
      }
      if (CLASS_ACTIVITY.equals(fqcn)
          || CLASS_SERVICE.equals(fqcn)
          || CLASS_CONTENTPROVIDER.equals(fqcn)) {

        String internalName = clz.getInternalName();
        if (internalName == null) {
          internalName = IntellijLintUtils.getInternalName(clz);
        }
        if (internalName == null) {
          continue;
        }
        String frameworkClass = ClassContext.getInternalName(fqcn);
        Collection<String> registered = mManifestRegistrations != null ? mManifestRegistrations.get(frameworkClass) : null;
        if (registered == null || !registered.contains(internalName)) {
          report(context, clz, frameworkClass);
        }
        break;
      }
    }

    for (UClass innerClass : clz.getNestedClasses()) {
      check(context, innerClass);
    }
  }

  private static void report(UastAndroidContext context, UClass clz, String internalName) {
    // Unlike the superclass, we don't have to check that the tags are compatible;
    // IDEA already checks that as part of its XML validation

    if (IntellijLintUtils.isSuppressed(clz, UastUtils.getContainingFile(clz), ISSUE)) {
      return;
    }
    String tag = classToTag(internalName);
    Location location = UastAndroidUtils.getLocation(clz);
    String fqcn = clz.getFqName();
    if (fqcn == null) {
      fqcn = clz.getName();
    }
    context.report(ISSUE, clz, location, String.format("The <%1$s> %2$s is not registered in the manifest", tag, fqcn));
  }
}
