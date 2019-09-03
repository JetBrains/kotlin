// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.serialization;

import com.amazon.ion.IonReader;
import com.amazon.ion.IonType;
import com.amazon.ion.IonWriter;
import com.amazon.ion.system.IonBinaryWriterBuilder;
import com.amazon.ion.system.IonReaderBuilder;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.BuildScriptClasspathModel;
import org.jetbrains.plugins.gradle.model.ClasspathEntryModel;
import org.jetbrains.plugins.gradle.tooling.internal.BuildScriptClasspathModelImpl;
import org.jetbrains.plugins.gradle.tooling.internal.ClasspathEntryModelImpl;
import org.jetbrains.plugins.gradle.tooling.util.IntObjectMap;
import org.jetbrains.plugins.gradle.tooling.util.IntObjectMap.SimpleObjectFactory;
import org.jetbrains.plugins.gradle.tooling.util.ObjectCollector;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.jetbrains.plugins.gradle.tooling.serialization.ToolingStreamApiUtils.*;

/**
 * @author Vladislav.Soroka
 */
public class BuildScriptClasspathModelSerializationService implements SerializationService<BuildScriptClasspathModel> {
  private final WriteContext myWriteContext = new WriteContext();
  private final ReadContext myReadContext = new ReadContext();

  @Override
  public byte[] write(BuildScriptClasspathModel classpathModel, Class<? extends BuildScriptClasspathModel> modelClazz) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    IonWriter writer = IonBinaryWriterBuilder.standard().build(out);
    try {
      write(writer, myWriteContext, classpathModel);
    }
    finally {
      writer.close();
    }
    return out.toByteArray();
  }

  @Override
  public BuildScriptClasspathModel read(byte[] object, Class<? extends BuildScriptClasspathModel> modelClazz) throws IOException {
    IonReader reader = IonReaderBuilder.standard().build(object);
    try {
      return read(reader, myReadContext);
    }
    finally {
      reader.close();
    }
  }

  @Override
  public Class<? extends BuildScriptClasspathModel> getModelClass() {
    return BuildScriptClasspathModel.class;
  }


  private static void write(final IonWriter writer, final WriteContext context, final BuildScriptClasspathModel model) throws IOException {
    context.objectCollector.add(model, new ObjectCollector.Processor<IOException>() {
      @Override
      public void process(boolean isAdded, int objectId) throws IOException {
        writer.stepIn(IonType.STRUCT);
        writer.setFieldName(OBJECT_ID_FIELD);
        writer.writeInt(objectId);
        if (isAdded) {
          if (!context.isFirstModelWritten) {
            context.isFirstModelWritten = true;
            writeString(writer, "gradleVersion", model.getGradleVersion());
            writeFile(writer, "gradleHomeDir", model.getGradleHomeDir());
          }
          writeClasspath(writer, model.getClasspath());
        }
        writer.stepOut();
      }
    });
  }

  private static void writeClasspath(IonWriter writer, Set<? extends ClasspathEntryModel> classpath) throws IOException {
    writer.setFieldName("classpath");
    writer.stepIn(IonType.LIST);
    for (ClasspathEntryModel entry : classpath) {
      writeClasspathEntry(writer, entry);
    }
    writer.stepOut();
  }

  private static void writeClasspathEntry(IonWriter writer, ClasspathEntryModel entry) throws IOException {
    writer.stepIn(IonType.STRUCT);
    writeStrings(writer, "classes", entry.getClasses());
    writeStrings(writer, "sources", entry.getSources());
    writeStrings(writer, "javadoc", entry.getJavadoc());
    writer.stepOut();
  }

  @Nullable
  private static BuildScriptClasspathModel read(final IonReader reader, final ReadContext context) {
    if (reader.next() == null) return null;
    reader.stepIn();

    BuildScriptClasspathModelImpl model =
      context.objectMap.computeIfAbsent(readInt(reader, OBJECT_ID_FIELD), new SimpleObjectFactory<BuildScriptClasspathModelImpl>() {
        @Override
        public BuildScriptClasspathModelImpl create() {
          BuildScriptClasspathModelImpl classpathModel = new BuildScriptClasspathModelImpl();
          if (!context.isFirstModelRead) {
            context.isFirstModelRead = true;
            context.gradleVersion = assertNotNull(readString(reader, "gradleVersion"));
            context.gradleHomeDir = readFile(reader, "gradleHomeDir");
          }
          classpathModel.setGradleVersion(context.gradleVersion);
          classpathModel.setGradleHomeDir(context.gradleHomeDir);
          List<ClasspathEntryModel> classpathEntries = readClasspath(reader);
          for (ClasspathEntryModel entry : classpathEntries) {
            classpathModel.add(entry);
          }
          return classpathModel;
        }
      });
    reader.stepOut();
    return model;
  }

  private static List<ClasspathEntryModel> readClasspath(IonReader reader) {
    List<ClasspathEntryModel> list = new ArrayList<ClasspathEntryModel>();
    reader.next();
    reader.stepIn();
    ClasspathEntryModel entry;
    while ((entry = readClasspathEntry(reader)) != null) {
      list.add(entry);
    }
    reader.stepOut();
    return list;
  }

  private static ClasspathEntryModel readClasspathEntry(IonReader reader) {
    if (reader.next() == null) return null;
    reader.stepIn();
    ClasspathEntryModel entryModel = new ClasspathEntryModelImpl(
      readStringSet(reader),
      readStringSet(reader),
      readStringSet(reader)
    );
    reader.stepOut();
    return entryModel;
  }

  private static class ReadContext {
    private boolean isFirstModelRead;
    private File gradleHomeDir;
    private String gradleVersion;
    private final IntObjectMap<BuildScriptClasspathModelImpl> objectMap = new IntObjectMap<BuildScriptClasspathModelImpl>();
  }

  private static class WriteContext {
    private boolean isFirstModelWritten;
    private final ObjectCollector<BuildScriptClasspathModel, IOException> objectCollector =
      new ObjectCollector<BuildScriptClasspathModel, IOException>();
  }
}

