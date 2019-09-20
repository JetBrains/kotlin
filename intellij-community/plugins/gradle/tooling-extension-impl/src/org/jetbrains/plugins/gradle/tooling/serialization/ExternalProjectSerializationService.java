// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.serialization;

import com.amazon.ion.IonReader;
import com.amazon.ion.IonType;
import com.amazon.ion.IonWriter;
import com.amazon.ion.system.IonBinaryWriterBuilder;
import com.amazon.ion.system.IonReaderBuilder;
import com.amazon.ion.util.IonStreamUtils;
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType;
import com.intellij.openapi.externalSystem.model.project.IExternalSystemSourceType;
import com.intellij.util.ThrowableConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.DefaultExternalDependencyId;
import org.jetbrains.plugins.gradle.ExternalDependencyId;
import org.jetbrains.plugins.gradle.model.*;
import org.jetbrains.plugins.gradle.tooling.util.IntObjectMap;
import org.jetbrains.plugins.gradle.tooling.util.IntObjectMap.ObjectFactory;
import org.jetbrains.plugins.gradle.tooling.util.ObjectCollector;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.intellij.util.ArrayUtilRt.EMPTY_STRING_ARRAY;
import static org.jetbrains.plugins.gradle.tooling.serialization.ToolingStreamApiUtils.*;

/**
 * @author Vladislav.Soroka
 */
public class ExternalProjectSerializationService implements SerializationService<ExternalProject> {
  private final WriteContext myWriteContext = new WriteContext();
  private final ReadContext myReadContext = new ReadContext();

  @Override
  public byte[] write(ExternalProject project, Class<? extends ExternalProject> modelClazz) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    IonWriter writer = IonBinaryWriterBuilder.standard().build(out);
    try {
      writeProject(writer, myWriteContext, project);
    }
    finally {
      writer.close();
    }
    return out.toByteArray();
  }

  @Override
  public ExternalProject read(byte[] object, Class<? extends ExternalProject> modelClazz) throws IOException {
    IonReader reader = IonReaderBuilder.standard().build(object);
    try {
      return readProject(reader, myReadContext);
    }
    finally {
      reader.close();
    }
  }

  @Override
  public Class<? extends ExternalProject> getModelClass() {
    return ExternalProject.class;
  }

  private static void writeProject(final IonWriter writer,
                                   final WriteContext context,
                                   final ExternalProject project) throws IOException {
    context.getProjectsCollector().add(project, new ObjectCollector.Processor<IOException>() {
      @Override
      public void process(boolean isAdded, int objectId) throws IOException {
        writer.stepIn(IonType.STRUCT);
        writer.setFieldName(OBJECT_ID_FIELD);
        writer.writeInt(objectId);
        if (isAdded) {
          writeString(writer, "id", project.getId());
          writeString(writer, "name", project.getName());
          writeString(writer, "qName", project.getQName());
          writeString(writer, "description", project.getDescription());
          writeString(writer, "group", project.getGroup());
          writeString(writer, "version", project.getVersion());
          writeString(writer, "projectDir", project.getProjectDir().getPath());
          writeString(writer, "buildDir", project.getBuildDir().getPath());
          writeFile(writer, "buildFile", project.getBuildFile());
          writeTasks(writer, project.getTasks());
          writeSourceSets(writer, context, project.getSourceSets());
          writeFiles(writer, "artifacts", project.getArtifacts());
          writeArtifactsByConfiguration(writer, project.getArtifactsByConfiguration());
          writeChildProjects(writer, context, project.getChildProjects());
        }
        writer.stepOut();
      }
    });
  }

  private static void writeChildProjects(IonWriter writer,
                                         WriteContext context,
                                         Map<String, ? extends ExternalProject> projects) throws IOException {
    writer.setFieldName("childProjects");
    writer.stepIn(IonType.LIST);
    for (ExternalProject project : projects.values()) {
      writeProject(writer, context, project);
    }
    writer.stepOut();
  }

  private static void writeSourceSets(IonWriter writer,
                                      WriteContext context,
                                      Map<String, ? extends ExternalSourceSet> sets) throws IOException {
    writer.setFieldName("sourceSets");
    writer.stepIn(IonType.LIST);
    for (ExternalSourceSet sourceSet : sets.values()) {
      writeSourceSet(writer, context, sourceSet);
    }
    writer.stepOut();
  }

  private static void writeSourceSet(IonWriter writer,
                                     WriteContext context,
                                     ExternalSourceSet sourceSet) throws IOException {
    writer.stepIn(IonType.STRUCT);

    writeString(writer, "name", sourceSet.getName());
    writeString(writer, "sourceCompatibility", sourceSet.getSourceCompatibility());
    writeString(writer, "targetCompatibility", sourceSet.getTargetCompatibility());
    writeFiles(writer, "artifacts", sourceSet.getArtifacts());
    writeDependencies(writer, context, sourceSet.getDependencies());
    writeSourceDirectorySets(writer, sourceSet.getSources());

    writer.stepOut();
  }

  private static void writeSourceDirectorySets(IonWriter writer,
                                               Map<? extends IExternalSystemSourceType, ? extends ExternalSourceDirectorySet> sources)
    throws IOException {
    writer.setFieldName("sources");
    writer.stepIn(IonType.LIST);
    for (Map.Entry<? extends IExternalSystemSourceType, ? extends ExternalSourceDirectorySet> entry : sources.entrySet()) {
      writeSourceDirectorySet(writer, entry.getKey(), entry.getValue());
    }
    writer.stepOut();
  }

  private static void writeSourceDirectorySet(IonWriter writer,
                                              IExternalSystemSourceType sourceType,
                                              ExternalSourceDirectorySet directorySet)
    throws IOException {
    writer.stepIn(IonType.STRUCT);
    writeString(writer, "sourceType", ExternalSystemSourceType.from(sourceType).name());
    writeString(writer, "name", directorySet.getName());
    writeFiles(writer, "srcDirs", directorySet.getSrcDirs());
    writeFiles(writer, "gradleOutputDirs", directorySet.getGradleOutputDirs());
    writeFile(writer, "outputDir", directorySet.getOutputDir());
    writer.setFieldName("inheritedCompilerOutput");
    writer.writeBool(directorySet.isCompilerOutputPathInherited());
    writePatterns(writer, directorySet.getPatterns());
    writeFilters(writer, directorySet.getFilters());
    writer.stepOut();
  }

  private static void writeFilters(IonWriter writer, List<? extends ExternalFilter> filters) throws IOException {
    writer.setFieldName("filters");
    writer.stepIn(IonType.LIST);
    for (ExternalFilter filter : filters) {
      writeFilter(writer, filter);
    }
    writer.stepOut();
  }

  private static void writeFilter(IonWriter writer, ExternalFilter filter) throws IOException {
    writer.stepIn(IonType.STRUCT);
    writeString(writer, "filterType", filter.getFilterType());
    writeString(writer, "propertiesAsJsonMap", filter.getPropertiesAsJsonMap());
    writer.stepOut();
  }

  private static void writePatterns(IonWriter writer, FilePatternSet patterns) throws IOException {
    writer.setFieldName("patterns");
    writer.stepIn(IonType.STRUCT);
    writer.setFieldName("includes");
    IonStreamUtils.writeStringList(writer, patterns.getIncludes().toArray(EMPTY_STRING_ARRAY));
    writer.setFieldName("excludes");
    IonStreamUtils.writeStringList(writer, patterns.getExcludes().toArray(EMPTY_STRING_ARRAY));
    writer.stepOut();
  }

  private static void writeDependencies(IonWriter writer,
                                        WriteContext context,
                                        Collection<ExternalDependency> dependencies) throws IOException {
    writer.setFieldName("dependencies");
    writer.stepIn(IonType.LIST);
    for (ExternalDependency dependency : dependencies) {
      writeDependency(writer, context, dependency);
    }
    writer.stepOut();
  }

  static void writeDependency(IonWriter writer,
                              WriteContext context,
                              ExternalDependency dependency) throws IOException {
    if (dependency instanceof ExternalLibraryDependency) {
      writeDependency(writer, context, (ExternalLibraryDependency)dependency);
    }
    else if (dependency instanceof ExternalMultiLibraryDependency) {
      writeDependency(writer, context, (ExternalMultiLibraryDependency)dependency);
    }
    else if (dependency instanceof ExternalProjectDependency) {
      writeDependency(writer, context, (ExternalProjectDependency)dependency);
    }
    else if (dependency instanceof FileCollectionDependency) {
      writeDependency(writer, context, (FileCollectionDependency)dependency);
    }
    else if (dependency instanceof UnresolvedExternalDependency) {
      writeDependency(writer, context, (UnresolvedExternalDependency)dependency);
    }
  }

  private static void writeDependency(final IonWriter writer, final WriteContext context, final ExternalLibraryDependency dependency)
    throws IOException {
    context.getDependenciesCollector().add(dependency, new ObjectCollector.Processor<IOException>() {
      @Override
      public void process(boolean isAdded, int objectId) throws IOException {
        writer.stepIn(IonType.STRUCT);
        writer.setFieldName(OBJECT_ID_FIELD);
        writer.writeInt(objectId);
        if (isAdded) {
          writer.setFieldName("_type");
          writer.writeString(ExternalLibraryDependency.class.getSimpleName());
          writeDependencyCommonFields(writer, context, dependency);

          writeFile(writer, "file", dependency.getFile());
          writeFile(writer, "source", dependency.getSource());
          writeFile(writer, "javadoc", dependency.getJavadoc());
        }
        writer.stepOut();
      }
    });
  }

  private static void writeDependency(final IonWriter writer, final WriteContext context, final ExternalMultiLibraryDependency dependency)
    throws IOException {
    context.getDependenciesCollector().add(dependency, new ObjectCollector.Processor<IOException>() {
      @Override
      public void process(boolean isAdded, int objectId) throws IOException {
        writer.stepIn(IonType.STRUCT);
        writer.setFieldName(OBJECT_ID_FIELD);
        writer.writeInt(objectId);
        if (isAdded) {
          writer.setFieldName("_type");
          writer.writeString(ExternalMultiLibraryDependency.class.getSimpleName());
          writeDependencyCommonFields(writer, context, dependency);
          writeFiles(writer, "files", dependency.getFiles());
          writeFiles(writer, "sources", dependency.getSources());
          writeFiles(writer, "javadocs", dependency.getJavadoc());
        }
        writer.stepOut();
      }
    });
  }

  private static void writeDependency(final IonWriter writer, final WriteContext context, final ExternalProjectDependency dependency)
    throws IOException {
    context.getDependenciesCollector().add(dependency, new ObjectCollector.Processor<IOException>() {
      @Override
      public void process(boolean isAdded, int objectId) throws IOException {
        writer.stepIn(IonType.STRUCT);
        writer.setFieldName(OBJECT_ID_FIELD);
        writer.writeInt(objectId);
        if (isAdded) {
          writer.setFieldName("_type");
          writer.writeString(ExternalProjectDependency.class.getSimpleName());
          writeDependencyCommonFields(writer, context, dependency);

          writeString(writer, "projectPath", dependency.getProjectPath());
          writeString(writer, "configurationName", dependency.getConfigurationName());
          writeFiles(writer, "projectDependencyArtifacts", dependency.getProjectDependencyArtifacts());
          writeFiles(writer, "projectDependencyArtifactsSources", dependency.getProjectDependencyArtifactsSources());
        }
        writer.stepOut();
      }
    });
  }

  private static void writeDependency(final IonWriter writer, final WriteContext context, final FileCollectionDependency dependency)
    throws IOException {
    context.getDependenciesCollector().add(dependency, new ObjectCollector.Processor<IOException>() {
      @Override
      public void process(boolean isAdded, int objectId) throws IOException {
        writer.stepIn(IonType.STRUCT);
        writer.setFieldName(OBJECT_ID_FIELD);
        writer.writeInt(objectId);
        if (isAdded) {
          writer.setFieldName("_type");
          writer.writeString(FileCollectionDependency.class.getSimpleName());
          writeDependencyCommonFields(writer, context, dependency);
          writeFiles(writer, "files", dependency.getFiles());
        }
        writer.stepOut();
      }
    });
  }

  private static void writeDependency(final IonWriter writer, final WriteContext context, final UnresolvedExternalDependency dependency)
    throws IOException {
    context.getDependenciesCollector().add(dependency, new ObjectCollector.Processor<IOException>() {
      @Override
      public void process(boolean isAdded, int objectId) throws IOException {
        writer.stepIn(IonType.STRUCT);
        writer.setFieldName(OBJECT_ID_FIELD);
        writer.writeInt(objectId);
        if (isAdded) {
          writer.setFieldName("_type");
          writer.writeString(UnresolvedExternalDependency.class.getSimpleName());
          writeDependencyCommonFields(writer, context, dependency);
          writeString(writer, "failureMessage", dependency.getFailureMessage());
        }
        writer.stepOut();
      }
    });
  }

  private static void writeDependencyCommonFields(IonWriter writer, WriteContext context, ExternalDependency dependency)
    throws IOException {
    ExternalDependencyId id = dependency.getId();
    writer.setFieldName("id");
    writer.stepIn(IonType.STRUCT);
    writeString(writer, "group", id.getGroup());
    writeString(writer, "name", id.getName());
    writeString(writer, "version", id.getVersion());
    writeString(writer, "packaging", id.getPackaging());
    writeString(writer, "classifier", id.getClassifier());
    writer.stepOut();

    writeString(writer, "scope", dependency.getScope());
    writeString(writer, "selectionReason", dependency.getSelectionReason());
    writer.setFieldName("classpathOrder");
    writer.writeInt(dependency.getClasspathOrder());
    writer.setFieldName("exported");
    writer.writeBool(dependency.getExported());

    writeDependencies(writer, context, dependency.getDependencies());
  }

  private static void writeTasks(IonWriter writer, Map<String, ? extends ExternalTask> tasks) throws IOException {
    writer.setFieldName("tasks");
    writer.stepIn(IonType.LIST);
    for (ExternalTask task : tasks.values()) {
      writer.stepIn(IonType.STRUCT);
      writeString(writer, "name", task.getName());
      writeString(writer, "qName", task.getQName());
      writeString(writer, "description", task.getDescription());
      writeString(writer, "group", task.getGroup());
      writeString(writer, "type", task.getType());
      writer.setFieldName("isTest");
      writer.writeBool(task.isTest());
      writer.stepOut();
    }
    writer.stepOut();
  }

  private static void writeArtifactsByConfiguration(final IonWriter writer, Map<String, Set<File>> configuration) throws IOException {
    writeMap(writer, "artifactsByConfiguration", configuration, new ThrowableConsumer<String, IOException>() {
      @Override
      public void consume(String s) throws IOException {
        writer.writeString(s);
      }
    }, new ThrowableConsumer<Set<File>, IOException>() {
      @Override
      public void consume(Set<File> files) throws IOException {
        writeFiles(writer, "value", files);
      }
    });
  }

  @Nullable
  private static DefaultExternalProject readProject(@NotNull final IonReader reader,
                                                    @NotNull final ReadContext context) {
    if (reader.next() == null) return null;
    reader.stepIn();

    DefaultExternalProject project =
      context.getProjectsMap().computeIfAbsent(readInt(reader, OBJECT_ID_FIELD), new ObjectFactory<DefaultExternalProject>() {
        @Override
        public DefaultExternalProject newInstance() {
          return new DefaultExternalProject();
        }

        @Override
        public void fill(DefaultExternalProject externalProject) {
          externalProject.setExternalSystemId("GRADLE");
          externalProject.setId(assertNotNull(readString(reader, "id")));
          externalProject.setName(assertNotNull(readString(reader, "name")));
          externalProject.setQName(assertNotNull(readString(reader, "qName")));
          externalProject.setDescription(readString(reader, "description"));
          externalProject.setGroup(assertNotNull(readString(reader, "group")));
          externalProject.setVersion(assertNotNull(readString(reader, "version")));
          File projectDir = readFile(reader, "projectDir");
          if (projectDir != null) {
            externalProject.setProjectDir(projectDir);
          }
          File buildDir = readFile(reader, "buildDir");
          if (buildDir != null) {
            externalProject.setBuildDir(buildDir);
          }
          File buildFile = readFile(reader, "buildFile");
          if (buildFile != null) {
            externalProject.setBuildFile(buildFile);
          }
          readTasks(reader, externalProject);
          readSourceSets(reader, context, externalProject);
          externalProject.setArtifacts(readFiles(reader));
          externalProject.setArtifactsByConfiguration(readStringToFileSetMap(reader));
          externalProject.setChildProjects(readProjects(reader, context));
        }
      });
    reader.stepOut();
    return project;
  }

  private static Map<String, DefaultExternalProject> readProjects(@NotNull IonReader reader,
                                                                  @NotNull final ReadContext context) {
    Map<String, DefaultExternalProject> map = new TreeMap<String, DefaultExternalProject>();
    reader.next();
    reader.stepIn();
    DefaultExternalProject project;
    while ((project = readProject(reader, context)) != null) {
      map.put(project.getName(), project);
    }
    reader.stepOut();
    return map;
  }

  private static void readTasks(IonReader reader, DefaultExternalProject project) {
    reader.next();
    reader.stepIn();
    Map<String, DefaultExternalTask> tasks = new HashMap<String, DefaultExternalTask>();
    DefaultExternalTask task;
    while ((task = readTask(reader)) != null) {
      tasks.put(task.getName(), task);
    }
    project.setTasks(tasks);
    reader.stepOut();
  }

  @Nullable
  private static DefaultExternalTask readTask(IonReader reader) {
    if (reader.next() == null) return null;
    reader.stepIn();
    DefaultExternalTask task = new DefaultExternalTask();
    task.setName(assertNotNull(readString(reader, "name")));
    task.setQName(assertNotNull(readString(reader, "qName")));
    task.setDescription(readString(reader, "description"));
    task.setGroup(readString(reader, "group"));
    task.setType(readString(reader, "type"));
    task.setTest(readBoolean(reader, "isTest"));
    reader.stepOut();
    return task;
  }

  private static void readSourceSets(IonReader reader,
                                     ReadContext context,
                                     DefaultExternalProject project) {
    reader.next();
    reader.stepIn();
    Map<String, DefaultExternalSourceSet> sourceSets = new HashMap<String, DefaultExternalSourceSet>();
    DefaultExternalSourceSet sourceSet;
    while ((sourceSet = readSourceSet(reader, context)) != null) {
      sourceSets.put(sourceSet.getName(), sourceSet);
    }
    project.setSourceSets(sourceSets);
    reader.stepOut();
  }

  @Nullable
  private static DefaultExternalSourceSet readSourceSet(IonReader reader,
                                                        ReadContext context) {
    if (reader.next() == null) return null;
    reader.stepIn();
    DefaultExternalSourceSet sourceSet = new DefaultExternalSourceSet();
    sourceSet.setName(readString(reader, "name"));
    sourceSet.setSourceCompatibility(readString(reader, "sourceCompatibility"));
    sourceSet.setTargetCompatibility(readString(reader, "targetCompatibility"));
    sourceSet.setArtifacts(readFiles(reader));
    sourceSet.getDependencies().addAll(readDependencies(reader, context));
    sourceSet.setSources(readSourceDirectorySets(reader));
    reader.stepOut();
    return sourceSet;
  }

  private static Map<ExternalSystemSourceType, DefaultExternalSourceDirectorySet> readSourceDirectorySets(IonReader reader) {
    reader.next();
    reader.stepIn();
    Map<ExternalSystemSourceType, DefaultExternalSourceDirectorySet> map =
      new HashMap<ExternalSystemSourceType, DefaultExternalSourceDirectorySet>();
    Map.Entry<ExternalSystemSourceType, DefaultExternalSourceDirectorySet> entry;
    while ((entry = readSourceDirectorySet(reader)) != null) {
      map.put(entry.getKey(), entry.getValue());
    }
    reader.stepOut();
    return map;
  }

  @Nullable
  private static Map.Entry<ExternalSystemSourceType, DefaultExternalSourceDirectorySet> readSourceDirectorySet(IonReader reader) {
    if (reader.next() == null) return null;
    reader.stepIn();
    ExternalSystemSourceType sourceType = ExternalSystemSourceType.valueOf(assertNotNull(readString(reader, "sourceType")));
    DefaultExternalSourceDirectorySet directorySet = new DefaultExternalSourceDirectorySet();
    directorySet.setName(assertNotNull(readString(reader, "name")));
    directorySet.setSrcDirs(readFilesSet(reader));
    directorySet.getGradleOutputDirs().addAll(readFiles(reader));
    File outputDir = readFile(reader, "outputDir");
    if (outputDir != null) {
      directorySet.setOutputDir(outputDir);
    }
    directorySet.setInheritedCompilerOutput(readBoolean(reader, "inheritedCompilerOutput"));
    FilePatternSet patternSet = readFilePattern(reader);
    directorySet.setExcludes(patternSet.getExcludes());
    directorySet.setIncludes(patternSet.getIncludes());
    directorySet.setFilters(readFilters(reader));
    reader.stepOut();
    return new AbstractMap.SimpleEntry<ExternalSystemSourceType, DefaultExternalSourceDirectorySet>(sourceType, directorySet);
  }

  private static List<DefaultExternalFilter> readFilters(IonReader reader) {
    reader.next();
    reader.stepIn();
    List<DefaultExternalFilter> list = new ArrayList<DefaultExternalFilter>();
    DefaultExternalFilter filter;
    while ((filter = readFilter(reader)) != null) {
      list.add(filter);
    }
    reader.stepOut();
    return list;
  }

  @Nullable
  private static DefaultExternalFilter readFilter(IonReader reader) {
    if (reader.next() == null) return null;
    DefaultExternalFilter filter = new DefaultExternalFilter();
    reader.stepIn();
    filter.setFilterType(assertNotNull(readString(reader, "filterType")));
    filter.setPropertiesAsJsonMap(assertNotNull(readString(reader, "propertiesAsJsonMap")));
    reader.stepOut();
    return filter;
  }

  private static FilePatternSet readFilePattern(IonReader reader) {
    reader.next();
    reader.stepIn();
    FilePatternSetImpl patternSet = new FilePatternSetImpl();
    patternSet.setIncludes(readStringSet(reader));
    patternSet.setExcludes(readStringSet(reader));
    reader.stepOut();
    return patternSet;
  }

  private static Collection<? extends ExternalDependency> readDependencies(IonReader reader,
                                                                           ReadContext context) {
    List<ExternalDependency> dependencies = new ArrayList<ExternalDependency>();
    reader.next();
    reader.stepIn();
    ExternalDependency dependency;
    while ((dependency = readDependency(reader, context)) != null) {
      dependencies.add(dependency);
    }
    reader.stepOut();
    return dependencies;
  }

  static ExternalDependency readDependency(final IonReader reader, final ReadContext context) {
    if (reader.next() == null) return null;
    reader.stepIn();

    ExternalDependency dependency =
      context.getDependenciesMap().computeIfAbsent(readInt(reader, OBJECT_ID_FIELD), new ObjectFactory<AbstractExternalDependency>() {

        @Override
        public AbstractExternalDependency newInstance() {
          String type = readString(reader, "_type");
          if (ExternalLibraryDependency.class.getSimpleName().equals(type)) {
            return new DefaultExternalLibraryDependency();
          }
          else if (ExternalMultiLibraryDependency.class.getSimpleName().equals(type)) {
            return new DefaultExternalMultiLibraryDependency();
          }
          else if (ExternalProjectDependency.class.getSimpleName().equals(type)) {
            return new DefaultExternalProjectDependency();
          }
          else if (FileCollectionDependency.class.getSimpleName().equals(type)) {
            return new DefaultFileCollectionDependency();
          }
          else if (UnresolvedExternalDependency.class.getSimpleName().equals(type)) {
            return new DefaultUnresolvedExternalDependency();
          }
          else {
            throw new RuntimeException("Unsupported dependency");
          }
        }

        @Override
        public void fill(AbstractExternalDependency externalDependency) {
          readDependencyCommonFields(reader, context, externalDependency);
          if (externalDependency instanceof DefaultExternalLibraryDependency) {
            DefaultExternalLibraryDependency libraryDependency = (DefaultExternalLibraryDependency)externalDependency;
            libraryDependency.setFile(readFile(reader, "file"));
            libraryDependency.setSource(readFile(reader, "source"));
            libraryDependency.setJavadoc(readFile(reader, "javadoc"));
          }
          else if (externalDependency instanceof DefaultExternalMultiLibraryDependency) {
            DefaultExternalMultiLibraryDependency multiLibraryDependency = (DefaultExternalMultiLibraryDependency)externalDependency;
            multiLibraryDependency.getFiles().addAll(readFiles(reader));
            multiLibraryDependency.getSources().addAll(readFiles(reader));
            multiLibraryDependency.getJavadoc().addAll(readFiles(reader));
          }
          else if (externalDependency instanceof DefaultExternalProjectDependency) {
            DefaultExternalProjectDependency projectDependency = (DefaultExternalProjectDependency)externalDependency;
            projectDependency.setProjectPath(readString(reader, "projectPath"));
            projectDependency.setConfigurationName(readString(reader, "configurationName"));
            projectDependency.setProjectDependencyArtifacts(readFiles(reader));
            projectDependency.setProjectDependencyArtifactsSources(readFiles(reader));
          }
          else if (externalDependency instanceof DefaultFileCollectionDependency) {
            DefaultFileCollectionDependency fileCollectionDependency = (DefaultFileCollectionDependency)externalDependency;
            fileCollectionDependency.getFiles().addAll(readFiles(reader));
          }
          else if (externalDependency instanceof DefaultUnresolvedExternalDependency) {
            DefaultUnresolvedExternalDependency unresolvedExternalDependency = (DefaultUnresolvedExternalDependency)externalDependency;
            unresolvedExternalDependency.setFailureMessage(readString(reader, "failureMessage"));
          }
          else {
            throw new RuntimeException("Unsupported dependency type: " + externalDependency.getClass().getName());
          }
        }
      });
    reader.stepOut();
    return dependency;
  }

  private static void readDependencyCommonFields(IonReader reader,
                                                 ReadContext context,
                                                 AbstractExternalDependency dependency) {
    readDependencyId(reader, dependency);
    dependency.setScope(readString(reader, "scope"));
    dependency.setSelectionReason(readString(reader, "selectionReason"));
    dependency.setClasspathOrder(readInt(reader, "classpathOrder"));
    dependency.setExported(readBoolean(reader, "exported"));
    dependency.getDependencies().addAll(readDependencies(reader, context));
  }

  private static void readDependencyId(IonReader reader, AbstractExternalDependency dependency) {
    DefaultExternalDependencyId id = (DefaultExternalDependencyId)dependency.getId();
    reader.next();
    assertFieldName(reader, "id");
    reader.stepIn();
    id.setGroup(readString(reader, "group"));
    id.setName(readString(reader, "name"));
    id.setVersion(readString(reader, "version"));
    id.setPackaging(assertNotNull(readString(reader, "packaging")));
    id.setClassifier(readString(reader, "classifier"));
    reader.stepOut();
  }

  public static class ReadContext {
    private final IntObjectMap<DefaultExternalProject> myProjectsMap = new IntObjectMap<DefaultExternalProject>();

    private final IntObjectMap<AbstractExternalDependency> myDependenciesMap = new IntObjectMap<AbstractExternalDependency>();

    public IntObjectMap<DefaultExternalProject> getProjectsMap() {
      return myProjectsMap;
    }

    public IntObjectMap<AbstractExternalDependency> getDependenciesMap() {
      return myDependenciesMap;
    }
  }

  public static class WriteContext {
    private final ObjectCollector<ExternalProject, IOException> myProjectsCollector =
      new ObjectCollector<ExternalProject, IOException>();
    private final ObjectCollector<ExternalDependency, IOException> myDependenciesCollector =
      new ObjectCollector<ExternalDependency, IOException>();

    public ObjectCollector<ExternalProject, IOException> getProjectsCollector() {
      return myProjectsCollector;
    }

    public ObjectCollector<ExternalDependency, IOException> getDependenciesCollector() {
      return myDependenciesCollector;
    }
  }
}

