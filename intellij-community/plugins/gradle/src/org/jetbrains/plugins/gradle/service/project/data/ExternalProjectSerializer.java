/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.plugins.gradle.service.project.data;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.CollectionSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.esotericsoftware.kryo.serializers.MapSerializer;
import com.esotericsoftware.kryo.util.Util;
import com.esotericsoftware.minlog.Log;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.StreamUtil;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.DefaultExternalDependencyId;
import org.jetbrains.plugins.gradle.model.*;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.*;

/**
 * @author Vladislav.Soroka
 */
public class ExternalProjectSerializer {
  private static final Logger LOG = Logger.getInstance(ExternalProjectSerializer.class);

  private final Kryo myKryo;

  public ExternalProjectSerializer() {
    myKryo = new Kryo() {
      @Override
      public <T> T newInstance(Class<T> type) {
        LOG.error("Serializing default type: " + type);
        return super.newInstance(type);
      }
    };
    Util.isAndroid = false;
    configureKryo();
  }

  private void configureKryo() {
    myKryo.setAutoReset(true);

    myKryo.setRegistrationRequired(true);
    Log.set(Log.LEVEL_WARN);

    myKryo.register(ArrayList.class, new CollectionSerializer() {
      @Override
      protected Collection create(Kryo kryo, Input input, Class<Collection> type) {
        return new ArrayList();
      }
    });
    myKryo.register(HashMap.class, new MapSerializer() {
      @Override
      protected Map create(Kryo kryo, Input input, Class<Map> type) {
        return new HashMap();
      }
    });
    myKryo.register(HashSet.class, new CollectionSerializer() {
      @Override
      protected Collection create(Kryo kryo, Input input, Class<Collection> type) {
        return new HashSet();
      }
    });

    myKryo.register(File.class, new FileSerializer());
    myKryo.register(DefaultExternalProject.class, new FieldSerializer<DefaultExternalProject>(myKryo, DefaultExternalProject.class) {
      @Override
      protected DefaultExternalProject create(Kryo kryo, Input input, Class<DefaultExternalProject> type) {
        return new DefaultExternalProject();
      }
    });

    myKryo.register(DefaultExternalTask.class, new FieldSerializer<DefaultExternalTask>(myKryo, DefaultExternalTask.class) {
      @Override
      protected DefaultExternalTask create(Kryo kryo, Input input, Class<DefaultExternalTask> type) {
        return new DefaultExternalTask();
      }
    });

    myKryo.register(DefaultExternalPlugin.class, new FieldSerializer<DefaultExternalPlugin>(myKryo, DefaultExternalPlugin.class) {
      @Override
      protected DefaultExternalPlugin create(Kryo kryo, Input input, Class<DefaultExternalPlugin> type) {
        return new DefaultExternalPlugin();
      }
    });

    myKryo.register(DefaultExternalSourceSet.class, new FieldSerializer<DefaultExternalSourceSet>(myKryo, DefaultExternalSourceSet.class) {
      @Override
      protected DefaultExternalSourceSet create(Kryo kryo, Input input, Class<DefaultExternalSourceSet> type) {
        return new DefaultExternalSourceSet();
      }
    });

    myKryo.register(
      DefaultExternalSourceDirectorySet.class,
      new FieldSerializer<DefaultExternalSourceDirectorySet>(myKryo, DefaultExternalSourceDirectorySet.class) {
        @Override
        protected DefaultExternalSourceDirectorySet create(Kryo kryo, Input input, Class<DefaultExternalSourceDirectorySet> type) {
          return new DefaultExternalSourceDirectorySet();
        }
      }
    );

    myKryo.register(DefaultExternalFilter.class, new FieldSerializer<DefaultExternalFilter>(myKryo, DefaultExternalFilter.class) {
      @Override
      protected DefaultExternalFilter create(Kryo kryo, Input input, Class<DefaultExternalFilter> type) {
        return new DefaultExternalFilter();
      }
    });

    myKryo.register(ExternalSystemSourceType.class, new DefaultSerializers.EnumSerializer(ExternalSystemSourceType.class));

    myKryo.register(
      DefaultExternalProjectDependency.class,
      new FieldSerializer<DefaultExternalProjectDependency>(myKryo, DefaultExternalProjectDependency.class) {
        @Override
        protected DefaultExternalProjectDependency create(Kryo kryo, Input input, Class<DefaultExternalProjectDependency> type) {
          return new DefaultExternalProjectDependency();
        }
      }
    );

    myKryo.register(
      DefaultFileCollectionDependency.class,
      new FieldSerializer<DefaultFileCollectionDependency>(myKryo, DefaultFileCollectionDependency.class) {
        @Override
        protected DefaultFileCollectionDependency create(Kryo kryo, Input input, Class<DefaultFileCollectionDependency> type) {
          return new DefaultFileCollectionDependency();
        }
      }
    );

    myKryo.register(
      DefaultExternalLibraryDependency.class,
      new FieldSerializer<DefaultExternalLibraryDependency>(myKryo, DefaultExternalLibraryDependency.class) {
        @Override
        protected DefaultExternalLibraryDependency create(Kryo kryo, Input input, Class<DefaultExternalLibraryDependency> type) {
          return new DefaultExternalLibraryDependency();
        }
      }
    );

    myKryo.register(
      DefaultUnresolvedExternalDependency.class,
      new FieldSerializer<DefaultUnresolvedExternalDependency>(myKryo, DefaultUnresolvedExternalDependency.class) {
        @Override
        protected DefaultUnresolvedExternalDependency create(Kryo kryo, Input input, Class<DefaultUnresolvedExternalDependency> type) {
          return new DefaultUnresolvedExternalDependency();
        }
      }
    );

    myKryo.register(
      DefaultExternalDependencyId.class,
      new FieldSerializer<DefaultExternalDependencyId>(myKryo, DefaultExternalDependencyId.class) {
        @Override
        protected DefaultExternalDependencyId create(Kryo kryo, Input input, Class<DefaultExternalDependencyId> type) {
          return new DefaultExternalDependencyId();
        }
      }
    );

    myKryo.register(
      FilePatternSetImpl.class,
      new FieldSerializer<FilePatternSetImpl>(myKryo, FilePatternSetImpl.class) {
        @Override
        protected FilePatternSetImpl create(Kryo kryo, Input input, Class<FilePatternSetImpl> type) {
          return new FilePatternSetImpl(new LinkedHashSet<>(), new LinkedHashSet<>());
        }
      }
    );

    myKryo.register(LinkedHashSet.class, new CollectionSerializer() {
      @Override
      protected Collection create(Kryo kryo, Input input, Class<Collection> type) {
        return new LinkedHashSet();
      }
    });
    myKryo.register(HashSet.class, new CollectionSerializer() {
      @Override
      protected Collection create(Kryo kryo, Input input, Class<Collection> type) {
        return new HashSet();
      }
    });
    myKryo.register(THashSet.class, new CollectionSerializer() {
      @Override
      protected Collection create(Kryo kryo, Input input, Class<Collection> type) {
        return new THashSet();
      }
    });
    myKryo.register(Set.class, new CollectionSerializer() {
      @Override
      protected Collection create(Kryo kryo, Input input, Class<Collection> type) {
        return new HashSet();
      }
    });
    myKryo.register(THashMap.class, new MapSerializer() {
      @Override
      protected Map create(Kryo kryo, Input input, Class<Map> type) {
        return new THashMap();
      }
    });
  }


  public void save(@NotNull ExternalProject externalProject) {
    Output output = null;
    try {
      final File externalProjectDir = externalProject.getProjectDir();
      final File configurationFile =
        getProjectConfigurationFile(new ProjectSystemId(externalProject.getExternalSystemId()), externalProjectDir);
      if (!FileUtil.createParentDirs(configurationFile)) return;

      output = new Output(new FileOutputStream(configurationFile));
      myKryo.writeObject(output, externalProject);

      LOG.debug("Data saved for imported project from: " + externalProjectDir.getPath());
    }
    catch (FileNotFoundException e) {
      LOG.error(e);
    }
    finally {
      StreamUtil.closeStream(output);
    }
  }

  @Nullable
  public ExternalProject load(@NotNull ProjectSystemId externalSystemId, File externalProjectPath) {
    LOG.debug("Attempt to load project data from: " + externalProjectPath);
    ExternalProject externalProject = null;
    try {
      final File configurationFile = getProjectConfigurationFile(externalSystemId, externalProjectPath);
      if (!configurationFile.isFile()) return null;

      Input input = new Input(new FileInputStream(configurationFile));
      try {
        externalProject = myKryo.readObject(input, DefaultExternalProject.class);
      }
      finally {
        StreamUtil.closeStream(input);
      }
    }
    catch (Exception e) {
      LOG.debug(e);
    }
    if (externalProject != null) {
      LOG.debug("Loaded project: " + externalProject.getProjectDir());
    }
    return externalProject;
  }

  private static File getProjectConfigurationFile(ProjectSystemId externalSystemId, File externalProjectPath) {
    return new File(getProjectConfigurationDir(externalSystemId),
                    Integer.toHexString(ExternalSystemUtil.fileHashCode(externalProjectPath)) + "/project.dat");
  }

  private static File getProjectConfigurationDir(ProjectSystemId externalSystemId) {
    return getPluginSystemDir(externalSystemId, "Projects");
  }

  private static File getPluginSystemDir(ProjectSystemId externalSystemId, String folder) {
    return new File(PathManager.getSystemPath(), externalSystemId.getId().toLowerCase() + "/" + folder).getAbsoluteFile();
  }

  private static class FileSerializer extends Serializer<File> {
    private final Kryo myStdKryo;

    FileSerializer() {
      myStdKryo = new Kryo();
      myStdKryo.register(File.class);
      myStdKryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
    }

    @Override
    public void write(Kryo kryo, Output output, File object) {
      myStdKryo.writeObject(output, object);
    }

    @Override
    public File read(Kryo kryo, Input input, Class<File> type) {
      File file = myStdKryo.readObject(input, File.class);
      return new File(file.getPath());
    }
  }
}
