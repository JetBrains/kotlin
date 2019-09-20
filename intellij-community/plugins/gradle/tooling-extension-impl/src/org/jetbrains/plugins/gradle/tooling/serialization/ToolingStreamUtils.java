// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.serialization;

import com.amazon.ion.IonReader;
import com.amazon.ion.IonWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.model.ExternalDependency;
import org.jetbrains.plugins.gradle.tooling.serialization.ExternalProjectSerializationService.ReadContext;
import org.jetbrains.plugins.gradle.tooling.serialization.ExternalProjectSerializationService.WriteContext;

import java.io.IOException;

/**
 * @author Vladislav.Soroka
 */
@SuppressWarnings("unused")
public class ToolingStreamUtils extends ToolingStreamApiUtils {

  public static ExternalDependency readDependency(@NotNull IonReader reader, @NotNull ReadContext context) {
    return ExternalProjectSerializationService.readDependency(reader, context);
  }

  public static void writeDependency(@NotNull IonWriter writer,
                                     @NotNull WriteContext context,
                                     @NotNull ExternalDependency dependency) throws IOException {
    ExternalProjectSerializationService.writeDependency(writer, context, dependency);
  }
}
