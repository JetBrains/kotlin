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

package org.jetbrains.kotlin.js.common;

import org.jetbrains.kotlin.js.Source;

public class SourceInfoImpl implements SourceInfo {
    protected Source source = null;
    protected int line = -1;
    protected int column = -1;
    protected int start = -1;
    protected int length = -1;

    public SourceInfoImpl(Source source, int line, int column, int start, int length) {
        this.source = source;
        this.line = line;
        this.column = column;
        this.start = start;
        this.length = length;
    }

    @Override
    public Source getSource() {
        return source;
    }

    @Override
    public int getLine() {
        return line;
    }

    @Override
    public int getColumn() {
        return column;
    }

    @Override
    public int getStart() {
        return start;
    }

    @Override
    public int getLength() {
        return length;
    }
}
