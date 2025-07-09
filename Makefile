build:
	./build.bash build

clean:
	./build.bash clean

run:
	./build.bash run

test:
	./build.bash test

.PHONY: clean run build test
