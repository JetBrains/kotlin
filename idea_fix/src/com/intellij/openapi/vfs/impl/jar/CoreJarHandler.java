/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.openapi.vfs.impl.jar;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yole
 */
public class CoreJarHandler extends JarHandlerBase {
    private final Map<String, VirtualFile> myFileMap = new HashMap<String, VirtualFile>();
    private final CoreJarFileSystem myFileSystem;

    public CoreJarHandler(CoreJarFileSystem fileSystem, String path) {
        super(fileSystem, path);
        myFileSystem = fileSystem;
    }

    @Nullable
    public VirtualFile findFileByPath(String pathInJar) {
        if (getZip() == null) {
            return null;
        }
        VirtualFile file = myFileMap.get(pathInJar);
        if (file == null) {
            if (pathInJar.length() > 0) {
                EntryInfo entryInfo = getEntryInfo(pathInJar);
                if (entryInfo == null) {
                    return null;
                }
            }
            file = new CoreJarVirtualFile(myFileSystem, this, pathInJar);
            myFileMap.put(pathInJar, file);
        }
        return file;
    }
}
