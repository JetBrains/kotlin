// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.serialization;

import org.jetbrains.plugins.gradle.tooling.ModelBuilderService;

import java.io.IOException;

/**
 * The service should provide more efficient data serialization of objects created by {@link ModelBuilderService}.
 * The order of the read/write operations is the same, so you do not need to serialize an object instance more than one time.
 * Use {@link org.jetbrains.plugins.gradle.tooling.util.ObjectCollector} to write only a single 'objectID' field for subsequent writes of the same instance.
 * Use {@link org.jetbrains.plugins.gradle.tooling.util.IntObjectMap} to read the same instance by reference.
 *
 * @see ModelBuilderService
 * @author Vladislav.Soroka
 */
public interface SerializationService<T> {
  byte[] write(T object, Class<? extends T> modelClazz) throws IOException;

  T read(byte[] object, Class<? extends T> modelClazz) throws IOException;

  Class<? extends T> getModelClass();
}
