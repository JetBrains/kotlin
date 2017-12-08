#!/usr/bin/python
#
# Copyright 2010-2017 JetBrains s.r.o.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
from distutils.core import setup, Extension

setup(name='kotlin_bridge',
      version='1.0',
      maintainer = 'JetBrains',
      maintainer_email = 'info@jetbrains.com',
      author = 'JetBrains',
      author_email = 'info@jetbrains.com',
      description = 'Kotlin/Native Python bridge',
      long_description = 'Using Kotlin/Native from Python example',

      # data_files=[("/Library/Python/2.7/site-packages/", ['libserver.dylib'])],

      ext_modules=[
         Extension('kotlin_bridge',
            include_dirs = ['.'],
            libraries = ['server'],
            library_dirs = ['.'],
            depends = ['server_api.h'],
            sources = ['src/main/c/kotlin_bridge.c']
        )
      ]
)

# On macOS, after install you may want to copy libserver.dylib to Python's extension directory,
# and do something like
#  sudo install_name_tool /Library/Python/2.7/site-packages/kotlin_bridge.so -change libserver.dylib @loader_path/libserver.dylib
# This way libserver.dylib could be loaded from extension's directory.