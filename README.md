# Generika Android

Generika App for Android, based on the Generikacc App for iOS.


## Repository

https://github.com/zdavatz/generika_android



## Setup

### Requirements

* [IcedTea](https://icedtea.classpath.org/wiki/Main_Page) 3.6 (OpenJDK 8)
* Play Service Vision API (For Barcode Detection)

#### IcedTea

E.g. on Gentoo Linux

```zsh
# use icedtea or icedtea-bin with `gtk` USE flag for X11
❯❯❯ equery l -po icedtea
 * Searching for icedtea ...
[IP-] [  ] dev-java/icedtea-3.6.0:8
```

You may want also following packages:

* [dev-util/android-sdk-update-manager](
  https://packages.gentoo.org/packages/dev-util/android-sdk-update-manager)
* [dev-util/android-tools](
  https://packages.gentoo.org/packages/dev-util/android-tools) (
  for adb or fastboot etc.)

See a link below about Android specific packages.

https://wiki.gentoo.org/wiki/Android

#### Play Service (Vision API)

It's automatically prepared before initial boot on device.  
This application is needed.

https://play.google.com/store/apps/details?id=com.google.android.gms&hl=en


### Build

```zsh
# via gradlew
% make build

# create apk (debug)
% make archive
# or
% gradle assemble

# create apk (release)
% make release
# or
% gradle assembleRelease
```

### Run

See targets `run` (connected emulator device).

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

# run
% make run ARGS="-avd Nexus_5_API_27"
```

For example, build-run-stop (cycle) will be like this:  
(at console, run an emulator on X11 DISPLAY)

```zsh
# pass $ARGS to gradlew
% make build ARGS="-x lint"

# run on your X11 DISPLAY ;)
% DISPLAY=":0" make run ARGS="-avd Nexus_5_API_27"

% make stop
```

#### Debugging Steps

Run the app on your device (connected via usb/wifi)

```zsh
# kill/start server
% make serve
...
* daemon not runnig; starting now at tcp:5037
* daemon started successfully

% make start ARGS="5555"
% make attach ARGS="<DEVICE_IP_ADDRESS>:5555"
% make list

% make archive
% make install ARGS="..."

# start application

# tail only application logs from attached process
% make log

% make detach
```

### Test

Run JUnit Tests.

```zsh
# via gradlew
% make test
% make test ARGS="--debug"
% make test ARGS="--stacktrace"
```


## License

`GPL-3.0`

```txt
Generika Android
Copyright (c) 2018 ywesee GmbH
```

See [LICENSE.txt](LICENCE).


Some parts of barcode detection work with Android Mobile Vision API using
extended codes based on codes which are included in samples project provided
by The Android Open Source Project as `APACHE-2.0`.

https://apache.org/licenses/GPL-compatibility.html

```txt
Apache 2 software can therefore be included in GPLv3 projects, because
the GPLv3 license accepts our software into GPLv3 works. However, GPLv3
software cannot be included in Apache projects. The licenses are incompatible
in one direction only, and it is a result of ASF's licensing philosophy and
the GPLv3 authors' interpretation of copyright law.
```

```txt
/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
```

See [googlesamples/android-vision](
https://github.com/googlesamples/android-vision).

## French Translations
Dr. André Dubied
