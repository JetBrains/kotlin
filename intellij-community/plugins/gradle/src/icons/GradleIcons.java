// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package icons;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

/**
 * NOTE THIS FILE IS AUTO-GENERATED
 * DO NOT EDIT IT BY HAND, run "Generate icon classes" configuration instead
 */
public final class GradleIcons {
  private static Icon load(String path) {
    return IconLoader.getIcon(path, GradleIcons.class);
  }

  private static Icon load(String path, Class<?> clazz) {
    return IconLoader.getIcon(path, clazz);
  }

  /** 16x16 */ public static final Icon Gradle = load("/icons/gradle.svg");
  /** 16x16 */ public static final Icon GradleFile = load("/icons/gradleFile.svg");
  /** 16x16 */ public static final Icon GradleNavigate = load("/icons/gradleNavigate.svg");
  /** 13x13 */ public static final Icon ToolWindowGradle = load("/icons/toolWindowGradle.svg");

  /** @deprecated to be removed in IDEA 2020 */
  @SuppressWarnings("unused")
  @Deprecated
  public static final Icon GradleImport = load("/icons/gradleImport.png");

  /** @deprecated to be removed in IDEA 2020 - use GradleIcons.Gradle */
  @SuppressWarnings("unused")
  @Deprecated
  public static final Icon GradlePlugin = GradleIcons.Gradle;

  /** @deprecated to be removed in IDEA 2020 */
  @SuppressWarnings("unused")
  @Deprecated
  public static final Icon GradleSync = load("/icons/gradleSync.png");

  /** @deprecated to be removed in IDEA 2020 - use AllIcons.Actions.OfflineMode */
  @SuppressWarnings("unused")
  @Deprecated
  public static final Icon OfflineMode = load("/actions/offlineMode.svg", com.intellij.icons.AllIcons.class);
}
