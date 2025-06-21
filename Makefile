SRC_DIR := src
BUILD_DIR := bin
CFLAGS := -Xlint:unchecked

SOURCES := $(wildcard $(SRC_DIR)/**/*.java)
PROGRAM := $(BUILD_DIR)/main.jar

$(PROGRAM): $(SOURCES)
	javac -d $(BUILD_DIR) $(CFLAGS) $(SOURCES)
	cp src/MANIFEST.MF $(BUILD_DIR)/MANIFEST.MF
	cd $(BUILD_DIR); jar -cmvf MANIFEST.MF main.jar **/*.class

clean:
	if [ -r $(BUILD_DIR) ]; then rm -r $(BUILD_DIR); fi

run: $(PROGRAM)
	java -jar $(PROGRAM) program

.PHONY: clean run
