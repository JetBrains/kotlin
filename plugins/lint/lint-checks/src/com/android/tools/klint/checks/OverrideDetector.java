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

import static com.android.SdkConstants.CONSTRUCTOR_NAME;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PROTECTED;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

import com.android.annotations.NonNull;
import com.android.tools.lint.client.api.LintDriver;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.ClassContext;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Detector.ClassScanner;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Checks for accidental overrides
 */
public class OverrideDetector extends Detector implements ClassScanner {
    /** Accidental overrides */
    public static final Issue ISSUE = Issue.create(
            "DalvikOverride", //$NON-NLS-1$
            "Method considered overridden by Dalvik",

            "The Android virtual machine will treat a package private method in one " +
            "class as overriding a package private method in its super class, even if " +
            "they are in separate packages. This may be surprising, but for compatibility " +
            "reasons the behavior has not been changed (yet).\n" +
            "\n" +
            "If you really did intend for this method to override the other, make the " +
            "method `protected` instead.\n" +
            "\n" +
            "If you did *not* intend the override, consider making the method private, or " +
            "changing its name or signature.",

            Category.CORRECTNESS,
            7,
            Severity.ERROR,
            new Implementation(
                    OverrideDetector.class,
                    EnumSet.of(Scope.ALL_CLASS_FILES)));

    /** map from owner class name to JVM signatures for its package private methods  */
    private final Map<String, Set<String>> mPackagePrivateMethods = Maps.newHashMap();

    /** Map from owner to signature to super class being overridden */
    private Map<String, Map<String, String>> mErrors;

    /**
     * Map from owner to signature to corresponding location. When there are
     * errors a single error can have locations for both the overriding and
     * overridden methods.
     */
    private Map<String, Map<String, Location>> mLocations;

    /** Constructs a new {@link OverrideDetector} */
    public OverrideDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.NORMAL;
    }

    @Override
    public void afterCheckProject(@NonNull Context context) {
        // Process the check in two passes:
        //
        // In the first pass, gather the full set of package private methods for
        // each class.
        // When all classes have been processed at the end of the first pass,
        // find out whether any of the methods are potentially overriding those
        // in its super classes.
        //
        // If so, request a second pass. In the second pass, we gather full locations
        // for both the base and overridden method calls, and store these.
        // If the location is found to be in a suppressed context, remove that error
        // entry.
        //
        // At the end of the second pass, we generate the errors, combining locations
        // from both the overridden and overriding methods.
        if (context.getPhase() == 1) {
            Set<String> classes = mPackagePrivateMethods.keySet();
            LintDriver driver = context.getDriver();
            for (String owner : classes) {
                Set<String> methods = mPackagePrivateMethods.get(owner);
                String superClass = driver.getSuperClass(owner);
                int packageIndex = owner.lastIndexOf('/');
                while (superClass != null) {
                    int superPackageIndex = superClass.lastIndexOf('/');

                    // Only compare methods that differ in packages
                    if (packageIndex == -1 || superPackageIndex != packageIndex ||
                            !owner.regionMatches(0, superClass, 0, packageIndex)) {
                        Set<String> superMethods = mPackagePrivateMethods.get(superClass);
                        if (superMethods != null) {
                            SetView<String> intersection = Sets.intersection(methods,
                                    superMethods);
                            if (!intersection.isEmpty()) {
                                if (mLocations == null) {
                                    mLocations = Maps.newHashMap();
                                }
                                // We need a separate data structure to keep track of which
                                // signatures are in error,
                                if (mErrors == null) {
                                    mErrors = Maps.newHashMap();
                                }

                                for (String signature : intersection) {
                                    Map<String, Location> locations = mLocations.get(owner);
                                    if (locations == null) {
                                        locations = Maps.newHashMap();
                                        mLocations.put(owner, locations);
                                    }
                                    locations.put(signature, null);

                                    locations = mLocations.get(superClass);
                                    if (locations == null) {
                                        locations = Maps.newHashMap();
                                        mLocations.put(superClass, locations);
                                    }
                                    locations.put(signature, null);


                                    Map<String, String> errors = mErrors.get(owner);
                                    if (errors == null) {
                                        errors = Maps.newHashMap();
                                        mErrors.put(owner, errors);
                                    }
                                    errors.put(signature, superClass);
                                }
                            }
                        }
                    }
                    superClass = driver.getSuperClass(superClass);
                }
            }

            if (mErrors != null) {
                context.requestRepeat(this, ISSUE.getImplementation().getScope());
            }
        } else {
            assert context.getPhase() == 2;

            for (Entry<String, Map<String, String>> ownerEntry : mErrors.entrySet()) {
                String owner = ownerEntry.getKey();
                Map<String, String> methodToSuper = ownerEntry.getValue();
                for (Entry<String, String> entry : methodToSuper.entrySet()) {
                    String signature = entry.getKey();
                    String superClass = entry.getValue();

                    Map<String, Location> ownerLocations = mLocations.get(owner);
                    if (ownerLocations != null) {
                        Location location = ownerLocations.get(signature);
                        if (location != null) {
                            Map<String, Location> superLocations = mLocations.get(superClass);
                            if (superLocations != null) {
                                Location superLocation = superLocations.get(signature);
                                if (superLocation != null) {
                                    location.setSecondary(superLocation);
                                    superLocation.setMessage(
                                            "This method is treated as overridden");
                                }
                            }
                            String methodName = signature;
                            int index = methodName.indexOf('(');
                            if (index != -1) {
                                methodName = methodName.substring(0, index);
                            }
                            String message = String.format(
                                    "This package private method may be unintentionally " +
                                    "overriding `%1$s` in `%2$s`", methodName,
                                    ClassContext.getFqcn(superClass));
                            context.report(ISSUE, location, message);
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("rawtypes") // ASM5 API
    @Override
    public void checkClass(@NonNull ClassContext context, @NonNull ClassNode classNode) {
        if (!context.getProject().getReportIssues()) {
            // If this is a library project not being analyzed, ignore it
            return;
        }

        List methodList = classNode.methods;
        if (context.getPhase() == 1) {
            for (Object m : methodList) {
                MethodNode method = (MethodNode) m;
                int access = method.access;
                // Only record non-static package private methods
                if ((access & (ACC_STATIC|ACC_PRIVATE|ACC_PROTECTED|ACC_PUBLIC)) != 0) {
                    continue;
                }

                // Ignore constructors too
                if (CONSTRUCTOR_NAME.equals(method.name)) {
                    continue;
                }

                String owner = classNode.name;
                Set<String> methods = mPackagePrivateMethods.get(owner);
                if (methods == null) {
                    methods = Sets.newHashSetWithExpectedSize(methodList.size());
                    mPackagePrivateMethods.put(owner, methods);
                }
                methods.add(method.name + method.desc);
            }
        } else {
            assert context.getPhase() == 2;
            Map<String, Location> methods = mLocations.get(classNode.name);
            if (methods == null) {
                // No locations needed from this class
                return;
            }

            for (Object m : methodList) {
                MethodNode method = (MethodNode) m;

                String signature = method.name + method.desc;
                if (methods.containsKey(signature)){
                    if (context.getDriver().isSuppressed(ISSUE, classNode,
                            method, null)) {
                        Map<String, String> errors = mErrors.get(classNode.name);
                        if (errors != null) {
                            errors.remove(signature);
                        }
                        continue;
                    }

                    Location location = context.getLocation(method, classNode);
                    methods.put(signature, location);
                    String description = ClassContext.createSignature(classNode.name,
                            method.name, method.desc);
                    location.setClientData(description);
                }
            }
        }
    }
}
