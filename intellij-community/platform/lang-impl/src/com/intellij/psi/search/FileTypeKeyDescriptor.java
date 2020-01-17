// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.search;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.indexing.SubstitutedFileType;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

class FileTypeKeyDescriptor implements KeyDescriptor<FileType> {
    static final FileTypeKeyDescriptor INSTANCE = new FileTypeKeyDescriptor();

    @Override
    public int getHashCode(FileType value) {
        return value.getName().hashCode();
    }

    @Override
    public boolean isEqual(FileType val1, FileType val2) {
        if (val1 instanceof SubstitutedFileType) val1 = ((SubstitutedFileType)val1).getFileType();
        if (val2 instanceof SubstitutedFileType) val2 = ((SubstitutedFileType)val2).getFileType();
        if (val1 instanceof OutDatedFileType || val2 instanceof OutDatedFileType) {
            return Comparing.equal(val1.getName(), val2.getName());
        }
        return Comparing.equal(val1, val2);
    }

    @Override
    public void save(@NotNull DataOutput out, FileType value) throws IOException {
        EnumeratorStringDescriptor.INSTANCE.save(out, value.getName());
    }

    @Override
    public FileType read(@NotNull DataInput in) throws IOException {
        String read = EnumeratorStringDescriptor.INSTANCE.read(in);
        FileType fileType = FileTypeRegistry.getInstance().findFileTypeByName(read);
        return fileType == null ? new OutDatedFileType(read) : fileType;
    }

    private static class OutDatedFileType implements FileType {
        @NotNull
        private final String myName;

        private OutDatedFileType(@NotNull String name) {myName = name;}

        @NotNull
        @Override
        public String getName() {
            return myName;
        }

        @NotNull
        @Override
        public String getDescription() {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public String getDefaultExtension() {
            throw new UnsupportedOperationException();
        }

        @Nullable
        @Override
        public Icon getIcon() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isBinary() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isReadOnly() {
            throw new UnsupportedOperationException();
        }

        @Nullable
        @Override
        public String getCharset(@NotNull VirtualFile file, byte @NotNull [] content) {
            throw new UnsupportedOperationException();
        }
    }
}
