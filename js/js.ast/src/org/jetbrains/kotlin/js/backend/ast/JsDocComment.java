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
import java.util.Map;

public class JsDocComment extends JsExpression {
    private final Map<String, Object> tags;

    public JsDocComment(Map<String, Object> tags) {
        this.tags = tags;
    }

    public Map<String, Object> getTags() {
        return tags;
    }

    public JsDocComment(String tagName, JsNameRef tagValue) {
        tags = Collections.<String, Object>singletonMap(tagName, tagValue);
    }

    public JsDocComment(String tagName, String tagValue) {
        tags = Collections.<String, Object>singletonMap(tagName, tagValue);
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
}
