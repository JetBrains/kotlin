#!/usr/bin/python
#
# Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
# that can be found in the license/LICENSE.txt file.
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
            include_dirs = ['./build'],
            libraries = ['server'],
            library_dirs = ['./build'],
            depends = ['server_api.h'],
            sources = ['src/main/c/kotlin_bridge.c']
        )
      ]
)

# On macOS, after install you may want to copy libserver.dylib to Python's extension directory,
# and do something like
#  sudo install_name_tool /Library/Python/2.7/site-packages/kotlin_bridge.so -change libserver.dylib @loader_path/libserver.dylib
# This way libserver.dylib could be loaded from extension's directory.