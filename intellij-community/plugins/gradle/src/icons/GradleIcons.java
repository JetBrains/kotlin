// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package icons;

import com.intellij.ui.IconManager;

import javax.swing.*;

import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;

/**
 * NOTE THIS FILE IS AUTO-GENERATED
 * DO NOT EDIT IT BY HAND, run "Generate icon classes" configuration instead
 */
public final class GradleIcons {
  private static Icon load(String path) {
    return IconManager.getInstance().getIcon(path, GradleIcons.class);
  }

  private static Icon load(String path, Class<?> clazz) {
    return IconManager.getInstance().getIcon(path, clazz);
  }

  /** 16x16 */ public static final Icon Gradle = load("/icons/gradle.svg");
  /** 16x16 */ public static final Icon GradleFile = load("/icons/gradleFile.svg");
  /** 16x16 */ public static final Icon GradleNavigate = load("/icons/gradleNavigate.svg");
  /** 13x13 */ public static final Icon ToolWindowGradle = load("/icons/toolWindowGradle.svg");

  /** @deprecated to be removed in IDEA 2020 */
  @SuppressWarnings("unused")
  @Deprecated
  @ScheduledForRemoval(inVersion = "2020.1")
  public static final Icon GradleImport = load("/icons/gradleImport.png");

  /** @deprecated to be removed in IDEA 2020 - use GradleIcons.Gradle */
  @SuppressWarnings("unused")
  @Deprecated
  @ScheduledForRemoval(inVersion = "2020.1")
  public static final Icon GradlePlugin = GradleIcons.Gradle;

  /** @deprecated to be removed in IDEA 2020 */
  @SuppressWarnings("unused")
  @Deprecated
  @ScheduledForRemoval(inVersion = "2020.1")
  public static final Icon GradleSync = load("/icons/gradleSync.png");

  /** @deprecated to be removed in IDEA 2020 - use AllIcons.Actions.OfflineMode */
  @SuppressWarnings("unused")
  @Deprecated
  @ScheduledForRemoval(inVersion = "2020.1")
  public static final Icon OfflineMode = load("/actions/offlineMode.svg", com.intellij.icons.AllIcons.class);
}
