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

import org.jetbrains.kotlin.js.backend.JsToStringGenerationVisitor;
import org.jetbrains.kotlin.js.backend.ast.metadata.HasMetadata;
import org.jetbrains.kotlin.js.util.TextOutputImpl;

import java.util.LinkedList;
import java.util.List;

abstract class AbstractNode extends HasMetadata implements JsNode {
    private List<JsComment> commentsBefore = null;
    private List<JsComment> commentsAfter = null;

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
        return commentsBefore;
    }

    @Override
    public List<JsComment> getCommentsAfterNode() {
        return commentsAfter;
    }

    @Override
    public void setCommentsBeforeNode(List<JsComment> comments) {
        commentsBefore = comments;
    }

    @Override
    public void setCommentsAfterNode(List<JsComment> comments) {
        commentsAfter = comments;
    }
}
