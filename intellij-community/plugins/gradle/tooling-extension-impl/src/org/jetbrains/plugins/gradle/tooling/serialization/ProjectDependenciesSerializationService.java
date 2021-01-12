// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.serialization;

import com.amazon.ion.IonReader;
import com.amazon.ion.IonType;
import com.amazon.ion.IonWriter;
import com.amazon.ion.system.IonBinaryWriterBuilder;
import com.amazon.ion.system.IonReaderBuilder;
import com.intellij.openapi.externalSystem.model.project.dependencies.*;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.tooling.util.IntObjectMap;
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
public class ProjectDependenciesSerializationService implements SerializationService<ProjectDependencies> {
  private final WriteContext myWriteContext = new WriteContext();
  private final ReadContext myReadContext = new ReadContext();

  @Override
  public byte[] write(ProjectDependencies dependencies, Class<? extends ProjectDependencies> modelClazz) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    IonWriter writer = IonBinaryWriterBuilder.standard().build(out);
    try {
      write(writer, myWriteContext, dependencies);
    }
    finally {
      writer.close();
    }
    return out.toByteArray();
  }

  @Override
  public ProjectDependencies read(byte[] object, Class<? extends ProjectDependencies> modelClazz) throws IOException {
    IonReader reader = IonReaderBuilder.standard().build(object);
    try {
      return read(reader, myReadContext);
    }
    finally {
      reader.close();
    }
  }

  @Override
  public Class<? extends ProjectDependencies> getModelClass() {
    return ProjectDependencies.class;
  }

  private static void write(final IonWriter writer, final WriteContext context, final ProjectDependencies model) throws IOException {
    context.objectCollector.add(model, new ObjectCollector.Processor<IOException>() {
      @Override
      public void process(boolean isAdded, int objectId) throws IOException {
        writer.stepIn(IonType.STRUCT);
        writer.setFieldName(OBJECT_ID_FIELD);
        writer.writeInt(objectId);
        if (isAdded) {
          writeComponentsDependencies(writer, context, model.getComponentsDependencies());
        }
        writer.stepOut();
      }
    });
  }

  private static void writeComponentsDependencies(IonWriter writer,
                                                  WriteContext context,
                                                  Collection<ComponentDependencies> dependencies) throws IOException {
    writer.setFieldName("componentsDependencies");
    writer.stepIn(IonType.LIST);
    for (ComponentDependencies componentDependencies : dependencies) {
      writeComponentDependencies(writer, context, componentDependencies);
    }
    writer.stepOut();
  }

  private static void writeComponentDependencies(final IonWriter writer,
                                                 final WriteContext context,
                                                 final ComponentDependencies componentDependencies) throws IOException {
    context.componentDependenciesCollector.add(componentDependencies, new ObjectCollector.Processor<IOException>() {
      @Override
      public void process(boolean isAdded, int objectId) throws IOException {
        writer.stepIn(IonType.STRUCT);
        writer.setFieldName(OBJECT_ID_FIELD);
        writer.writeInt(objectId);
        if (isAdded) {
          writeString(writer, "name", componentDependencies.getComponentName());
          writer.setFieldName("compile");
          writeDependencyNode(writer, context, componentDependencies.getCompileDependenciesGraph());
          writer.setFieldName("runtime");
          writeDependencyNode(writer, context, componentDependencies.getRuntimeDependenciesGraph());
        }
        writer.stepOut();
      }
    });
  }

  private static void writeDependencyNode(final IonWriter writer,
                                          final WriteContext context,
                                          final DependencyNode node) throws IOException {
    if (node instanceof ProjectDependencyNode) {
      writeDependencyNode(writer, context, (ProjectDependencyNode)node);
    }
    else if (node instanceof ArtifactDependencyNode) {
      writeDependencyNode(writer, context, (ArtifactDependencyNode)node);
    }
    else if (node instanceof FileCollectionDependencyNode) {
      writeDependencyNode(writer, context, (FileCollectionDependencyNode)node);
    }
    else if (node instanceof ReferenceNode) {
      writeDependencyNode(writer, context, (ReferenceNode)node);
    }
    else if (node instanceof DependencyScopeNode) {
      writeDependencyNode(writer, context, (DependencyScopeNode)node);
    }
    else if (node instanceof UnknownDependencyNode) {
      writeDependencyNode(writer, context, (UnknownDependencyNode)node);
    }
  }

  private static void writeDependencyNode(final IonWriter writer,
                                          final WriteContext context,
                                          final ProjectDependencyNode node)
    throws IOException {
    context.dependencyNodeCollector.add(node, new ObjectCollector.Processor<IOException>() {
      @Override
      public void process(boolean isAdded, int objectId) throws IOException {
        writer.stepIn(IonType.STRUCT);
        writer.setFieldName(OBJECT_ID_FIELD);
        writer.writeInt(objectId);
        if (isAdded) {
          writer.setFieldName("_type");
          writer.writeString(ProjectDependencyNode.class.getSimpleName());
          writeDependencyCommonFields(writer, node);
          writeString(writer, "projectName", node.getProjectName());
          writeDependenciesField(writer, context, node);
        }
        writer.stepOut();
      }
    });
  }

  private static void writeDependencyNode(final IonWriter writer,
                                          final WriteContext context,
                                          final ArtifactDependencyNode node)
    throws IOException {
    context.dependencyNodeCollector.add(node, new ObjectCollector.Processor<IOException>() {
      @Override
      public void process(boolean isAdded, int objectId) throws IOException {
        writer.stepIn(IonType.STRUCT);
        writer.setFieldName(OBJECT_ID_FIELD);
        writer.writeInt(objectId);
        if (isAdded) {
          writer.setFieldName("_type");
          writer.writeString(ArtifactDependencyNode.class.getSimpleName());
          writeDependencyCommonFields(writer, node);
          writeString(writer, "group", node.getGroup());
          writeString(writer, "module", node.getModule());
          writeString(writer, "version", node.getVersion());
          writeDependenciesField(writer, context, node);
        }
        writer.stepOut();
      }
    });
  }

  private static void writeDependencyNode(final IonWriter writer,
                                          final WriteContext context,
                                          final FileCollectionDependencyNode node)
    throws IOException {
    context.dependencyNodeCollector.add(node, new ObjectCollector.Processor<IOException>() {
      @Override
      public void process(boolean isAdded, int objectId) throws IOException {
        writer.stepIn(IonType.STRUCT);
        writer.setFieldName(OBJECT_ID_FIELD);
        writer.writeInt(objectId);
        if (isAdded) {
          writer.setFieldName("_type");
          writer.writeString(FileCollectionDependencyNode.class.getSimpleName());
          writeDependencyCommonFields(writer, node);
          writeString(writer, "displayName", node.getDisplayName());
          writeString(writer, "path", node.getPath());
          writeDependenciesField(writer, context, node);
        }
        writer.stepOut();
      }
    });
  }

  private static void writeDependencyNode(final IonWriter writer,
                                          final WriteContext context,
                                          final ReferenceNode node)
    throws IOException {
    context.dependencyNodeCollector.add(node, new ObjectCollector.Processor<IOException>() {
      @Override
      public void process(boolean isAdded, int objectId) throws IOException {
        writer.stepIn(IonType.STRUCT);
        writer.setFieldName(OBJECT_ID_FIELD);
        writer.writeInt(objectId);
        if (isAdded) {
          writer.setFieldName("_type");
          writer.writeString(ReferenceNode.class.getSimpleName());
          writeDependencyCommonFields(writer, node);
        }
        writer.stepOut();
      }
    });
  }

  private static void writeDependencyNode(final IonWriter writer,
                                          final WriteContext context,
                                          final DependencyScopeNode node)
    throws IOException {
    context.dependencyNodeCollector.add(node, new ObjectCollector.Processor<IOException>() {
      @Override
      public void process(boolean isAdded, int objectId) throws IOException {
        writer.stepIn(IonType.STRUCT);
        writer.setFieldName(OBJECT_ID_FIELD);
        writer.writeInt(objectId);
        if (isAdded) {
          writer.setFieldName("_type");
          writer.writeString(DependencyScopeNode.class.getSimpleName());
          writeDependencyCommonFields(writer, node);
          writeString(writer, "scope", node.getScope());
          writeString(writer, "displayName", node.getDisplayName());
          writeString(writer, "description", node.getDescription());
          writeDependenciesField(writer, context, node);
        }
        writer.stepOut();
      }
    });
  }

  private static void writeDependencyNode(final IonWriter writer,
                                          final WriteContext context,
                                          final UnknownDependencyNode node)
    throws IOException {
    context.dependencyNodeCollector.add(node, new ObjectCollector.Processor<IOException>() {
      @Override
      public void process(boolean isAdded, int objectId) throws IOException {
        writer.stepIn(IonType.STRUCT);
        writer.setFieldName(OBJECT_ID_FIELD);
        writer.writeInt(objectId);
        if (isAdded) {
          writer.setFieldName("_type");
          writer.writeString(UnknownDependencyNode.class.getSimpleName());
          writeDependencyCommonFields(writer, node);
          writeString(writer, "name", node.getDisplayName());
          writeDependenciesField(writer, context, node);
        }
        writer.stepOut();
      }
    });
  }

  private static void writeDependencyCommonFields(IonWriter writer, DependencyNode node)
    throws IOException {
    writeLong(writer, "id", node.getId());
    writeString(writer, "resolutionState", node.getResolutionState());
  }

  private static void writeDependenciesField(IonWriter writer,
                                             WriteContext context,
                                             DependencyNode dependencyNode) throws IOException {
    writer.setFieldName("dependencies");
    writer.stepIn(IonType.LIST);
    for (DependencyNode componentDependencies : dependencyNode.getDependencies()) {
      writeDependencyNode(writer, context, componentDependencies);
    }
    writer.stepOut();
  }

  @Nullable
  private static ProjectDependencies read(final IonReader reader, final ReadContext context) {
    if (reader.next() == null) return null;
    reader.stepIn();

    ProjectDependencies model =
      context.objectMap.computeIfAbsent(readInt(reader, OBJECT_ID_FIELD), new IntObjectMap.SimpleObjectFactory<ProjectDependenciesImpl>() {

        @Override
        public ProjectDependenciesImpl create() {
          ProjectDependenciesImpl dependencies = new ProjectDependenciesImpl();
          List<ComponentDependencies> componentsDependencies = readComponentsDependencies(reader, context);
          for (ComponentDependencies componentDependencies : componentsDependencies) {
            dependencies.add(componentDependencies);
          }
          return dependencies;
        }
      });
    reader.stepOut();
    return model;
  }

  private static List<ComponentDependencies> readComponentsDependencies(IonReader reader, ReadContext context) {
    List<ComponentDependencies> list = new ArrayList<ComponentDependencies>();
    reader.next();
    reader.stepIn();
    ComponentDependencies componentDependencies;
    while ((componentDependencies = readComponentDependencies(reader, context)) != null) {
      list.add(componentDependencies);
    }
    reader.stepOut();
    return list;
  }

  @Nullable
  private static ComponentDependencies readComponentDependencies(final IonReader reader, final ReadContext context) {
    if (reader.next() == null) return null;
    reader.stepIn();
    ComponentDependencies dependency =
      context.componentDependenciesMap
        .computeIfAbsent(readInt(reader, OBJECT_ID_FIELD), new IntObjectMap.SimpleObjectFactory<ComponentDependencies>() {
          @Override
          public ComponentDependencies create() {
            String componentName = assertNotNull(readString(reader, "name"));
            DependencyScopeNode compileDependencies = (DependencyScopeNode)assertNotNull(readDependencyNode(reader, context, "compile"));
            DependencyScopeNode runtimeDependencies = (DependencyScopeNode)assertNotNull(readDependencyNode(reader, context, "runtime"));
            return new ComponentDependenciesImpl(componentName, compileDependencies, runtimeDependencies);
          }
        });
    reader.stepOut();
    return dependency;
  }

  private static DependencyNode readDependencyNode(final IonReader reader, final ReadContext context, @Nullable String fieldName) {
    if (reader.next() == null) return null;
    assertFieldName(reader, fieldName);
    reader.stepIn();

    DependencyNode dependency =
      context.nodesMap
        .computeIfAbsent(readInt(reader, OBJECT_ID_FIELD), new IntObjectMap.ObjectFactory<DependencyNode>() {

          @Override
          public DependencyNode newInstance() {
            String type = readString(reader, "_type");
            long id = readLong(reader, "id");
            String resolutionState = readString(reader, "resolutionState");
            DependencyNode node;
            if (ProjectDependencyNode.class.getSimpleName().equals(type)) {
              String projectName = assertNotNull(readString(reader, "projectName"));
              node = new ProjectDependencyNodeImpl(id, projectName);
            }
            else if (ArtifactDependencyNode.class.getSimpleName().equals(type)) {
              String group = assertNotNull(readString(reader, "group"));
              String module = assertNotNull(readString(reader, "module"));
              String version = assertNotNull(readString(reader, "version"));
              node = new ArtifactDependencyNodeImpl(id, group, module, version);
            }
            else if (FileCollectionDependencyNode.class.getSimpleName().equals(type)) {
              String displayName = assertNotNull(readString(reader, "displayName"));
              String path = assertNotNull(readString(reader, "path"));
              node = new FileCollectionDependencyNodeImpl(id, displayName, path);
            }
            else if (DependencyScopeNode.class.getSimpleName().equals(type)) {
              String scope = assertNotNull(readString(reader, "scope"));
              String displayName = assertNotNull(readString(reader, "displayName"));
              String description = assertNotNull(readString(reader, "description"));
              node = new DependencyScopeNode(id, scope, displayName, description);
            }
            else if (ReferenceNode.class.getSimpleName().equals(type)) {
              node = new ReferenceNode(id);
            }
            else if (UnknownDependencyNode.class.getSimpleName().equals(type)) {
              String name = assertNotNull(readString(reader, "name"));
              node = new UnknownDependencyNode(id, name);
            }
            else {
              throw new RuntimeException("Unsupported dependency node");
            }
            if (node instanceof AbstractDependencyNode) {
              ((AbstractDependencyNode)node).setResolutionState(resolutionState);
            }
            return node;
          }

          @Override
          public void fill(DependencyNode node) {
            if (node instanceof AbstractDependencyNode) {
              node.getDependencies().addAll(readDependencyNodes(reader, context));
            }
          }
        });
    reader.stepOut();
    return dependency;
  }

  private static Collection<? extends DependencyNode> readDependencyNodes(IonReader reader, ReadContext context) {
    List<DependencyNode> list = new ArrayList<DependencyNode>();
    reader.next();
    reader.stepIn();
    DependencyNode node;
    while ((node = readDependencyNode(reader, context, null)) != null) {
      list.add(node);
    }
    reader.stepOut();
    return list;
  }

  private static class ReadContext {
    private final IntObjectMap<ProjectDependenciesImpl> objectMap = new IntObjectMap<ProjectDependenciesImpl>();
    private final IntObjectMap<ComponentDependencies> componentDependenciesMap = new IntObjectMap<ComponentDependencies>();
    private final IntObjectMap<DependencyNode> nodesMap = new IntObjectMap<DependencyNode>();
  }

  private static class WriteContext {
    private final ObjectCollector<ProjectDependencies, IOException> objectCollector =
      new ObjectCollector<ProjectDependencies, IOException>();
    private final ObjectCollector<ComponentDependencies, IOException> componentDependenciesCollector =
      new ObjectCollector<ComponentDependencies, IOException>();
    private final ObjectCollector<DependencyNode, IOException> dependencyNodeCollector =
      new ObjectCollector<DependencyNode, IOException>();
  }
}

