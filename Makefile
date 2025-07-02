SRC_DIR := src
BUILD_DIR := bin
CFLAGS := -Xlint:unchecked

SOURCES := $(wildcard $(SRC_DIR)/**/*.java)
PROGRAM := $(BUILD_DIR)/built.txt

$(PROGRAM): $(SOURCES)
	mkdir -p $(BUILD_DIR)
	javac -d $(BUILD_DIR) $(CFLAGS) $(SOURCES)
	touch $(PROGRAM)

clean:
	if [ -r $(BUILD_DIR) ]; then rm -r $(BUILD_DIR); fi

run: $(PROGRAM)
	java -cp $(BUILD_DIR) assembler.Assembler program
	java -cp $(BUILD_DIR) architecture.Architecture program

.PHONY: clean run
