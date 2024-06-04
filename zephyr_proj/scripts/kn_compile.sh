OUT_FOLDER=~/kn_exp/zephyr_proj/out/kn
rm -rf ${OUT_FOLDER}
mkdir ${OUT_FOLDER}

~/kn_exp/kotlin-native/dist/bin/kotlinc-native ~/kn_exp/zephyr_proj/kotlin/src/nativeMain/kotlin/lib.kt -target zephyr_m55 -produce static -Xtemporary-files-dir=${OUT_FOLDER} -Xsave-llvm-ir-after=Codegen -Xsave-llvm-ir-directory=${OUT_FOLDER}
llvm-dis ${OUT_FOLDER}/api.bc
llvm-dis ${OUT_FOLDER}/out.bc