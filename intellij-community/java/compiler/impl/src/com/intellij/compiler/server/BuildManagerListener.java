package com.intellij.compiler.server;

import com.intellij.openapi.project.Project;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * @author Eugene Zhuravlev
 */
public interface BuildManagerListener {
  Topic<BuildManagerListener> TOPIC = Topic.create("Build Manager", BuildManagerListener.class);

  default void beforeBuildProcessStarted(@NotNull Project project, @NotNull UUID sessionId) {}

  default void buildStarted(@NotNull Project project, @NotNull UUID sessionId, boolean isAutomake) {}

  default void buildFinished(@NotNull Project project, @NotNull UUID sessionId, boolean isAutomake) {}
}
