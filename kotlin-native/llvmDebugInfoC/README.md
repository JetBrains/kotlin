# Working on C++ code

1. Generate CMakeLists.txt
```
./gradlew :kotlin-native:llvmDebugInfoC:generateCMakeLists
```
2. Open this directory in CLion

# Working with konan_lldb.py

1. Generate a venv from Xcode's Python
```shell
sh -c 'exec "$(xcode-select -p)"/Library/Frameworks/Python3.framework/Versions/Current/bin/python3 -m venv .venv'
```

2. Symlink lldb Python API for PyCharm
```shell
echo "$(dirname $(xcode-select -p))"/SharedFrameworks/LLDB.framework/Versions/A/Resources/Python > "$(echo .venv/lib/python*)/site-packages/lldb.pth"
```