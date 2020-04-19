// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal;

import com.intellij.codeInspection.InspectionProfile;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;
import com.intellij.idea.ActionsBundle;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.profile.codeInspection.InspectionProfileManager;
import com.intellij.util.ResourceUtil;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public final class DumpInspectionDescriptionsAction extends AnAction implements DumbAware {
  private static final Logger LOG = Logger.getInstance(DumpInspectionDescriptionsAction.class);

  public DumpInspectionDescriptionsAction() {
    super(ActionsBundle.messagePointer("action.DumpInspectionDescriptionsAction.text"));
  }

  @Override
  public void actionPerformed(final @NotNull AnActionEvent event) {
    InspectionProfile profile = InspectionProfileManager.getInstance().getCurrentProfile();
    Collection<String> classes = new TreeSet<>();
    Map<String, Collection<String>> groups = new TreeMap<>();

    String tempDirectory = FileUtil.getTempDirectory();
    File descDirectory = new File(tempDirectory, "inspections");
    if (!descDirectory.mkdirs() && !descDirectory.isDirectory()) {
      LOG.error("Unable to create directory: " + descDirectory.getAbsolutePath());
      return;
    }

    for (InspectionToolWrapper<?, ?> toolWrapper : profile.getInspectionTools(null)) {
      classes.add(getInspectionClass(toolWrapper).getName());

      String group = getGroupName(toolWrapper);
      Collection<String> names = groups.get(group);
      if (names == null) groups.put(group, (names = new TreeSet<>()));
      names.add(toolWrapper.getShortName());

      final InputStream stream = getDescriptionStream(toolWrapper);
      if (stream != null) {
        doDump(new File(descDirectory, toolWrapper.getShortName() + ".html"), new Processor() {
          @Override public void process(BufferedWriter writer) throws Exception {
            writer.write(ResourceUtil.loadText(stream));
          }
        });
      }
    }
    doNotify("Inspection descriptions dumped to\n" + descDirectory.getAbsolutePath());

    File fqnListFile = new File(tempDirectory, "inspection_fqn_list.txt");
    boolean fqnOk = doDump(fqnListFile, new Processor() {
      @Override public void process(BufferedWriter writer) throws Exception {
        for (String name : classes) {
          writer.write(name);
          writer.newLine();
        }
      }
    });
    if (fqnOk) {
      doNotify("Inspection class names dumped to\n" + fqnListFile.getAbsolutePath());
    }

    final File groupsFile = new File(tempDirectory, "inspection_groups.txt");
    final boolean groupsOk = doDump(groupsFile, new Processor() {
      @Override public void process(BufferedWriter writer) throws Exception {
        for (Map.Entry<String, Collection<String>> entry : groups.entrySet()) {
          writer.write(entry.getKey());
          writer.write(':');
          writer.newLine();
          for (String name : entry.getValue()) {
            writer.write("  ");
            writer.write(name);
            writer.newLine();
          }
        }
      }
    });
    if (groupsOk) {
      doNotify("Inspection groups dumped to\n" + fqnListFile.getAbsolutePath());
    }
  }

  private static Class getInspectionClass(final InspectionToolWrapper toolWrapper) {
    return toolWrapper instanceof LocalInspectionToolWrapper ? ((LocalInspectionToolWrapper)toolWrapper).getTool().getClass() : toolWrapper.getClass();
  }

  private static String getGroupName(final InspectionToolWrapper toolWrapper) {
    final String name = toolWrapper.getGroupDisplayName();
    return StringUtil.isEmptyOrSpaces(name) ? "General" : name;
  }

  private static InputStream getDescriptionStream(final InspectionToolWrapper toolWrapper) {
    final Class aClass = getInspectionClass(toolWrapper);
    return ResourceUtil.getResourceAsStream(aClass, "/inspectionDescriptions", toolWrapper.getShortName() + ".html");
  }

  private interface Processor {
    void process(BufferedWriter writer) throws Exception;
  }

  private static boolean doDump(final File file, final Processor processor) {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
      processor.process(writer);
      return true;
    }
    catch (Exception e) {
      LOG.error(e);
      return false;
    }
  }

  private static void doNotify(final String message) {
    Notifications.Bus.notify(new Notification("Actions", "Inspection descriptions dumped", message, NotificationType.INFORMATION));
  }
}