# /Users/txie/.konan/dependencies/apple-llvm-20200714-macos-aarch64-essentials/bin/clang++ \
#     -B/Users/txie/zephyr-sdk-0.16.5-1/arm-zephyr-eabi/arm-zephyr-eabi/bin -fno-stack-protector \
#     -target thumb -mfloat-abi=soft -mcpu=cortex-m55 -fPIC \
#     -I/Users/txie/zephyr-sdk-0.16.5-1/arm-zephyr-eabi/arm-zephyr-eabi/include/c++/12.2.0 \
#     -I/Users/txie/zephyr-sdk-0.16.5-1/arm-zephyr-eabi/arm-zephyr-eabi/include/c++/12.2.0/arm-zephyr-eabi/thumb/v8-m.main+fp/hard \
#     -I/Users/txie/zephyr-sdk-0.16.5-1/arm-zephyr-eabi/arm-zephyr-eabi/include \
#     -fshort-enums \
#     -std=c++17 \
#     -emit-llvm -c \
#     lib/lib.cpp \
#     -o out/lib.bc

# "/Users/txie/.konan/dependencies/apple-llvm-20200714-macos-aarch64-essentials/bin/clang-11" \
#     -cc1 -triple thumbv8.1m.main-- -emit-obj -mrelax-all -disable-free -disable-llvm-verifier \
#     -discard-value-names -main-file-name out/lib.bc -mrelocation-model static -mframe-pointer=all \
#     -fmath-errno -fno-rounding-math -mconstructor-aliases -target-cpu cortex-m55 -target-feature \
#     -crc -target-feature -crypto -target-feature -sha2 -target-feature -aes -target-feature \
#     -dotprod -target-feature +dsp -target-feature +mve -target-feature +mve.fp -target-feature +fullfp16 \
#     -target-feature +ras -target-feature -fp16fml -target-feature -bf16 -target-feature -sb \
#     -target-feature -i8mm -target-feature +lob -target-feature -cdecp0 -target-feature -cdecp1 \
#     -target-feature -cdecp2 -target-feature -cdecp3 -target-feature -cdecp4 -target-feature -cdecp5 \
#     -target-feature -cdecp6 -target-feature -cdecp7 -target-feature -hwdiv-arm -target-feature +hwdiv \
#     -target-feature +strict-align -target-abi aapcs -mfloat-abi soft -fallow-half-arguments-and-returns \
#     -fno-split-dwarf-inlining -debugger-tuning=gdb -target-linker-version 650.9 -v \
#     -resource-dir /Users/txie/.konan/dependencies/apple-llvm-20200714-macos-aarch64-essentials/lib/clang/11.1.0 \
#     -Wno-elaborated-enum-base -fdebug-compilation-dir /Users/txie/kn_exp/zephyr_proj -ferror-limit 19 -fno-signed-char \
#     -fgnuc-version=4.2.1 -fcolor-diagnostics -faddrsig -fshort-enums \
#     -o /Users/txie/kn_exp/zephyr_proj/out/lib.o \
#     -x ir /Users/txie/kn_exp/zephyr_proj/out/lib.bc

cd ~/zephyrproject/zephyr
west build -p always -b mps3_an547 /Users/txie/kn_exp/zephyr_proj

#qemu-system-arm -machine mps3-an547 -nographic -kernel /Users/txie/zephyrproject/zephyr/build/zephyr/zephyr.elf
