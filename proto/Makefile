CXX = g++
CXXFLAGS = -O2 -std=c++11
LDFLAGS = -lprotoc -lprotobuf

BUILDDIR = build
EXE = $(BUILDDIR)/protoc
JAR = $(BUILDDIR)/protokot-runtime.jar

# C++ part
PROTOC_DIR = compiler/google/src/google/protobuf/compiler/kotlin
PROTOC_EXE = $(PROTOC_DIR)/protoc

# Kotlin part
RUNTIME_DIR = compiler/runtime
RUNTIME_JAR = $(RUNTIME_DIR)/build/libs/protokot-runtime.jar

all: $(EXE) $(JAR)

# Place protoc* into build directory
$(EXE): $(BUILDDIR) $(PROTOC_EXE)
	cp $(PROTOC_EXE) $(EXE)

# Build Protobuf Compiler via delegating call to another Makefile
$(PROTOC_EXE):
	$(MAKE) -C $(PROTOC_DIR)

# Place ProtoKot Runtime JAR into build directory
$(JAR): $(BUILDDIR) gradle_invoke
	cp $(RUNTIME_JAR) $(JAR)

# Build ProtoKot Runtime via delegating call to Gradle Script
gradle_invoke:
	cd $(RUNTIME_DIR); gradle build

# Create auxillary directories
$(BINDIR):
	mkdir -p $(BINDIR)

$(BUILDDIR):
	mkdir -p $(BUILDDIR)

clean:
	$(MAKE) -C $(PROTOC_DIR) clean
	cd $(RUNTIME_DIR); gradle clean
	rm -rf $(BUILDDIR)

.PHONY: clean all generate test gradle_invoke
