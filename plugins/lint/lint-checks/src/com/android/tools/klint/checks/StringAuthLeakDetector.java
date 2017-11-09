package com.android.tools.klint.checks;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.klint.detector.api.Category;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.Location;
import com.android.tools.klint.detector.api.JavaContext;
import com.android.tools.klint.detector.api.Severity;
import com.android.tools.klint.detector.api.Scope;

import org.jetbrains.uast.UElement;
import org.jetbrains.uast.ULiteralExpression;
import org.jetbrains.uast.visitor.AbstractUastVisitor;
import org.jetbrains.uast.visitor.UastVisitor;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detector that looks for leaked credentials in strings.
 */
public class StringAuthLeakDetector extends Detector implements Detector.UastScanner {

    /** Looks for hidden code */
    public static final Issue AUTH_LEAK = Issue.create(
            "AuthLeak", "Code might contain an auth leak",
            "Strings in java apps can be discovered by decompiling apps, this lint check looks " +
            "for code which looks like it may contain an url with a username and password",
            Category.SECURITY, 6, Severity.WARNING,
            new Implementation(StringAuthLeakDetector.class, Scope.JAVA_FILE_SCOPE));

    @Nullable
    @Override
    public List<Class<? extends UElement>> getApplicableUastTypes() {
        return Collections.<Class<? extends UElement>>singletonList(ULiteralExpression.class);
    }

    @Nullable
    @Override
    public UastVisitor createUastVisitor(@NonNull JavaContext context) {
        return new AuthLeakChecker(context);
    }

    private static class AuthLeakChecker extends AbstractUastVisitor {
        private final static String LEGAL_CHARS = "([\\w_.!~*\'()%;&=+$,-]+)";      // From RFC 2396
        private final static Pattern AUTH_REGEXP =
                Pattern.compile("([\\w+.-]+)://" + LEGAL_CHARS + ':' + LEGAL_CHARS + '@' +
                        LEGAL_CHARS);

        private final JavaContext mContext;

        private AuthLeakChecker(JavaContext context) {
            mContext = context;
        }
        
        @Override
        public boolean visitLiteralExpression(ULiteralExpression node) {
            if (node.getValue() instanceof String) {
                Matcher matcher = AUTH_REGEXP.matcher((String)node.getValue());
                if (matcher.find()) {
                    String password = matcher.group(3);
                    if (password == null || (password.startsWith("%") && password.endsWith("s"))) {
                        return super.visitLiteralExpression(node);
                    }
                    Location location = mContext.getUastLocation(node);
                    mContext.report(AUTH_LEAK, node, location, "Possible credential leak");
                }
            }
            return super.visitLiteralExpression(node);
        }
    }
}
