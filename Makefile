build:
	./bin/gradlew build $(ARGS)
.PHONY: build

archive:
	# generate apk with buildType:debug
	./bin/gradlew assemble $(ARGS)
.PHONY: archive

release:
	# generate apk with buildType:release
	./bin/gradlew assembleRelease $(ARGS)
.PHONY: release

run-emulator:
	./bin/emulator $(ARGS)
.PHONY: run-emulator

run:
	adb install -r -t app/build/outputs/apk/debug/app-debug.apk
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
