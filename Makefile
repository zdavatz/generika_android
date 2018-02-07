build:
	./bin/gradlew build $(ARGS)
.PHONY: build

run-emulator:
	./bin/emulator $(ARGS)
.PHONY: run-emulator

run: | run-emulator
.PHONY: run

stop:
	adb devices | grep '^emulator' | cut -f1 | \
		while read line; do adb -s "$${line}" emu kill; \
		done
.PHONY: stop

test:
	./bin/gradlew test
.PHONY: test

clean:
	./bin/gradlew clean
.PHONY: clean

.DEFAULT_GOAL = test
default: test
