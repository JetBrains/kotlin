/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

import java.io.*;

File file = new File(basedir, "target/test-kotlin-bom-1.0-SNAPSHOT.jar")
if (!file.exists() || !file.isFile()) {
    throw new FileNotFoundException("Could not find generated JAR: " + file)
}

String expectedDependency = "org.jetbrains.kotlin:kotlin-reflect:jar:$kotlinVersion:compile"
List<String> lines = new File(basedir, "build.log").readLines()

if (!lines.any { it.contains(expectedDependency) }) {
    throw new Exception("Expected to find dependency '$expectedDependency' in dependency tree")
}