// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.serialization;

import com.amazon.ion.IonReader;
import com.amazon.ion.IonWriter;
import com.amazon.ion.system.IonBinaryWriterBuilder;
import com.amazon.ion.system.IonReaderBuilder;
import org.jetbrains.plugins.gradle.model.AnnotationProcessingModel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AnnotationProcessingModelSerializationService implements SerializationService<AnnotationProcessingModel> {

  private final WriteContext myWriteContext = new WriteContext();
  private final ReadContext myReadContext = new ReadContext();

  @Override
  public byte[] write(AnnotationProcessingModel annotationProcessingModel, Class<? extends AnnotationProcessingModel> modelClazz) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    IonWriter writer = IonBinaryWriterBuilder.standard().build(out);
    try {
      write(writer, myWriteContext, annotationProcessingModel);
    }
    finally {
      writer.close();
    }
    return out.toByteArray();
  }

  private void write(IonWriter writer, WriteContext context, AnnotationProcessingModel model) {

  }

  @Override
  public AnnotationProcessingModel read(byte[] object, Class<? extends AnnotationProcessingModel> modelClazz) throws IOException {
    IonReader reader = IonReaderBuilder.standard().build(object);
    try {
      return read(reader, myReadContext);
    }
    finally {
      reader.close();
    }
  }

  private AnnotationProcessingModel read(IonReader reader, ReadContext context) {
    return null;
  }

  @Override
  public Class<? extends AnnotationProcessingModel> getModelClass() {
    return AnnotationProcessingModel.class;
  }

  private static class WriteContext {

  }

  private static class ReadContext {

  }
}
