/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.breakpoints;

import com.intellij.debugger.DebuggerBundle;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.requests.RequestManagerImpl;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.ui.breakpoints.FilteredRequestor;
import com.intellij.debugger.ui.breakpoints.LineBreakpoint;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.EverythingGlobalScope;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.sun.jdi.ClassNotPreparedException;
import com.sun.jdi.Location;
import com.sun.jdi.ObjectCollectedException;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.request.BreakpointRequest;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.debugger.breakpoints.properties.JavaLineBreakpointProperties;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// Copied from com.intellij.debugger.ui.breakpoints.LineBreakpoint.
// Changed parts are marked with '// MODIFICATION: ' comments.
// This should be deleted when IDEA replaces the 'MethodBytecodeUtil.removeSameLineLocations' with overload-able method call.
public abstract class KotlinLineBreakpointBase extends LineBreakpoint<JavaLineBreakpointProperties> {
    private static final Logger LOG = Logger.getInstance(KotlinLineBreakpointBase.class);

    protected KotlinLineBreakpointBase(Project project, XBreakpoint xBreakpoint) {
        super(project, xBreakpoint);
    }

    @Nullable
    private static BreakpointRequest createLocationBreakpointRequest(@NotNull FilteredRequestor requestor,
            @Nullable Location location,
            @NotNull DebugProcessImpl debugProcess) {
        if (location != null) {
            RequestManagerImpl requestsManager = debugProcess.getRequestsManager();
            BreakpointRequest request = requestsManager.createBreakpointRequest(requestor, location);
            requestsManager.enableRequest(request);
            return request;
        }
        return null;
    }

    @Override
    protected void createRequestForPreparedClass(final DebugProcessImpl debugProcess, final ReferenceType classType) {
        if (!ReadAction.compute(() -> isInScopeOf(debugProcess, classType.name()))) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(classType.name() + " is out of debug-process scope, breakpoint request won't be created for line " + getLineIndex());
            }
            return;
        }
        try {
            List<Location> locations = debugProcess.getPositionManager().locationsOfLine(classType, getSourcePosition());
            if (!locations.isEmpty()) {
                locations = StreamEx.of(locations).peek(loc -> {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Found location [codeIndex=" + loc.codeIndex() +
                                  "] for reference type " + classType.name() +
                                  " at line " + getLineIndex() +
                                  "; isObsolete: " + (debugProcess.getVirtualMachineProxy().versionHigher("1.4") && loc.method().isObsolete()));
                    }
                }).filter(l -> acceptLocation(debugProcess, classType, l)).toList();
                // MODIFICATION: Start Kotlin implementation
                //locations = MethodBytecodeUtil.removeSameLineLocations(locations);
                // MODIFICATION: End Kotlin implementation
                for (Location loc : locations) {
                    createLocationBreakpointRequest(this, loc, debugProcess);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Created breakpoint request for reference type " + classType.name() + " at line " + getLineIndex() + "; codeIndex=" + loc.codeIndex());
                    }
                }
            }
            else if (DebuggerUtilsEx.allLineLocations(classType) == null) {
                // there's no line info in this class
                debugProcess.getRequestsManager()
                        .setInvalid(this, DebuggerBundle.message("error.invalid.breakpoint.no.line.info", classType.name()));
                if (LOG.isDebugEnabled()) {
                    LOG.debug("No line number info in " + classType.name());
                }
            }
            else {
                // there's no executable code in this class
                debugProcess.getRequestsManager().setInvalid(this, DebuggerBundle.message(
                        "error.invalid.breakpoint.no.executable.code", (getLineIndex() + 1), classType.name())
                );
                if (LOG.isDebugEnabled()) {
                    LOG.debug("No locations of type " + classType.name() + " found at line " + getLineIndex());
                }
            }
        }
        catch (ClassNotPreparedException ex) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("ClassNotPreparedException: " + ex.getMessage());
            }
            // there's a chance to add a breakpoint when the class is prepared
        }
        catch (ObjectCollectedException ex) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("ObjectCollectedException: " + ex.getMessage());
            }
            // there's a chance to add a breakpoint when the class is prepared
        }
        catch(Exception ex) {
            LOG.info(ex);
        }
        updateUI();
    }

    private boolean isInScopeOf(DebugProcessImpl debugProcess, String className) {
        final SourcePosition position = getSourcePosition();
        if (position != null) {
            final VirtualFile breakpointFile = position.getFile().getVirtualFile();
            final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(myProject).getFileIndex();
            if (breakpointFile != null && fileIndex.isUnderSourceRootOfType(breakpointFile, JavaModuleSourceRootTypes.SOURCES)) {
                if (debugProcess.getSearchScope().contains(breakpointFile)) {
                    return true;
                }
                // apply filtering to breakpoints from content sources only, not for sources attached to libraries
                final Collection<VirtualFile> candidates = findClassCandidatesInSourceContent(className, debugProcess.getSearchScope(), fileIndex);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Found "+ (candidates == null? "null" : candidates.size()) + " candidate containing files for class " + className);
                }
                if (candidates == null) {
                    // If no candidates are found in scope then assume that class is loaded dynamically and allow breakpoint
                    return true;
                }

                // breakpointFile is not in scope here and there are some candidates in scope
                //for (VirtualFile classFile : candidates) {
                //  if (LOG.isDebugEnabled()) {
                //    LOG.debug("Breakpoint file: " + breakpointFile.getPath()+ "; candidate file: " + classFile.getPath());
                //  }
                //  if (breakpointFile.equals(classFile)) {
                //    return true;
                //  }
                //}
                if (LOG.isDebugEnabled()) {
                    final GlobalSearchScope scope = debugProcess.getSearchScope();
                    final boolean contains = scope.contains(breakpointFile);
                    List<VirtualFile> files = ContainerUtil.map(
                            JavaPsiFacade.getInstance(myProject).findClasses(className, scope),
                            aClass -> aClass.getContainingFile().getVirtualFile());
                    List<VirtualFile> allFiles = ContainerUtil.map(
                            JavaPsiFacade.getInstance(myProject).findClasses(className, new EverythingGlobalScope(myProject)),
                            aClass -> aClass.getContainingFile().getVirtualFile());
                    final VirtualFile contentRoot = fileIndex.getContentRootForFile(breakpointFile);
                    final Module module = fileIndex.getModuleForFile(breakpointFile);

                    LOG.debug("Did not find '" +
                              className + "' in " + scope +
                              "; contains=" + contains +
                              "; contentRoot=" + contentRoot +
                              "; module = " + module +
                              "; all files in index are: " + files+
                              "; all possible files are: " + allFiles
                    );
                }

                return false;
            }
        }
        return true;
    }

    @Nullable
    private Collection<VirtualFile> findClassCandidatesInSourceContent(final String className, final GlobalSearchScope scope, final ProjectFileIndex fileIndex) {
        final int dollarIndex = className.indexOf("$");
        final String topLevelClassName = dollarIndex >= 0? className.substring(0, dollarIndex) : className;
        return ReadAction.compute(() -> {
            final PsiClass[] classes = JavaPsiFacade.getInstance(myProject).findClasses(topLevelClassName, scope);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Found "+ classes.length + " classes " + topLevelClassName + " in scope "+scope);
            }
            if (classes.length == 0) {
                return null;
            }
            final List<VirtualFile> list = new ArrayList<>(classes.length);
            for (PsiClass aClass : classes) {
                final PsiFile psiFile = aClass.getContainingFile();

                if (LOG.isDebugEnabled()) {
                    final StringBuilder msg = new StringBuilder();
                    msg.append("Checking class ").append(aClass.getQualifiedName());
                    msg.append("\n\t").append("PsiFile=").append(psiFile);
                    if (psiFile != null) {
                        final VirtualFile vFile = psiFile.getVirtualFile();
                        msg.append("\n\t").append("VirtualFile=").append(vFile);
                        if (vFile != null) {
                            msg.append("\n\t").append("isInSourceContent=").append(fileIndex.isUnderSourceRootOfType(vFile, JavaModuleSourceRootTypes.SOURCES));
                        }
                    }
                    LOG.debug(msg.toString());
                }

                if (psiFile == null) {
                    return null;
                }
                final VirtualFile vFile = psiFile.getVirtualFile();
                if (vFile == null || !fileIndex.isUnderSourceRootOfType(vFile, JavaModuleSourceRootTypes.SOURCES)) {
                    return null; // this will switch off the check if at least one class is from libraries
                }
                list.add(vFile);
            }
            return list;
        });
    }
}
