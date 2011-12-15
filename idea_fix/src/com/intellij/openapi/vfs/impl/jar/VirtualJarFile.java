package com.intellij.openapi.vfs.impl.jar;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: Natalia.Ukhorskaya
 * Date: 12/1/11
 * Time: 5:06 PM
 */

public class VirtualJarFile extends VirtualFile {
    private byte[] byteArray;
    private final CoreJarFileSystem myFileSystem;
    private final String name;

    public VirtualJarFile(CoreJarFileSystem myFileSystem, String pathInJar) {
        this.myFileSystem = myFileSystem;
        this.name = pathInJar;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @NotNull
    @Override
    public VirtualFileSystem getFileSystem() {
        return myFileSystem;
    }

    @Override
    public String getPath() {
        return name;
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public VirtualFile getParent() {
        return null;
    }

    @Override
    public VirtualFile[] getChildren() {
        return VirtualFile.EMPTY_ARRAY;
    }

    @NotNull
    @Override
    public OutputStream getOutputStream(Object requestor, long newModificationStamp, long newTimeStamp) throws IOException {
        return System.out;
    }

    @NotNull
    @Override
    public byte[] contentsToByteArray() throws IOException {
        if (byteArray.length <= 0) {
            int length;
            byte[] tmp = new byte[1024];
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream rtJar = VirtualJarFile.class.getResourceAsStream("/" + name);
            try {
                while ((length = rtJar.read(tmp)) >= 0) {
                    out.write(tmp, 0, length);
                }
            } catch (IOException e) {
                e.printStackTrace();
                return new byte[0];
            }
            byteArray = out.toByteArray();
        }
        return byteArray;
    }

    @Override
    public long getTimeStamp() {
        return 0;
    }

    @Override
    public long getLength() {
        return byteArray.length;
    }

    @Override
    public void refresh(boolean asynchronous, boolean recursive, Runnable postRunnable) {

    }

    @Override
    public InputStream getInputStream() throws IOException {
        return VirtualJarFile.class.getResourceAsStream("/" + name);
    }
}
