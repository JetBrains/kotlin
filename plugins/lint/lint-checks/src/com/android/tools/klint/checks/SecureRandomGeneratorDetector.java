/*
 * Copyright (C) 2013 The Android Open Source Project
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

import static com.android.SdkConstants.CONSTRUCTOR_NAME;
import static com.android.tools.lint.checks.SecureRandomDetector.OWNER_SECURE_RANDOM;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.ClassContext;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Detector.ClassScanner;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Checks for pseudo random number generator initialization issues
 */
public class SecureRandomGeneratorDetector extends Detector implements ClassScanner {

    @SuppressWarnings("SpellCheckingInspection")
    private static final String BLOG_URL
            = "https://android-developers.blogspot.com/2013/08/some-securerandom-thoughts.html";

    /** Whether the random number generator is initialized correctly */
    public static final Issue ISSUE = Issue.create(
            "TrulyRandom", //$NON-NLS-1$
            "Weak RNG",
            "Key generation, signing, encryption, and random number generation may not " +
            "receive cryptographically strong values due to improper initialization of " +
            "the underlying PRNG on Android 4.3 and below.\n" +
            "\n" +
            "If your application relies on cryptographically secure random number generation " +
            "you should apply the workaround described in " + BLOG_URL + " .\n" +
            "\n" +
            "This lint rule is mostly informational; it does not accurately detect whether " +
            "cryptographically secure RNG is required, or whether the workaround has already " +
            "been applied. After reading the blog entry and updating your code if necessary, " +
            "you can disable this lint issue.",

            Category.SECURITY,
            9,
            Severity.WARNING,
            new Implementation(
                    SecureRandomGeneratorDetector.class,
                    Scope.CLASS_FILE_SCOPE))
            .addMoreInfo(BLOG_URL);

    private static final String WRAP = "wrap";                        //$NON-NLS-1$
    private static final String UNWRAP = "unwrap";                    //$NON-NLS-1$
    private static final String INIT = "init";                        //$NON-NLS-1$
    private static final String INIT_SIGN = "initSign";               //$NON-NLS-1$
    private static final String GET_INSTANCE = "getInstance";         //$NON-NLS-1$
    private static final String FOR_NAME = "forName";                 //$NON-NLS-1$
    private static final String JAVA_LANG_CLASS = "java/lang/Class";  //$NON-NLS-1$
    private static final String JAVAX_CRYPTO_KEY_GENERATOR = "javax/crypto/KeyGenerator";
    private static final String JAVAX_CRYPTO_KEY_AGREEMENT = "javax/crypto/KeyAgreement";
    private static final String JAVA_SECURITY_KEY_PAIR_GENERATOR =
            "java/security/KeyPairGenerator";
    private static final String JAVAX_CRYPTO_SIGNATURE = "javax/crypto/Signature";
    private static final String JAVAX_CRYPTO_CIPHER = "javax/crypto/Cipher";
    private static final String JAVAX_NET_SSL_SSLENGINE = "javax/net/ssl/SSLEngine";

    /** Constructs a new {@link SecureRandomGeneratorDetector} */
    public SecureRandomGeneratorDetector() {
    }

    // ---- Implements ClassScanner ----

    @Nullable
    @Override
    public List<String> getApplicableCallOwners() {
        return Arrays.asList(
                JAVAX_CRYPTO_KEY_GENERATOR,
                JAVA_SECURITY_KEY_PAIR_GENERATOR,
                JAVAX_CRYPTO_KEY_AGREEMENT,
                OWNER_SECURE_RANDOM,
                JAVAX_NET_SSL_SSLENGINE,
                JAVAX_CRYPTO_SIGNATURE,
                JAVAX_CRYPTO_CIPHER
        );
    }

    @Nullable
    @Override
    public List<String> getApplicableCallNames() {
        return Collections.singletonList(FOR_NAME);
    }

    /** Location of first call to key generator (etc), if any */
    private Location mLocation;

    /** Whether the issue should be ignored (because we have a workaround, or because
     * we're only targeting correct implementations, etc */
    private boolean mIgnore;

    @Override
    public void checkCall(@NonNull ClassContext context, @NonNull ClassNode classNode,
            @NonNull MethodNode method, @NonNull MethodInsnNode call) {
        if (mIgnore) {
            return;
        }

        String owner = call.owner;
        String name = call.name;

        // Look for the workaround code: if we see a Class.forName on the harmony NativeCrypto,
        // we'll consider that a sign.

        if (name.equals(FOR_NAME)) {
            if (call.getOpcode() != Opcodes.INVOKESTATIC ||
                    !owner.equals(JAVA_LANG_CLASS)) {
                return;
            }
            AbstractInsnNode prev = LintUtils.getPrevInstruction(call);
            if (prev instanceof LdcInsnNode) {
                Object cst = ((LdcInsnNode)prev).cst;
                //noinspection SpellCheckingInspection
                if (cst instanceof String &&
                        "org.apache.harmony.xnet.provider.jsse.NativeCrypto".equals(cst)) {
                    mIgnore = true;
                }
            }
            return;
        }

        // Look for calls that probably require a properly initialized random number generator.
        assert owner.equals(JAVAX_CRYPTO_KEY_GENERATOR)
                || owner.equals(JAVA_SECURITY_KEY_PAIR_GENERATOR)
                || owner.equals(JAVAX_CRYPTO_KEY_AGREEMENT)
                || owner.equals(OWNER_SECURE_RANDOM)
                || owner.equals(JAVAX_CRYPTO_CIPHER)
                || owner.equals(JAVAX_CRYPTO_SIGNATURE)
                || owner.equals(JAVAX_NET_SSL_SSLENGINE) : owner;
        boolean warn = false;

        if (owner.equals(JAVAX_CRYPTO_SIGNATURE)) {
            warn = name.equals(INIT_SIGN);
        } else if (owner.equals(JAVAX_CRYPTO_CIPHER)) {
            if (name.equals(INIT)) {
                int arity = getDescArity(call.desc);
                AbstractInsnNode node = call;
                for (int i = 0; i < arity; i++) {
                    node = LintUtils.getPrevInstruction(node);
                    if (node == null) {
                        break;
                    }
                }
                if (node != null) {
                    int opcode = node.getOpcode();
                    if (opcode == Opcodes.ICONST_3 || // Cipher.WRAP_MODE
                        opcode == Opcodes.ICONST_1) { // Cipher.ENCRYPT_MODE
                        warn = true;
                    }
                }
            }
        } else if (name.equals(GET_INSTANCE) || name.equals(CONSTRUCTOR_NAME)
                || name.equals(WRAP) || name.equals(UNWRAP)) { // For SSLEngine
            warn = true;
        }

        if (warn) {
            if (mLocation != null) {
                return;
            }
            if (context.getMainProject().getMinSdk() > 18) {
                // Fix no longer needed
                mIgnore = true;
                return;
            }

            if (context.getDriver().isSuppressed(ISSUE, classNode, method, call)) {
                mIgnore = true;
            } else {
                mLocation = context.getLocation(call);
            }
        }
    }

    @VisibleForTesting
    static int getDescArity(String desc) {
        int arity = 0;
        // For example, (ILjava/security/Key;)V => 2
        for (int i = 1, max = desc.length(); i < max; i++) {
            char c = desc.charAt(i);
            if (c == ')') {
                break;
            } else if (c == 'L') {
                arity++;
                i = desc.indexOf(';', i);
                assert i != -1 : desc;
            } else {
                arity++;
            }
        }

        return arity;
    }

    @Override
    public void afterCheckProject(@NonNull Context context) {
        if (mLocation != null && !mIgnore) {
            String message = "Potentially insecure random numbers on Android 4.3 and older. "
                    + "Read " + BLOG_URL + " for more info.";
            context.report(ISSUE, mLocation, message);
        }
    }
}
