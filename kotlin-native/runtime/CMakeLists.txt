# Disclaimer: this is a straw-man attempt to add navigation to Clion or similar tools.
# At the moment it can't be used for real building or debugging. Feel free to contribute

cmake_minimum_required(VERSION 3.14)
project(runtime)

set(CMAKE_CXX_STANDARD 14)

set(SRC_DIR ${CMAKE_SOURCE_DIR}/src)

set(GOOGLETEST_DIR ${CMAKE_SOURCE_DIR}/googletest)

include_directories(${SRC_DIR}/main/cpp)
include_directories(${SRC_DIR}/debug/headers)
include_directories(${CMAKE_SOURCE_DIR}/../common/src/hash/headers)

include_directories(${GOOGLETEST_DIR}/googletest/include)
include_directories(${GOOGLETEST_DIR}/googlemock/include)

add_executable(runtime
	src/main/cpp/Arrays.cpp
	src/main/cpp/Atomic.cpp
	src/main/cpp/Boxing.cpp
	src/main/cpp/Console.cpp
	src/main/cpp/CyclicCollector.cpp
	src/main/cpp/Exceptions.cpp
	src/main/cpp/ExecFormat.cpp
	src/main/cpp/Interop.cpp
	src/main/cpp/JSInterop.cpp
	src/main/cpp/KAssert.cpp
	src/main/cpp/KString.cpp
	src/main/cpp/KotlinMath.cpp
	src/main/cpp/Memory.cpp
	src/main/cpp/MemorySharedRefs.cpp
	src/main/cpp/Natives.cpp
	src/main/cpp/ObjCExceptions.cpp
	src/main/cpp/Operator.cpp
	src/main/cpp/Porting.cpp
	src/main/cpp/Regex.cpp
	src/main/cpp/ReturnSlot.cpp
	src/main/cpp/Runtime.cpp
	src/main/cpp/StdCppStubs.cpp
	src/main/cpp/Time.cpp
	src/main/cpp/ToString.cpp
	src/main/cpp/TypeInfo.cpp
	src/main/cpp/Types.cpp
	src/main/cpp/Weak.cpp
	src/main/cpp/Worker.cpp
	src/main/cpp/dlmalloc/malloc.cpp
	src/main/cpp/dtoa/cbigint.cpp
	src/main/cpp/dtoa/dblparse.cpp
	src/main/cpp/dtoa/fltparse.cpp
	src/main/cpp/math/fmod.cpp
	src/main/cpp/math/fmodf.cpp
	src/main/cpp/math/scalbn.cpp
	src/main/cpp/snprintf/snprintf.cpp
	src/profile_runtime/cpp/ProfileRuntime.cpp
	src/relaxed/cpp/MemoryImpl.cpp
	src/release/cpp/SourceInfo.cpp
	src/strict/cpp/MemoryImpl.cpp

	src/opt_alloc/cpp/AllocImpl.cpp
	src/std_alloc/cpp/AllocImpl.cpp

	src/debug/cpp/KDebug.cpp
	src/debug/cpp/SourceInfo.cpp
	src/exceptions_support/cpp/ExceptionsSupport.cpp
	src/launcher/cpp/androidLauncher.cpp
	src/launcher/cpp/launcher.cpp

	src/main/cpp/ObjCExport.mm
	src/main/cpp/ObjCInterop.mm
	src/main/cpp/ObjCInteropUtils.mm
	src/main/cpp/ObjCExportCollectionUtils.mm
	src/main/cpp/ObjCExportErrors.mm
	src/main/cpp/ObjCExportExceptionDetails.mm

	src/objc/cpp/ObjCExportNumbers.mm
	src/objc/cpp/ObjCExportClasses.mm
	src/objc/cpp/ObjCExportCollections.mm
	src/objc/cpp/ObjCInteropUtilsClasses.mm

	# Tests
	src/test_support/cpp/CompilerGenerated.cpp
	src/test_support/cpp/CompilerGeneratedObjC.mm
	src/test_support/cpp/TestLauncher.cpp

	src/main/cpp/ArraysTest.cpp
)

target_compile_definitions(runtime PUBLIC "-DKONAN_OSX=1")
target_compile_definitions(runtime PUBLIC "-DKONAN_MACOSX=1")
target_compile_definitions(runtime PUBLIC "-DKONAN_X64=1")
target_compile_definitions(runtime PUBLIC "-DKONAN_OBJC_INTEROP")
target_compile_definitions(runtime PUBLIC "-DKONAN_CORE_SYMBOLICATION=1")
target_compile_definitions(runtime PUBLIC "-DKONAN_HAS_CXX11_EXCEPTION_FUNCTIONS=1")
target_compile_definitions(runtime PUBLIC "-DKONAN_REPORT_BACKTRACE_TO_IOS_CRASH_LOG=1")


