/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.ide.favoritesTreeView;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;

import java.io.IOException;
import java.util.List;

public class NoteSerializable implements WorkingSetSerializable<NoteNode, NoteNode> {

  @Override
  public String getId() {
    return NoteNode.class.getName();
  }

  @Override
  public int getVersion() {
    return 0;
  }

  @Override
  public void serializeMe(NoteNode t, StringBuilder oos) throws IOException {
    oos.append(StringUtil.escapeXmlEntities(t.getText()));
    oos.append("<>");
    oos.append(t.isReadonly());
    oos.append("<>");
  }

  @Override
  public NoteNode deserializeMe(Project project, String ois) throws IOException {
    final List<String> strings = StringUtil.split(ois, "<>", true);
    if (strings.size() == 2) {
      return new NoteNode(StringUtil.unescapeXmlEntities(strings.get(0)), Boolean.parseBoolean(strings.get(1)));
    }
    return null;
  }

  @Override
  public NoteNode deserializeMeInvalid(Project project, String ois) throws IOException {
    return null;
  }
}
