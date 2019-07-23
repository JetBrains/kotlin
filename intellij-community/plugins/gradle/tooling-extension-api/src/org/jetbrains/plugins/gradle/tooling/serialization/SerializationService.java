// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.serialization;

import java.io.IOException;

/**
 * @author Vladislav.Soroka
 */
public interface SerializationService<T> {
  byte[] write(T object, Class<? extends T> modelClazz) throws IOException;

  T read(byte[] object, Class<? extends T> modelClazz) throws IOException;

  Class<? extends T> getModelClass();
}
