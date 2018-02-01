# Generika Android

Generika App for Android, based on the Generikacc App for iOS.


## Repository

https://github.com/zdavatz/generika_android



## Setup

### Requirements

* [IcedTea](https://icedtea.classpath.org/wiki/Main_Page) 3.6 (OpenJDK 8)

E.g. on Gentoo Linux

```zsh
# use icedtea or icedtea-bin with `gtk` USE flag for X11
❯❯❯ equery l -po icedtea
 * Searching for icedtea ...
[IP-] [  ] dev-java/icedtea-3.6.0:8
```

You may want also following packages:

* [dev-util/android-studio](
  https://packages.gentoo.org/packages/dev-util/android-studio)
* [dev-util/android-tools](
  https://packages.gentoo.org/packages/dev-util/android-tools) (
  for adb or fastboot etc.)

See a link below about Android specific packages.

https://wiki.gentoo.org/wiki/Android


### Build

```zsh
# via gradlew
% make build
```

### Run

See targets `run` or `run-emulator`.

```zsh
# list your virtual devices
% make run ARGS="-list-avds"
./bin/emulator -list-avds
Nexus_5X_API_25
Nexus_5X_API_26
Nexus_5_API_27
Pixel_2_API_26
Pixel_2_API_27
Pixel_2_XL_API_26
...

# run emulator
% make run ARGS="-avd Nexus_5_API_27"
```

### Test

Run JUnit Tests.

```zsh
# via gradlew
% make test
```


## License

`GPL-3.0`

```txt
Generika Android
Copyright (c) 2018 ywesee GmbH
```

See [LICENSE.txt](LICENCE).
