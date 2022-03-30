/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class JsDocComment extends JsExpression {
    private final List<JsDocTag> tags;

    public static class JsDocTag {
       private final String label;
       private final Object value;

       public JsDocTag(String label, Object value) {
           this.label = label;
           this.value = value;
       }

       public String getLabel() {
           return label;
       }

       public Object getValue() {
           return value;
       }
    }

    public JsDocComment(List<JsDocTag> tags) {
        this.tags = tags;
    }

    public List<JsDocTag> getTags() {
        return tags;
    }

    public JsDocComment(String tagName, JsNameRef tagValue) {
        tags = getSingletonListOf(tagName, tagValue);
    }

    public JsDocComment(String tagName, String tagValue) {
        tags = getSingletonListOf(tagName, tagValue);
    }

    @Override
    public void accept(JsVisitor v) {
        v.visitDocComment(this);
    }

    @Override
    public void traverse(JsVisitorWithContext v, JsContext ctx) {
    }

    @NotNull
    @Override
    public JsDocComment deepCopy() {
        return new JsDocComment(tags).withMetadataFrom(this);
    }

    private static List<JsDocTag> getSingletonListOf(String tagName, Object tagValue) {
        return Collections.singletonList(new JsDocTag(tagName, tagValue));
    }
}
