DIRECTORY = "src/test/kotlin/tests"
OUTPUT = $(shell ls ${DIRECTORY}/*/*.txt)
OBJECTS = $(OUTPUT:.txt=.out)
green = '\033[0;32m'
red = '\033[0;31m'
lightRed = '\033[1;31m'
orange = '\033[0;33m'
nc = '\033[0m'


%.out: %.txt
	@./run_one_test.sh $(@:.out=.txt)

all: $(OBJECTS)
	@echo "ok"

.PHONY: all
