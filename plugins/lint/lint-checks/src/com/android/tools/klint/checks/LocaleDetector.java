/*
 * Copyright (C) 2012 The Android Open Source Project
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

import static com.android.SdkConstants.FORMAT_METHOD;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.ClassContext;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Detector.ClassScanner;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

import java.util.Arrays;
import java.util.List;

/**
 * Checks for errors related to locale handling
 */
public class LocaleDetector extends Detector implements ClassScanner {
    private static final Implementation IMPLEMENTATION = new Implementation(
            LocaleDetector.class,
            Scope.CLASS_FILE_SCOPE);

    /** Calling risky convenience methods */
    public static final Issue STRING_LOCALE = Issue.create(
            "DefaultLocale", //$NON-NLS-1$
            "Implied default locale in case conversion",

            "Calling `String#toLowerCase()` or `#toUpperCase()` *without specifying an " +
            "explicit locale* is a common source of bugs. The reason for that is that those " +
            "methods will use the current locale on the user's device, and even though the " +
            "code appears to work correctly when you are developing the app, it will fail " +
            "in some locales. For example, in the Turkish locale, the uppercase replacement " +
            "for `i` is *not* `I`.\n" +
            "\n" +
            "If you want the methods to just perform ASCII replacement, for example to convert " +
            "an enum name, call `String#toUpperCase(Locale.US)` instead. If you really want to " +
            "use the current locale, call `String#toUpperCase(Locale.getDefault())` instead.",

            Category.CORRECTNESS,
            6,
            Severity.WARNING,
            IMPLEMENTATION)
            .addMoreInfo(
            "http://developer.android.com/reference/java/util/Locale.html#default_locale"); //$NON-NLS-1$

    static final String DATE_FORMAT_OWNER = "java/text/SimpleDateFormat"; //$NON-NLS-1$
    private static final String STRING_OWNER = "java/lang/String";                //$NON-NLS-1$

    /** Constructs a new {@link LocaleDetector} */
    public LocaleDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    // ---- Implements ClassScanner ----

    @Override
    @Nullable
    public List<String> getApplicableCallNames() {
        return Arrays.asList(
                "toLowerCase", //$NON-NLS-1$
                "toUpperCase", //$NON-NLS-1$
                FORMAT_METHOD
        );
    }

    @Override
    public void checkCall(@NonNull ClassContext context, @NonNull ClassNode classNode,
            @NonNull MethodNode method, @NonNull MethodInsnNode call) {
        String owner = call.owner;
        if (!owner.equals(STRING_OWNER)) {
            return;
        }

        String desc = call.desc;
        String name = call.name;

        if (name.equals(FORMAT_METHOD)) {
            // Only check the non-locale version of String.format
            if (!desc.equals("(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;")) { //$NON-NLS-1$
                return;
            }
            // Find the formatting string
            Analyzer analyzer = new Analyzer(new SourceInterpreter() {
                @Override
                public SourceValue newOperation(AbstractInsnNode insn) {
                    if (insn.getOpcode() == Opcodes.LDC) {
                        Object cst = ((LdcInsnNode) insn).cst;
                        if (cst instanceof String) {
                            return new StringValue(1, (String) cst);
                        }
                    }
                    return super.newOperation(insn);
                }
            });
            try {
                Frame[] frames = analyzer.analyze(classNode.name, method);
                InsnList instructions = method.instructions;
                Frame frame = frames[instructions.indexOf(call)];
                if (frame.getStackSize() == 0) {
                    return;
                }
                SourceValue stackValue = (SourceValue) frame.getStack(0);
                if (stackValue instanceof StringValue) {
                    String format = ((StringValue) stackValue).getString();
                    if (format != null && StringFormatDetector.isLocaleSpecific(format)) {
                        Location location = context.getLocation(call);
                        String message =
                            "Implicitly using the default locale is a common source of bugs: " +
                            "Use `String.format(Locale, ...)` instead";
                        context.report(STRING_LOCALE, method, call, location, message);
                    }
                }
            } catch (AnalyzerException e) {
                context.log(e, null);
            }
        } else {
            if (desc.equals("()Ljava/lang/String;")) {   //$NON-NLS-1$
                Location location = context.getLocation(call);
                String message = String.format(
                    "Implicitly using the default locale is a common source of bugs: " +
                    "Use `%1$s(Locale)` instead", name);
                context.report(STRING_LOCALE, method, call, location, message);
            }
        }
    }

    private static class StringValue extends SourceValue {
        private final String mString;

        StringValue(int size, String string) {
            super(size);
            mString = string;
        }

        String getString() {
            return mString;
        }

        @Override
        public int getSize() {
            return 1;
        }
    }
}
