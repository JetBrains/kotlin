
BUILD_DIR=$(PWD)/build
INCLUDE_DIR=$(PWD)/include
LIB_ARM_DIR=$(PWD)/lib/arm
LIB_X86_DIR=$(PWD)/lib/x86
LIBC=$(PWD)/libc

KT=$(PWD)/../translator/build/libs/translator-1.0.jar
LLINK=llvm-link-3.6
CC=clang-3.6

CCFLAGS_ARM=-g -S -Wall -m32 -emit-llvm -nostdlib -ffreestanding -march=armv7-m -mthumb -flto -O0 -target arm-none-eabi -DARM
CCFLAGS=-g -O0 -S -Wall -emit-llvm -nostdlib -ffreestanding
CCFLAGS_DEBUG=-g -O0 -S -Wall -emit-llvm -nostdlib -ffreestanding -DDEBUG
LLINK_FLAGS=-S

KT_ALL_DEPS=java -jar $(KT)
LLINK_ALL_DEPS=$(LLINK) $(LLINK_FLAGS) $(filter %.ll,$^) > $@

all: memory $(BUILD_DIR) $(BUILD_DIR)/stdlib_arm.ll $(BUILD_DIR)/stdlib_x86.ll
debug: memory_debug

$(BUILD_DIR):
	mkdir -p $(BUILD_DIR)

$(BUILD_DIR)/stdlib_x86.ll: $(LIB_X86_DIR)/*.ll
	$(LLINK_ALL_DEPS)

$(BUILD_DIR)/stdlib_arm.ll: $(LIB_ARM_DIR)/*.ll
	$(LLINK_ALL_DEPS)

$(BUILD_DIR)/classes_x86.ll: $(INCLUDE_DIR)/*.kt
	$(KT_ALL_DEPS) -o $@ $(filter %.kt,$^)

$(BUILD_DIR)/classes_arm.ll: $(INCLUDE_DIR)/*.kt
	$(KT_ALL_DEPS) --arm -o $@ $(filter %.kt,$^)

memory:
	$(CC) $(CCFLAGS) $(LIBC)/memory.c -o $(LIB_X86_DIR)/memory.ll
	$(CC) $(CCFLAGS_ARM) $(LIBC)/memory.c -o $(LIB_ARM_DIR)/memory.ll

memory_debug:
	$(CC) $(CCFLAGS_DEBUG) $(LIBC)/memory.c -o $(LIB_X86_DIR)/memory.ll

clean:
	rm -rf $(BUILD_DIR)

.PHONY: all clean

