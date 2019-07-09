/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven.incremental;

import org.apache.maven.plugin.logging.Log;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class FileCopier {
    private final static int VERSION = 0;

    private final Log log;

    public FileCopier(Log log) {
        this.log = log;
    }

    public void syncDirs(@NotNull File from, @NotNull File to, @NotNull File snapshotsStorageFile) {
        try {
            syncDirsImpl(from, to, snapshotsStorageFile);
        } catch (IOException e) {
            log.warn("Could not copy Kotlin files from " + from + " to " + to, e);
        }
    }

    private void syncDirsImpl(
            @NotNull File sourceBaseFile,
            @NotNull File targetBaseFile,
            @NotNull File snapshotsStorageFile
    ) throws IOException {
        // snapshots are stored as relative paths (to source or target base)
        Map<String, FileSnapshot> previousSnapshots = readFileSnapshots(snapshotsStorageFile);

        Path sourceBase = Paths.get(sourceBaseFile.getPath()).normalize();
        Path targetBase = Paths.get(targetBaseFile.getPath()).normalize();
        Map<String, FileSnapshot> newSnapshots = new HashMap<>();
        for (Path path : Files.walk(sourceBase).collect(Collectors.toList())) {
            if (!Files.isRegularFile(path)) continue;

            String relativePath = sourceBase.relativize(path).toString();

            File file = path.toFile();
            FileSnapshot snapshot = new FileSnapshot(file.lastModified(), file.length());

            // remove current files from previousSnapshots map so only removed files would remain in previousSnapshots
            FileSnapshot prevSnapshot = previousSnapshots.remove(relativePath);
            if (!snapshot.equals(prevSnapshot)) {
                Path target = targetBase.resolve(relativePath);
                if (!Files.isDirectory(target)) {
                    Files.deleteIfExists(target);
                    Files.createDirectories(target.getParent());
                    Files.copy(path, target);
                }

                log.debug("Copied " + path + " to " + target);
            }
            newSnapshots.put(relativePath, snapshot);
        }

        for (String removedPath : previousSnapshots.keySet()) {
            Path target = targetBase.resolve(removedPath);
            log.debug("Deleted " + target + " as it is no longer present in " + sourceBase);
            if (Files.isRegularFile(target)) {
                Files.delete(target);
            }
        }

        writeFileSnapshots(newSnapshots, snapshotsStorageFile);
    }

    private void writeFileSnapshots(@NotNull Map<String, FileSnapshot> snapshots, @NotNull File outputFile) {
        try (ObjectOutputStream output = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)))) {
            output.writeInt(VERSION);
            output.writeInt(snapshots.size());
            for (Map.Entry<String, FileSnapshot> entry : snapshots.entrySet()) {
                String path = entry.getKey();
                output.writeUTF(path);
                FileSnapshot snapshot = entry.getValue();
                output.writeLong(snapshot.lastModified);
                output.writeLong(snapshot.size);
            }
        } catch (Exception e) {
            log.debug("Couldn't write copied files list to " + outputFile, e);
        }
    }

    @NotNull
    private Map<String, FileSnapshot> readFileSnapshots(@NotNull File inputFile) {
        Map<String, FileSnapshot> snapshots = new HashMap<>();
        if (!inputFile.isFile()) return snapshots;

        try (ObjectInputStream input  = new ObjectInputStream(new BufferedInputStream(new FileInputStream(inputFile)))) {
            int version = input.readInt();
            if (version != VERSION) return snapshots;

            int size = input.readInt();
            for (int i = 0; i < size; i++) {
                String path = input.readUTF();
                long lastModified = input.readLong();
                long fileSize = input.readLong();
                snapshots.put(path, new FileSnapshot(lastModified, fileSize));
            }
        } catch (Exception e) {
            log.debug("Couldn't read copied files list from " + inputFile, e);
        }

        return snapshots;
    }

    private static class FileSnapshot {
        final long lastModified;
        final long size;

        private FileSnapshot(long lastModified, long size) {
            this.lastModified = lastModified;
            this.size = size;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FileSnapshot that = (FileSnapshot) o;

            if (lastModified != that.lastModified) return false;
            return size == that.size;
        }

        @Override
        public int hashCode() {
            int result = (int) (lastModified ^ (lastModified >>> 32));
            result = 31 * result + (int) (size ^ (size >>> 32));
            return result;
        }
    }
}
