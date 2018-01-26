build:
	./bin/gradlew build
.PHONY: build

test:
	./bin/gradlew test
.PHONY: test

clean:
	./bin/gradlew clean
.PHONY: clean


.DEFAULT_GOAL = test
default: test
