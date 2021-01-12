// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.serialization.internal.adapter;

import org.gradle.internal.impldep.com.google.common.base.Objects;
import org.gradle.tooling.model.idea.IdeaLanguageLevel;

public class InternalIdeaLanguageLevel implements IdeaLanguageLevel {
  private final String level;

  public InternalIdeaLanguageLevel(String level) {
    this.level = level;
  }

  public boolean isJDK_1_4() {
    return "JDK_1_4".equals(this.level);
  }

  public boolean isJDK_1_5() {
    return "JDK_1_5".equals(this.level);
  }

  public boolean isJDK_1_6() {
    return "JDK_1_6".equals(this.level);
  }

  public boolean isJDK_1_7() {
    return "JDK_1_7".equals(this.level);
  }

  public boolean isJDK_1_8() {
    return "JDK_1_8".equals(this.level);
  }

  @Override
  public String getLevel() {
    return this.level;
  }

  public String toString() {
    return "IdeaLanguageLevel{level='" + this.level + "'}";
  }

  public boolean equals(Object o) {
    return this == o || o instanceof InternalIdeaLanguageLevel &&
                        Objects.equal(this.level, ((InternalIdeaLanguageLevel)o).level);
  }

  public int hashCode() {
    return this.level != null ? this.level.hashCode() : 0;
  }
}
