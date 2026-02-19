# Working on C++ code

1. Generate CMakeLists.txt
```
./gradlew :kotlin-native:llvmDebugInfoC:generateCMakeLists
```
2. Open this directory in CLion

# Working with konan_lldb.py on macOS

1. Generate a venv from Xcode's Python
```shell
cd kotlin-native/llvmDebugInfoC/src/scripts
sh -c 'exec "$(xcode-select -p)"/Library/Frameworks/Python3.framework/Versions/Current/bin/python3 -m venv .venv'
```

2. Symlink lldb Python API for lldb completions in PyCharm
```shell
echo "$(dirname "$(xcode-select -p)")"/SharedFrameworks/LLDB.framework/Versions/A/Resources/Python > "$(echo .venv/lib/python*)/site-packages/lldb.pth"
open -b com.jetbrains.pycharm .
```