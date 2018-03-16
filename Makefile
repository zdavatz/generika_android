identifier="org.oddb.generika"

# -- build targes

build:
	./bin/gradlew build -x test $(ARGS)
.PHONY: build

archive:
	# generate apk with buildType:debug
	./bin/gradlew assemble $(ARGS)
.PHONY: archive

release:
	# generate apk with buildType:release
	./bin/gradlew assembleRelease $(ARGS)
.PHONY: release


# -- development targets

serve:
	adb kill-server
	adb start-server
.PHONY: serve

list:
	adb devices -l
.PHONY: list

attach:
	adb connect $(ARGS)
.PHONY: attach

detach:
	adb disconnect $(ARGS)
.PHONY: detach

run:
	./bin/emulator $(ARGS)
.PHONY: run

install:
	adb $(ARGS) install -r -t app/build/outputs/apk/debug/app-debug.apk
.PHONY: install

log:
	adb logcat | grep `adb shell ps | grep ${identifier} | cut -c10-15`
.PHONY: log

stop:
	adb devices | grep '^emulator' | cut -f1 | \
		while read line; do adb -s "$${line}" emu kill; \
		done
.PHONY: stop


# -- testing targets

test:
	./bin/gradlew test -PisTest=true $(ARGS)
.PHONY: test


# -- other targets

clean:
	./bin/gradlew clean
.PHONY: clean

.DEFAULT_GOAL = test
default: test
