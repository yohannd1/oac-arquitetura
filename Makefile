SOURCE := src
BUILD := build
CFLAGS := -Xlint:unchecked

build: clean
	javac -d $(BUILD) $(CFLAGS) $(SOURCE)/**/*.java
	cp src/MANIFEST.MF build/MANIFEST.MF
	cd $(BUILD); jar -cmvf MANIFEST.MF main.jar **/*.class

clean:
	if [ -r $(BUILD) ]; then rm -r $(BUILD); fi

run: build
	java -jar $(BUILD)/main.jar
