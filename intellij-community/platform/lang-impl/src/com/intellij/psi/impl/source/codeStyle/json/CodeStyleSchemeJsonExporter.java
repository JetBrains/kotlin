// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl.source.codeStyle.json;

import com.google.gson.*;
import com.intellij.application.options.codeStyle.properties.*;
import com.intellij.openapi.options.SchemeExporter;
import com.intellij.openapi.project.Project;
import com.intellij.psi.codeStyle.CodeStyleScheme;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class CodeStyleSchemeJsonExporter extends SchemeExporter<CodeStyleScheme> {

  public static final String CODE_STYLE_JSON_EXT = "codestyle.json";

  @Override
  public void exportScheme(@Nullable Project project, @NotNull CodeStyleScheme scheme, @NotNull OutputStream outputStream) {
    exportScheme(scheme, outputStream, null);
  }

  public void exportScheme(@NotNull CodeStyleScheme scheme, @NotNull OutputStream outputStream, @Nullable List<String> languageNames) {
    GsonBuilder builder = new GsonBuilder();
    builder.setPrettyPrinting();
    builder.registerTypeAdapter(
      CodeStyleSchemeJsonDescriptor.PropertyListHolder.class,
      new JsonSerializer<CodeStyleSchemeJsonDescriptor.PropertyListHolder>() {
        @Override
        public JsonElement serialize(CodeStyleSchemeJsonDescriptor.PropertyListHolder src,
                                     Type typeOfSrc,
                                     JsonSerializationContext context) {
          JsonObject o = new JsonObject();
          for (AbstractCodeStylePropertyMapper mapper : src) {
            JsonObject langProperties = serializeMapper(mapper);
            o.add(
              mapper.getLanguageDomainId(),
              langProperties
            );
          }
          return o;
        }
      });
    Gson gson = builder.create();
    String json = gson.toJson(new CodeStyleSchemeJsonDescriptor(scheme, languageNames));
    try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
      writer.write(json);
    }
  }

  private static JsonObject serializeMapper(AbstractCodeStylePropertyMapper src) {
    JsonObject o = new JsonObject();
    for (String name : src.enumProperties()) {
      CodeStylePropertyAccessor accessor = src.getAccessor(name);
      if (accessor != null) {
        Object externalized = accessor.get();
        if (externalized instanceof String) {
          o.addProperty(name, (String)externalized);
        }
        else if (externalized instanceof Integer) {
          o.addProperty(name, (Integer)externalized);
        }
        else if (externalized instanceof Boolean) {
          o.addProperty(name, (Boolean)externalized);
        }
        else if (externalized instanceof List) {
          final JsonArray array = new JsonArray();
          for (Object element : (List)externalized) {
            if (element instanceof String) {
              array.add((String)element);
            }
            else if (element instanceof Integer) {
              array.add((Integer)element);
            }
          }
          o.add(name, array);
        }
      }
    }
    return o;
  }


  @Override
  public String getExtension() {
    return CODE_STYLE_JSON_EXT;
  }


}
