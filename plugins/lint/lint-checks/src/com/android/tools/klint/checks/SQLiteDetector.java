/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tools.lint.checks;

import static com.android.tools.lint.client.api.JavaParser.TYPE_STRING;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.client.api.JavaParser.ResolvedMethod;
import com.android.tools.lint.client.api.JavaParser.ResolvedNode;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.ConstantEvaluator;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import lombok.ast.AstVisitor;
import lombok.ast.Expression;
import lombok.ast.MethodInvocation;
import lombok.ast.Node;

/**
 * Detector which looks for problems related to SQLite usage
 */
public class SQLiteDetector extends Detector implements Detector.JavaScanner {
    private static final Implementation IMPLEMENTATION = new Implementation(
          SQLiteDetector.class, Scope.JAVA_FILE_SCOPE);

    /** Using STRING instead of TEXT for columns */
    public static final Issue ISSUE = Issue.create(
            "SQLiteString", //$NON-NLS-1$
            "Using STRING instead of TEXT",

            "In SQLite, any column can store any data type; the declared type for a column " +
            "is more of a hint as to what the data should be cast to when stored.\n" +
            "\n" +
            "There are many ways to store a string. `TEXT`, `VARCHAR`, `CHARACTER` and `CLOB` " +
            "are string types, *but `STRING` is not*. Columns defined as STRING are actually " +
            "numeric.\n" +
            "\n" +
            "If you try to store a value in a numeric column, SQLite will try to cast it to a " +
            "float or an integer before storing. If it can't, it will just store it as a " +
            "string.\n" +
            "\n" +
            "This can lead to some subtle bugs. For example, when SQLite encounters a string " +
            "like `1234567e1234`, it will parse it as a float, but the result will be out of " +
            "range for floating point numbers, so `Inf` will be stored! Similarly, strings " +
            "that look like integers will lose leading zeroes.\n" +
            "\n" +
            "To fix this, you can change your schema to use a `TEXT` type instead.",

            Category.CORRECTNESS,
            5,
            Severity.WARNING,
            IMPLEMENTATION)
            .addMoreInfo("https://www.sqlite.org/datatype3.html"); //$NON-NLS-1$

    // ---- Implements Detector.JavaScanner ----

    @Override
    public List<String> getApplicableMethodNames() {
        return Collections.singletonList("execSQL"); //$NON-NLS-1$
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @Nullable AstVisitor visitor,
            @NonNull MethodInvocation node) {
        ResolvedNode resolved = context.resolve(node);
        if (!(resolved instanceof ResolvedMethod)) {
            return;
        }

        ResolvedMethod method = (ResolvedMethod) resolved;
        if (!method.getContainingClass().matches("android.database.sqlite.SQLiteDatabase")) {
            return;
        }

        // Try to resolve the String and look for STRING keys
        if (method.getArgumentCount() > 0
                && method.getArgumentType(0).matchesSignature(TYPE_STRING)
                && node.astArguments().size() == method.getArgumentCount()) {
            Iterator<Expression> iterator = node.astArguments().iterator();
            Node argument = iterator.next();
            String sql = ConstantEvaluator.evaluateString(context, argument, true);
            if (sql != null && (sql.startsWith("CREATE TABLE") || sql.startsWith("ALTER TABLE"))
                    && sql.matches(".*\\bSTRING\\b.*")) {
                String message = "Using column type STRING; did you mean to use TEXT? "
                        + "(STRING is a numeric type and its value can be adjusted; for example,"
                        + "strings that look like integers can drop leading zeroes. See issue "
                        + "explanation for details.)";
                context.report(ISSUE, node, context.getLocation(node), message);
            }
        }
    }
}
