/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.jps.build;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ObjectUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.builders.BuildTarget;
import org.jetbrains.jps.builders.java.dependencyView.Mappings;
import org.jetbrains.jps.builders.storage.SourceToOutputMapping;
import org.jetbrains.jps.cmdline.ProjectDescriptor;
import org.jetbrains.jps.incremental.MessageHandler;
import org.jetbrains.jps.incremental.messages.BuildMessage;
import org.jetbrains.jps.incremental.messages.DoneSomethingNotification;
import org.jetbrains.jps.incremental.storage.OutputToTargetRegistry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.*;

public final class BuildResult implements MessageHandler {
  private final List<BuildMessage> myErrorMessages;
  private final List<BuildMessage> myWarnMessages;
  private final List<BuildMessage> myInfoMessages;
  private boolean myUpToDate = true;
  private String myMappingsDump;

  public BuildResult() {
    myErrorMessages = new ArrayList<>();
    myWarnMessages = new ArrayList<>();
    myInfoMessages = new ArrayList<>();
  }

  void storeMappingsDump(ProjectDescriptor pd) throws IOException {
    final ByteArrayOutputStream dump = new ByteArrayOutputStream();

    try (PrintStream stream = new PrintStream(dump)) {
      Mappings mappings = pd.dataManager.getMappings();
      if (mappings != null) {
        mappings.toStream(stream);
      }
      dumpSourceToOutputMappings(pd, stream);
    }

    dump.close();
    myMappingsDump = dump.toString();
  }

  private static void dumpSourceToOutputMappings(ProjectDescriptor pd, PrintStream stream) throws IOException {
    List<BuildTarget<?>> targets = new ArrayList<>(pd.getBuildTargetIndex().getAllTargets());
    targets.sort((o1, o2) -> {
      return StringUtil.comparePairs(o1.getTargetType().getTypeId(), o1.getId(), o2.getTargetType().getTypeId(), o2.getId(), false);
    });
    final Int2ObjectMap<BuildTarget<?>> id2Target = new Int2ObjectOpenHashMap<>();
    for (BuildTarget<?> target : targets) {
      id2Target.put(pd.dataManager.getTargetsState().getBuildTargetId(target), target);
    }
    Int2ObjectMap<String> hashCodeToOutputPath = new Int2ObjectOpenHashMap<>();
    for (BuildTarget<?> target : targets) {
      stream.println("Begin Of SourceToOutput (target " + getTargetIdWithTypeId(target) + ")");
      SourceToOutputMapping map = pd.dataManager.getSourceToOutputMap(target);
      List<String> sourcesList = new ArrayList<>(map.getSources());
      Collections.sort(sourcesList);
      for (String source : sourcesList) {
        List<String> outputs = new ArrayList<>(ObjectUtils.notNull(map.getOutputs(source), Collections.emptySet()));
        Collections.sort(outputs);
        for (String output : outputs) {
          hashCodeToOutputPath.put(FileUtil.pathHashCode(output), output);
        }
        String sourceToCompare = SystemInfo.isFileSystemCaseSensitive ? source : source.toLowerCase(Locale.US);
        stream.println(" " + sourceToCompare + " -> " + StringUtil.join(outputs, ","));
      }
      stream.println("End Of SourceToOutput (target " + getTargetIdWithTypeId(target) + ")");
    }


    OutputToTargetRegistry registry = pd.dataManager.getOutputToTargetRegistry();
    List<Integer> keys = new ArrayList<>(registry.getKeys());
    Collections.sort(keys);
    stream.println("Begin Of OutputToTarget");
    for (Integer key : keys) {
      IntSet targetsIds = registry.getState(key);
      if (targetsIds == null) continue;
      final List<String> targetsNames = new ArrayList<>();
      targetsIds.forEach(value -> {
        BuildTarget<?> target = id2Target.get(value);
        targetsNames.add(target != null ? getTargetIdWithTypeId(target) : "<unknown " + value + ">");
      });
      Collections.sort(targetsNames);
      stream.println(hashCodeToOutputPath.get(key.intValue()) + " -> " + targetsNames);
    }
    stream.println("End Of OutputToTarget");
  }

  @NotNull
  private static String getTargetIdWithTypeId(BuildTarget<?> target) {
    return target.getTargetType().getTypeId() + ":" + target.getId();
  }

  @Override
  public void processMessage(BuildMessage msg) {
    if (msg.getKind() == BuildMessage.Kind.ERROR) {
      myErrorMessages.add(msg);
      myUpToDate = false;
    }
    else if (msg.getKind() == BuildMessage.Kind.WARNING) {
      myWarnMessages.add(msg);
    }
    else {
      myInfoMessages.add(msg);
    }
    if (msg instanceof DoneSomethingNotification) {
      myUpToDate = false;
    }
  }

  public String getMappingsDump() {
    return myMappingsDump;
  }

  public void assertUpToDate() {
    assertTrue("Project sources weren't up to date", myUpToDate);
  }

  public void assertFailed() {
    assertFalse("Build not failed as expected", isSuccessful());
  }

  public boolean isSuccessful() {
    return myErrorMessages.isEmpty();
  }

  public void assertSuccessful() {
    if (!isSuccessful()) {
      fail("Build failed.\n" +
           "Errors:\n" + StringUtil.join(myErrorMessages, "\n") + "\n" +
           "Info messages:\n" + StringUtil.join(myInfoMessages, "\n"));
    }
  }

  @NotNull
  public List<BuildMessage> getMessages(@NotNull BuildMessage.Kind kind) {
    if (kind == BuildMessage.Kind.ERROR) return myErrorMessages;
    else if (kind == BuildMessage.Kind.WARNING) return myWarnMessages;
    else return myInfoMessages;
  }
}
