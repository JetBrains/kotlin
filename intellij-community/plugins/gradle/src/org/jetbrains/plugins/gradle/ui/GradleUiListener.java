package org.jetbrains.plugins.gradle.ui;

import com.intellij.util.messages.Topic;

/**
 * Defines various callbacks for the gradle integration UI processing.
 * <p/>
 * Implementations of this interface are not obliged to be thread-safe.
 * 
 * @author Denis Zhdanov
 */
public interface GradleUiListener {

  Topic<GradleUiListener> TOPIC = Topic.create("Gradle UI", GradleUiListener.class);
  
  /**
   * Is called before conflict changes UI is shown.
   */
  void beforeConflictUiShown();
  
  void afterConflictUiShown();
}
