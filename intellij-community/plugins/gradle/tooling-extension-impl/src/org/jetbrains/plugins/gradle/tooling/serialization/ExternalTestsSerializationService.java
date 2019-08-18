// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.serialization;

import com.amazon.ion.IonReader;
import com.amazon.ion.IonType;
import com.amazon.ion.IonWriter;
import com.amazon.ion.system.IonBinaryWriterBuilder;
import com.amazon.ion.system.IonReaderBuilder;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.tests.DefaultExternalTestSourceMapping;
import org.jetbrains.plugins.gradle.model.tests.DefaultExternalTestsModel;
import org.jetbrains.plugins.gradle.model.tests.ExternalTestSourceMapping;
import org.jetbrains.plugins.gradle.model.tests.ExternalTestsModel;
import org.jetbrains.plugins.gradle.tooling.util.IntObjectMap;
import org.jetbrains.plugins.gradle.tooling.util.IntObjectMap.SimpleObjectFactory;
import org.jetbrains.plugins.gradle.tooling.util.ObjectCollector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.jetbrains.plugins.gradle.tooling.serialization.ToolingStreamApiUtils.*;

/**
 * @author Vladislav.Soroka
 */
public class ExternalTestsSerializationService implements SerializationService<ExternalTestsModel> {
  private final WriteContext myWriteContext = new WriteContext();
  private final ReadContext myReadContext = new ReadContext();

  @Override
  public byte[] write(ExternalTestsModel testsModel, Class<? extends ExternalTestsModel> modelClazz) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    IonWriter writer = IonBinaryWriterBuilder.standard().build(out);
    try {
      write(writer, myWriteContext, testsModel);
    }
    finally {
      writer.close();
    }
    return out.toByteArray();
  }

  @Override
  public ExternalTestsModel read(byte[] object, Class<? extends ExternalTestsModel> modelClazz) throws IOException {
    IonReader reader = IonReaderBuilder.standard().build(object);
    try {
      return read(reader, myReadContext);
    }
    finally {
      reader.close();
    }
  }

  @Override
  public Class<? extends ExternalTestsModel> getModelClass() {
    return ExternalTestsModel.class;
  }


  private static void write(final IonWriter writer, final WriteContext context, final ExternalTestsModel model) throws IOException {
    context.objectCollector.add(model, new ObjectCollector.Processor<IOException>() {
      @Override
      public void process(boolean isAdded, int objectId) throws IOException {
        writer.stepIn(IonType.STRUCT);
        writer.setFieldName(OBJECT_ID_FIELD);
        writer.writeInt(objectId);
        if (isAdded) {
          writeTestSourceMappings(writer, context, model.getTestSourceMappings());
        }
        writer.stepOut();
      }
    });
  }

  private static void writeTestSourceMappings(IonWriter writer,
                                              WriteContext context,
                                              Collection<ExternalTestSourceMapping> sourceMappings) throws IOException {
    writer.setFieldName("sourceTestMappings");
    writer.stepIn(IonType.LIST);
    for (ExternalTestSourceMapping mapping : sourceMappings) {
      writeTestSourceMapping(writer, context, mapping);
    }
    writer.stepOut();
  }

  private static void writeTestSourceMapping(final IonWriter writer,
                                             final WriteContext context,
                                             final ExternalTestSourceMapping testSourceMapping) throws IOException {
    context.mappingCollector.add(testSourceMapping, new ObjectCollector.Processor<IOException>() {
      @Override
      public void process(boolean isAdded, int objectId) throws IOException {
        writer.stepIn(IonType.STRUCT);
        writer.setFieldName(OBJECT_ID_FIELD);
        writer.writeInt(objectId);
        if (isAdded) {
          writeString(writer, "testName", testSourceMapping.getTestName());
          writeString(writer, "testTaskPath", testSourceMapping.getTestTaskPath());
          writeString(writer, "cleanTestTaskPath", testSourceMapping.getCleanTestTaskPath());
          writeStrings(writer, "sourceFolders", testSourceMapping.getSourceFolders());
        }
        writer.stepOut();
      }
    });
  }

  @Nullable
  private static ExternalTestsModel read(final IonReader reader, final ReadContext context) {
    if (reader.next() == null) return null;
    reader.stepIn();
    ExternalTestsModel model =
      context.objectMap.computeIfAbsent(readInt(reader, OBJECT_ID_FIELD), new SimpleObjectFactory<ExternalTestsModel>() {
      @Override
      public ExternalTestsModel create() {
        DefaultExternalTestsModel testsModel = new DefaultExternalTestsModel();
        testsModel.setSourceTestMappings(readTestSourceMappings(reader, context));
        return testsModel;
      }
    });
    reader.stepOut();
    return model;
  }

  private static List<ExternalTestSourceMapping> readTestSourceMappings(IonReader reader, ReadContext context) {
    List<ExternalTestSourceMapping> list = new ArrayList<ExternalTestSourceMapping>();
    reader.next();
    reader.stepIn();
    ExternalTestSourceMapping testSourceMapping;
    while ((testSourceMapping = readTestSourceMapping(reader, context)) != null) {
      list.add(testSourceMapping);
    }
    reader.stepOut();
    return list;
  }

  @Nullable
  private static ExternalTestSourceMapping readTestSourceMapping(final IonReader reader, ReadContext context) {
    if (reader.next() == null) return null;
    reader.stepIn();
    ExternalTestSourceMapping dependency =
      context.testSourceMapping.computeIfAbsent(readInt(reader, OBJECT_ID_FIELD), new SimpleObjectFactory<ExternalTestSourceMapping>() {
        @Override
        public ExternalTestSourceMapping create() {
          DefaultExternalTestSourceMapping mapping = new DefaultExternalTestSourceMapping();
          mapping.setTestName(readString(reader, "testName"));
          mapping.setTestTaskPath(assertNotNull(readString(reader, "testTaskPath")));
          mapping.setCleanTestTaskPath(assertNotNull(readString(reader, "cleanTestTaskPath")));
          mapping.setSourceFolders(readStringSet(reader));
          return mapping;
        }
      });
    reader.stepOut();
    return dependency;
  }

  private static class ReadContext {
    private final IntObjectMap<ExternalTestsModel> objectMap = new IntObjectMap<ExternalTestsModel>();
    private final IntObjectMap<ExternalTestSourceMapping> testSourceMapping = new IntObjectMap<ExternalTestSourceMapping>();
  }

  private static class WriteContext {
    private final ObjectCollector<ExternalTestsModel, IOException> objectCollector = new ObjectCollector<ExternalTestsModel, IOException>();
    private final ObjectCollector<ExternalTestSourceMapping, IOException> mappingCollector =
      new ObjectCollector<ExternalTestSourceMapping, IOException>();
  }
}

