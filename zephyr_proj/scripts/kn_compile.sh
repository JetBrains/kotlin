OUT_FOLDER=~/kn_exp/zephyr_proj/out/kn
CLANG_BIN=~/.konan/dependencies/llvm-11.1.0-linux-x64-essentials/bin/
rm -rf ${OUT_FOLDER}
mkdir ${OUT_FOLDER}

~/kn_exp/kotlin-native/dist/bin/kotlinc-native \
    ~/kn_exp/zephyr_proj/kotlin/src/nativeMain/kotlin/lib.kt \
    -target zephyr_m55 -produce static \
    -Xtemporary-files-dir=${OUT_FOLDER} \
    -Xsave-llvm-ir-after=MandatoryBitcodeLLVMPostprocessingPhase \
    -Xsave-llvm-ir-directory=${OUT_FOLDER} \
    -module-name libkn
llvm-dis ${OUT_FOLDER}/api.bc
llvm-dis ${OUT_FOLDER}/out.bc

rm ${OUT_FOLDER}/libstatic.a.o

${CLANG_BIN}/clang++ \
    -target thumb -mtp=soft -mfloat-abi=soft -mcpu=cortex-m3 -c -D__GLIBC_USE \
    ${OUT_FOLDER}/out.MandatoryBitcodeLLVMPostprocessingPhase.ll \
    -o  ${OUT_FOLDER}/libstatic.a.o

mv ${OUT_FOLDER}/libstatic.a.o ${OUT_FOLDER}/libkn.o