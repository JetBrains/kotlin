
BUILD_DIR=$(PWD)/build
INCLUDE_DIR=$(PWD)/include
LIB_DIR=$(PWD)/ll
LIB_ARM_DIR=$(LIB_DIR)/arm
LIB_X86_DIR=$(LIB_DIR)/x86
LIBC=$(PWD)/c
LLVM_ARM_FILES = $(patsubst $(LIBC)/%.c,$(LIB_ARM_DIR)/%.ll,$(wildcard $(LIBC)/*.c))
LLVM_X86_FILES = $(patsubst $(LIBC)/%.c,$(LIB_X86_DIR)/%.ll,$(wildcard $(LIBC)/*.c))

KT=$(PWD)/../translator/build/libs/translator-1.0.jar
LLINK=llvm-link-3.6
CC=clang-3.6

CCFLAGS_ARM=-g -S -Wall -m32 -emit-llvm -nostdlib -ffreestanding -march=armv7-m -mthumb -flto -O0 -target arm-none-eabi -DARM -Wno-int-to-pointer-cast -Wno-gcc-compat
CCFLAGS=-g -O0 -S -Wall -emit-llvm -nostdlib -ffreestanding -Wno-int-to-pointer-cast -Wno-gcc-compat
CCFLAGS_DEBUG=-g -O0 -S -Wall -emit-llvm -nostdlib -ffreestanding -DDBG
LLINK_FLAGS=-S

KT_ALL_DEPS=java -jar $(KT)
LLINK_ALL_DEPS=$(LLINK) $(LLINK_FLAGS) $(filter %.ll,$^) > $@

all: $(BUILD_DIR) $(BUILD_DIR)/stdlib_arm.ll $(BUILD_DIR)/stdlib_x86.ll
debug: memory_debug  $(BUILD_DIR) $(BUILD_DIR)/stdlib_arm.ll $(BUILD_DIR)/stdlib_x86.ll

$(BUILD_DIR):
	mkdir -p $(BUILD_DIR)
	mkdir -p $(LIB_ARM_DIR)
	mkdir -p $(LIB_X86_DIR)

$(BUILD_DIR)/stdlib_arm.ll: $(LLVM_ARM_FILES)
	$(LLINK_ALL_DEPS)

$(BUILD_DIR)/stdlib_x86.ll: $(LLVM_X86_FILES)
	$(LLINK_ALL_DEPS)

$(BUILD_DIR)/classes_x86.ll: $(INCLUDE_DIR)/*.kt
	$(KT_ALL_DEPS) -o $@ $(filter %.kt,$^)

$(BUILD_DIR)/classes_arm.ll: $(INCLUDE_DIR)/*.kt
	$(KT_ALL_DEPS) --arm -o $@ $(filter %.kt,$^)

$(LIB_X86_DIR)/%.ll: $(LIBC)/%.c
	$(CC) $(CCFLAGS) -c $< -o $(LIB_X86_DIR)/$(basename $(notdir $<)).ll

$(LIB_ARM_DIR)/%.ll: $(LIBC)/%.c
	$(CC) $(CCFLAGS_ARM) -c $< -o $(LIB_ARM_DIR)/$(basename $(notdir $<)).ll

memory_debug:
	$(CC) $(CCFLAGS_DEBUG) $(LIBC)/memory.c -o $(LIB_X86_DIR)/memory.ll

clean:
	rm -rf $(BUILD_DIR)
	rm -rf $(LIB_DIR)

.PHONY: all clean

