/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.intellij.mock;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.DeprecatedVirtualFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.containers.CollectionFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Map;

public final class MockVirtualFileSystem extends DeprecatedVirtualFileSystem {
    private static final String PROTOCOL = "mock";

    private final MyVirtualFile myRoot = new MyVirtualFile("", null) {
        @NotNull
        @Override
        public VirtualFileSystem getFileSystem() {
            return MockVirtualFileSystem.this;
        }
    };

    @NotNull
    @Override
    public MyVirtualFile findFileByPath(@NotNull String path) {
        String normalized = path.replace(File.separatorChar, '/').replace('/', ':');
        if (StringUtil.startsWithChar(normalized, ':')) normalized = normalized.substring(1);
        MyVirtualFile file = myRoot;
        for (String component : StringUtil.split(normalized, ":")) {
            file = file.getOrCreate(component);
        }
        return file;
    }

    @NotNull
    public MockVirtualFileSystem file(@NotNull String path, @NotNull String data) {
        MyVirtualFile file = findFileByPath(path);
        file.setContent(null, data, false);
        return this;
    }

    @NotNull
    public VirtualFile getRoot() {
        return myRoot;
    }

    @NotNull
    @Override
    public String getProtocol() {
        return PROTOCOL;
    }

    @Override
    public void refresh(boolean asynchronous) { }

    @Override
    public VirtualFile refreshAndFindFileByPath(@NotNull String path) {
        return findFileByPath(path);
    }

    private static class MyVirtualFile extends LightVirtualFile {
        private final MyVirtualFile myParent;
        private Map<String, MyVirtualFile> myChildren;

        private MyVirtualFile(String name, MyVirtualFile parent) {
            super(name);
            myParent = parent;
        }

        @NotNull
        @Override
        public VirtualFileSystem getFileSystem() {
            return myParent.getFileSystem();
        }

        @NotNull
        public MyVirtualFile getOrCreate(@NotNull String name) {
            MyVirtualFile file = findChild(name);
            if (file == null) {
                if (myChildren == null) {
                    myChildren = CollectionFactory.createSmallMemoryFootprintMap();
                }
                file = new MyVirtualFile(name, this);
                myChildren.put(name, file);
            }
            return file;
        }

        @Override
        public boolean isDirectory() {
            return myParent == null || (myChildren != null && !myChildren.isEmpty());
        }

        @NotNull
        @Override
        public String getPath() {
            MockVirtualFileSystem.MyVirtualFile parent = getParent();
            return parent == null ? getName() : parent.getPath() + "/" + getName();
        }

        @Override
        public MyVirtualFile getParent() {
            return myParent;
        }

        @Override
        public VirtualFile[] getChildren() {
            return myChildren == null ? EMPTY_ARRAY : VfsUtilCore.toVirtualFileArray(myChildren.values());
        }

        @Nullable
        @Override
        public MyVirtualFile findChild(@NotNull String name) {
            return myChildren == null ? null : myChildren.get(name);
        }
    }
}
