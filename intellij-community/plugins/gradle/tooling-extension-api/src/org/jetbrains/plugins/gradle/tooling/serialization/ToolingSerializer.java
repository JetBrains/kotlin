// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.serialization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.tooling.util.ClassMap;

import java.io.IOException;

import static java.util.ServiceLoader.load;

/**
 * @author Vladislav.Soroka
 */
public class ToolingSerializer {
  private final DefaultSerializationService myDefaultSerializationService;
  private final ClassMap<SerializationService<?>> mySerializationServices;

  public ToolingSerializer() {
    myDefaultSerializationService = new DefaultSerializationService();
    mySerializationServices = new ClassMap<SerializationService<?>>();
    for (SerializationService<?> serializerService : load(SerializationService.class, SerializationService.class.getClassLoader())) {
      register(serializerService);
    }
  }

  public final void register(@NotNull SerializationService<?> serializerService) {
    mySerializationServices.put(serializerService.getModelClass(), serializerService);
  }

  public byte[] write(@NotNull Object object, @SuppressWarnings("rawtypes") @NotNull Class modelClazz) throws IOException {
    //noinspection unchecked
    return getService(modelClazz).write(object, modelClazz);
  }

  @Nullable
  public <T> T read(@NotNull byte[] object, @NotNull Class<T> modelClazz) throws IOException {
    return getService(modelClazz).read(object, modelClazz);
  }

  @NotNull
  private <T> SerializationService<T> getService(@NotNull Class<T> modelClazz) {
    SerializationService<?> service = mySerializationServices.get(modelClazz);
    //noinspection unchecked
    return service == null ? myDefaultSerializationService : (SerializationService<T>)service;
  }
}
