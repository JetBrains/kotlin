// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.serialization;

import com.amazon.ion.IonReader;
import com.amazon.ion.IonType;
import com.amazon.ion.IonWriter;
import com.amazon.ion.system.IonBinaryWriterBuilder;
import com.amazon.ion.system.IonReaderBuilder;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.*;
import org.jetbrains.plugins.gradle.tooling.util.IntObjectMap;
import org.jetbrains.plugins.gradle.tooling.util.IntObjectMap.SimpleObjectFactory;
import org.jetbrains.plugins.gradle.tooling.util.ObjectCollector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.plugins.gradle.tooling.serialization.ToolingStreamApiUtils.*;

/**
 * @author Vladislav.Soroka
 */
public class GradleExtensionsSerializationService implements SerializationService<GradleExtensions> {
  private final WriteContext myWriteContext = new WriteContext();
  private final ReadContext myReadContext = new ReadContext();

  @Override
  public byte[] write(GradleExtensions gradleExtensions, Class<? extends GradleExtensions> modelClazz) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    IonWriter writer = IonBinaryWriterBuilder.standard().build(out);
    try {
      write(writer, myWriteContext, gradleExtensions);
    }
    finally {
      writer.close();
    }
    return out.toByteArray();
  }

  @Override
  public GradleExtensions read(byte[] object, Class<? extends GradleExtensions> modelClazz) throws IOException {
    IonReader reader = IonReaderBuilder.standard().build(object);
    try {
      return read(reader, myReadContext);
    }
    finally {
      reader.close();
    }
  }

  @Override
  public Class<? extends GradleExtensions> getModelClass() {
    return GradleExtensions.class;
  }

  private static void write(final IonWriter writer, final WriteContext context, final GradleExtensions model) throws IOException {
    context.objectCollector.add(model, new ObjectCollector.Processor<IOException>() {
      @Override
      public void process(boolean isAdded, int objectId) throws IOException {
        writer.stepIn(IonType.STRUCT);
        writer.setFieldName(OBJECT_ID_FIELD);
        writer.writeInt(objectId);
        if (isAdded) {
          writeString(writer, "parentProjectPath", model.getParentProjectPath());
          writeConfigurations(writer, context, model.getConfigurations());
          writeConventions(writer, context, model.getConventions());
          writeExtensions(writer, context, model.getExtensions());
          writeGradleProperties(writer, context, model.getGradleProperties());
          // do not write tasks, as it will be added from ExternalProject model at BaseGradleProjectResolverExtension.populateModuleExtraModels
        }
        writer.stepOut();
      }
    });
  }

  private static void writeConfigurations(IonWriter writer,
                                          WriteContext context,
                                          List<? extends GradleConfiguration> configurations) throws IOException {
    writer.setFieldName("configurations");
    writer.stepIn(IonType.LIST);
    for (GradleConfiguration configuration : configurations) {
      writeConfiguration(writer, context, configuration);
    }
    writer.stepOut();
  }


  static void writeConfiguration(final IonWriter writer,
                                 final WriteContext context,
                                 final GradleConfiguration configuration) throws IOException {
    context.configurationsCollector.add(configuration, new ObjectCollector.Processor<IOException>() {
      @Override
      public void process(boolean isAdded, int objectId) throws IOException {
        writer.stepIn(IonType.STRUCT);
        writer.setFieldName(OBJECT_ID_FIELD);
        writer.writeInt(objectId);
        if (isAdded) {
          writeString(writer, "name", configuration.getName());
          writeString(writer, "description", configuration.getDescription());
          writeBoolean(writer, "visible", configuration.isVisible());
          writeBoolean(writer, "scriptClasspathConfiguration", configuration.isScriptClasspathConfiguration());
        }
        writer.stepOut();
      }
    });
  }

  private static void writeConventions(IonWriter writer,
                                       WriteContext context,
                                       List<? extends GradleConvention> conventions) throws IOException {
    writer.setFieldName("conventions");
    writer.stepIn(IonType.LIST);
    for (GradleConvention convention : conventions) {
      write(writer, context, convention);
    }
    writer.stepOut();
  }

  private static void writeExtensions(IonWriter writer,
                                      WriteContext context,
                                      List<? extends GradleExtension> extensions) throws IOException {
    writer.setFieldName("extensions");
    writer.stepIn(IonType.LIST);
    for (GradleExtension extension : extensions) {
      write(writer, context, extension);
    }
    writer.stepOut();
  }

  private static void writeGradleProperties(IonWriter writer,
                                            WriteContext context,
                                            List<? extends GradleProperty> properties) throws IOException {
    writer.setFieldName("properties");
    writer.stepIn(IonType.LIST);
    for (GradleProperty property : properties) {
      write(writer, context, property);
    }
    writer.stepOut();
  }

  static void write(final IonWriter writer,
                    final WriteContext context,
                    final GradleProperty property) throws IOException {
    context.propertiesCollector.add(property, new ObjectCollector.Processor<IOException>() {
      @Override
      public void process(boolean isAdded, int objectId) throws IOException {
        writer.stepIn(IonType.STRUCT);
        writer.setFieldName(OBJECT_ID_FIELD);
        writer.writeInt(objectId);
        if (isAdded) {
          writeString(writer, "name", property.getName());
          writeString(writer, "typeFqn", property.getTypeFqn());
        }
        writer.stepOut();
      }
    });
  }

  @Nullable
  private static GradleExtensions read(final IonReader reader, final ReadContext context) {
    if (reader.next() == null) return null;
    reader.stepIn();

    DefaultGradleExtensions extensions =
      context.objectMap.computeIfAbsent(readInt(reader, OBJECT_ID_FIELD), new SimpleObjectFactory<DefaultGradleExtensions>() {
        @Override
        public DefaultGradleExtensions create() {
          DefaultGradleExtensions gradleExtensions = new DefaultGradleExtensions();
          gradleExtensions.setParentProjectPath(readString(reader, "parentProjectPath"));
          gradleExtensions.getConfigurations().addAll(readConfigurations(reader, context));
          gradleExtensions.getConventions().addAll(readConventions(reader, context));
          gradleExtensions.getExtensions().addAll(readExtensions(reader, context));
          gradleExtensions.getGradleProperties().addAll(readGradleProperties(reader, context));
          // do not read tasks, as it will be added from ExternalProject model at BaseGradleProjectResolverExtension.populateModuleExtraModels
          return gradleExtensions;
        }
      });
    reader.stepOut();
    return extensions;
  }

  private static List<DefaultGradleConfiguration> readConfigurations(IonReader reader, ReadContext context) {
    List<DefaultGradleConfiguration> list = new ArrayList<DefaultGradleConfiguration>();
    reader.next();
    reader.stepIn();
    DefaultGradleConfiguration configuration;
    while ((configuration = readConfiguration(reader, context)) != null) {
      list.add(configuration);
    }
    reader.stepOut();
    return list;
  }

  @Nullable
  private static DefaultGradleConfiguration readConfiguration(final IonReader reader, ReadContext context) {
    if (reader.next() == null) return null;
    reader.stepIn();
    DefaultGradleConfiguration configuration =
      context.configurationsMap.computeIfAbsent(readInt(reader, OBJECT_ID_FIELD), new SimpleObjectFactory<DefaultGradleConfiguration>() {
        @Override
        public DefaultGradleConfiguration create() {
          return new DefaultGradleConfiguration(
            assertNotNull(readString(reader, "name")),
            readString(reader, "description"),
            readBoolean(reader, "visible"),
            readBoolean(reader, "scriptClasspathConfiguration"));
        }
      });
    reader.stepOut();
    return configuration;
  }

  private static List<DefaultGradleConvention> readConventions(IonReader reader, ReadContext context) {
    List<DefaultGradleConvention> list = new ArrayList<DefaultGradleConvention>();
    reader.next();
    reader.stepIn();
    DefaultGradleConvention entry;
    while ((entry = readConvention(reader, context)) != null) {
      list.add(entry);
    }
    reader.stepOut();
    return list;
  }

  @Nullable
  private static DefaultGradleConvention readConvention(final IonReader reader, ReadContext context) {
    if (reader.next() == null) return null;
    reader.stepIn();
    DefaultGradleConvention convention =
      context.conventionsMap.computeIfAbsent(readInt(reader, OBJECT_ID_FIELD), new SimpleObjectFactory<DefaultGradleConvention>() {
        @Override
        public DefaultGradleConvention create() {
          return new DefaultGradleConvention(assertNotNull(readString(reader, "name")), readString(reader, "typeFqn"));
        }
      });
    reader.stepOut();
    return convention;
  }

  private static List<DefaultGradleExtension> readExtensions(IonReader reader, ReadContext context) {
    List<DefaultGradleExtension> list = new ArrayList<DefaultGradleExtension>();
    reader.next();
    reader.stepIn();
    DefaultGradleExtension entry;
    while ((entry = readExtension(reader, context)) != null) {
      list.add(entry);
    }
    reader.stepOut();
    return list;
  }

  @Nullable
  private static DefaultGradleExtension readExtension(final IonReader reader, ReadContext context) {
    if (reader.next() == null) return null;
    reader.stepIn();
    DefaultGradleExtension convention =
      context.extensionsMap.computeIfAbsent(readInt(reader, OBJECT_ID_FIELD), new SimpleObjectFactory<DefaultGradleExtension>() {
        @Override
        public DefaultGradleExtension create() {
          return new DefaultGradleExtension(assertNotNull(readString(reader, "name")), readString(reader, "typeFqn"));
        }
      });
    reader.stepOut();
    return convention;
  }

  private static List<DefaultGradleProperty> readGradleProperties(IonReader reader, ReadContext context) {
    List<DefaultGradleProperty> list = new ArrayList<DefaultGradleProperty>();
    reader.next();
    reader.stepIn();
    DefaultGradleProperty entry;
    while ((entry = readGradleProperty(reader, context)) != null) {
      list.add(entry);
    }
    reader.stepOut();
    return list;
  }

  @Nullable
  private static DefaultGradleProperty readGradleProperty(final IonReader reader, ReadContext context) {
    if (reader.next() == null) return null;
    reader.stepIn();
    DefaultGradleProperty convention =
      context.propertiesMap.computeIfAbsent(readInt(reader, OBJECT_ID_FIELD), new SimpleObjectFactory<DefaultGradleProperty>() {
        @Override
        public DefaultGradleProperty create() {
          return new DefaultGradleProperty(assertNotNull(readString(reader, "name")), readString(reader, "typeFqn"));
        }
      });
    reader.stepOut();
    return convention;
  }

  private static class ReadContext {
    private final IntObjectMap<DefaultGradleExtensions> objectMap = new IntObjectMap<DefaultGradleExtensions>();
    private final IntObjectMap<DefaultGradleConfiguration> configurationsMap = new IntObjectMap<DefaultGradleConfiguration>();
    private final IntObjectMap<DefaultGradleConvention> conventionsMap = new IntObjectMap<DefaultGradleConvention>();
    private final IntObjectMap<DefaultGradleExtension> extensionsMap = new IntObjectMap<DefaultGradleExtension>();
    private final IntObjectMap<DefaultGradleProperty> propertiesMap = new IntObjectMap<DefaultGradleProperty>();
  }

  private static class WriteContext {
    private final ObjectCollector<GradleExtensions, IOException> objectCollector = new ObjectCollector<GradleExtensions, IOException>();
    private final ObjectCollector<GradleConfiguration, IOException> configurationsCollector =
      new ObjectCollector<GradleConfiguration, IOException>();
    private final ObjectCollector<GradleProperty, IOException> propertiesCollector =
      new ObjectCollector<GradleProperty, IOException>();
  }
}

