/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

#ifndef RUNTIME_SOURCEINFO_H
#define RUNTIME_SOURCEINFO_H

#include <string>

class SourceInfo {
    std::string fileName;
public:
    int lineNumber = -1;
    int column = -1;
    bool nodebug = false;
    std::string& getFileName() { return fileName; }
    void setFilename(const char *newFileName) { fileName = newFileName ?: ""; }
};

#endif // RUNTIME_SOURCEINFO_H
