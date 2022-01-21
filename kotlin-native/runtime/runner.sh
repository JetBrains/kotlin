for i in $(seq 1 1000); do

echo $i

./build/bin/test/mingw_x64/mingw_x64ExperimentalMMCmsMimallocRuntimeTests.exe --gtest_filter=*ConcurrentMarkAndSweepTest*

done