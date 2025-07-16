PROGRAM := examples/ex01

build:
	./build.bash build

clean:
	./build.bash clean

run:
	./build.bash run $(PROGRAM)

test:
	./build.bash test

.PHONY: clean run build test
