// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.ui.actions;

import com.intellij.codeInspection.InspectionsResultUtil;
import com.intellij.configurationStore.JbXmlOutputter;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;

final class XmlWriterWrapper implements Closeable {
  private final Project myProject;
  private final Path myOutputDirectory;
  private final String myName;
  private final String myRootTagName;

  private Writer myFileWriter;
  private JbXmlOutputter myOutputter;

  XmlWriterWrapper(@NotNull Project project,
                   @NotNull Path outputDirectory,
                   @NotNull String name,
                   @NotNull String rootTagName) {
    myProject = project;
    myOutputDirectory = outputDirectory;
    myName = name;
    myRootTagName = rootTagName;
  }

  void writeElement(@NotNull Element element) {
    try {
      checkOpen();
      myFileWriter.write('\n');
      myOutputter.output(element, myFileWriter);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  void checkOpen() throws IOException {
    if (myFileWriter == null) {
      myFileWriter = openFile(myOutputDirectory, myName);
      myOutputter = JbXmlOutputter.createOutputter(myProject);
      startWritingXml();
    }
  }

  @Override
  public void close() throws IOException {
    if (myFileWriter == null) {
      return;
    }

    try {
      endWritingXml();
    }
    finally {
      Writer fileWriter = myFileWriter;
      myFileWriter = null;
      fileWriter.close();
    }
  }

  @NotNull
  private static Writer openFile(@NotNull Path outputDirectory, @NotNull String name) throws IOException {
    return InspectionsResultUtil.getWriter(outputDirectory, name);
  }

  private void startWritingXml() throws IOException {
    myFileWriter.write('<');
    myFileWriter.write(myRootTagName);
    myFileWriter.write('>');
  }

  private void endWritingXml() throws IOException {
    try {
      myFileWriter.write("\n");
      myFileWriter.write('<');
      myFileWriter.write('/');
      myFileWriter.write(myRootTagName);
      myFileWriter.write('>');
    }
    finally {
      myFileWriter.close();
    }
  }
}
