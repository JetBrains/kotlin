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
import org.jetbrains.kotlin.js.backend.JsToStringGenerationVisitor;
import org.jetbrains.kotlin.js.backend.ast.metadata.HasMetadata;
import org.jetbrains.kotlin.js.backend.ast.metadata.HasMetadataImpl;
import org.jetbrains.kotlin.js.util.TextOutputImpl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

abstract class AbstractNode implements JsNode, HasMetadata {
    private static class Internals extends HasMetadataImpl {
        List<JsComment> commentsBefore = null;
        List<JsComment> commentsAfter = null;
    }

    private Internals internals = null;

    private Internals getInternals() {
        if (internals == null) {
            internals = new Internals();
        }
        return internals;
    }

    @Override
    public String toString() {
        TextOutputImpl out = new TextOutputImpl();
        new JsToStringGenerationVisitor(out).accept(this);
        return out.toString();
    }

    @SuppressWarnings("unchecked")
    public <T extends HasMetadata & JsNode> T withMetadataFrom(T other) {
        this.copyMetadataFrom(other);
        Object otherSource = other.getSource();
        if (otherSource != null) {
            source(otherSource);
        }
        setCommentsBeforeNode(other.getCommentsBeforeNode());
        setCommentsAfterNode(other.getCommentsAfterNode());
        return (T) this;
    }

    @Override
    public List<JsComment> getCommentsBeforeNode() {
        return internals == null ? null : internals.commentsBefore;
    }

    @Override
    public List<JsComment> getCommentsAfterNode() {
        return internals == null ? null : internals.commentsAfter;
    }

    @Override
    public void setCommentsBeforeNode(List<JsComment> comments) {
        getInternals().commentsBefore = comments;
    }

    @Override
    public void setCommentsAfterNode(List<JsComment> comments) {
        getInternals().commentsAfter = comments;
    }

    @Override
    public <T> T getData(@NotNull String key) {
        return getInternals().getData(key);
    }

    @Override
    public <T> void setData(@NotNull String key, T value) {
        getInternals().setData(key, value);
    }

    @Override
    public boolean hasData(@NotNull String key) {
        return internals != null && internals.hasData(key);
    }

    @Override
    public void removeData(@NotNull String key) {
        if (internals != null) {
            internals.removeData(key);
        }
    }

    @Override
    public void copyMetadataFrom(@NotNull HasMetadata other) {
        if (!other.getRawMetadata().isEmpty()) {
            getInternals().copyMetadataFrom(other);
        }
    }

    @NotNull
    @Override
    public Map<String, Object> getRawMetadata() {
        return internals != null ? internals.getRawMetadata() : Collections.emptyMap();
    }
}
