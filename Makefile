build:
	./bin/gradlew build
.PHONY: build

run-emulator:
	./bin/emulator $(ARGS)
.PHONY: run-emulator

run: | run-emulator
.PHONY: run

test:
	./bin/gradlew test
.PHONY: test

clean:
	./bin/gradlew clean
.PHONY: clean

.DEFAULT_GOAL = test
default: test
