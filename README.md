# Motator
GPS route mapper app for Android

Motator is my 2021 hobby project which I aim to have done in time to track my running of the Seattle Marathon on November 28th, 2021.

## Goals

1. Platform: Android
2. Language: Kotlin
3. Built via Terminal
4. Fixed Dual-Square UI w/ Map View
5. Documentation

The first version is _finished_ when the above have been met and crossed off.

### Goal 1. Android

I used to have a [Samsung Galaxy Player](https://en.wikipedia.org/wiki/Samsung_Galaxy_Player) (SGP) 5 that went with me all the way on the [PCT](https://en.wikipedia.org/wiki/Pacific_Crest_Trail) in 2014. I had previously owned an iPod Touch which I loathed but this device I loved and set me on the path of Android. I no longer have the SGP, and Android phones tend to be bloated bastards, but I still intend to support a lower version of Android as much is _conveniently_ possible. If it comes down to it and I have to target a newer version to get this done then I'll do it but my secret hope is to have Gingerbread support. We shall see ...

It would be fantastic to support iOS and other platforms but that will have to wait until I have time to invest cracking open _that_ hood.

### Goal 2. Language: Kotlin

I know some Java but it is not my native language but even moreso, Kotlin is new and mostly unfamiliar to me. I'll use this project as an opportunity to learn it and figure out how to build with it. This also adds a small bit of complexity to the command-line aspect (see Goal #3).

### Goal 3. Built via Terminal

The Chromebook I'm developing this on can run Linux on but is not powerful enough for an app such as Android Studio. And besides, I'm always interested in going beneath the surface of wizardy UI's to get my virtual hands dirty. For those two reasons this app is to be built using a shell script. I'll try to provide some kind of tutorial style documentation utilizing Google Cloud Shell for each of the command steps as this project matures.

### Goal 4: Fixed Dual-Square UI w/ Map View

This is an approach I used for EwePlay that I still like: two squares of UI elements either on top of each other for portrait mode or next to each other in landscape mode. It is heavily biased towards a 16:9 aspect ratio but that's mainly what mobile phones are using still so it makes sense. The elements in the squares always remain the same so rotating the phone sort of "rotates each square". The other aspect of this that I like is that it's a _fixed_ UI: nothing sliding in/out, appearing/disappearing, etc. The elements become easier to trust because their location doesn't change.

* Portrait mode shows square 1 above 2
* Landscape mode shows square 1 left of 2

Square 1 will be the map view using [OpenStreetMap](https://wiki.openstreetmap.org/wiki/Develop) data:

* Map graphics
* GPS points
* Lines connecting GPS points
* Mile markers

Square 2 will contain a slider, stats (text), and some buttons:

* Slider: Position of map view on current route
* Stats: Miles, Time, MPH, Pace 
* Buttons: Reset, Start, Pause, Share
* Reset: Removes all GPS points and stops recording points
* Start: Records GPS points to route every few seconds
* Pause: Stops recording GPS points but retains current
* Share: Export GPS points to GPX file

### Goal 5: Documentation

I'd like this project even in its first version to be a solid _artifact_ and that requires some solid documentation.

* Use `README.md`
* Edit the text then edit it again
* Images for examples
* Tutorial for build
* Example usage scenarios
