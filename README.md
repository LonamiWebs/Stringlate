# Stringlate
*Help translating FOSS applications.*

[![Translate - with Stringlate](https://img.shields.io/badge/stringlate-translate-green.svg)](https://lonamiwebs.github.io/stringlate/translate?git=https%3A%2F%2Fgithub.com%2FLonamiWebs%2Fstringlate.git&name=Stringlate&web=https%3A%2F%2Fgithub.com%2FLonamiWebs%2FStringlate)

[<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="60">](https://f-droid.org/app/io.github.lonamiwebs.stringlate)

## Description
This application was born to help the FOSS community translate their Android
applications in an easy way. Perhaps your family doesn't know enough English
and you might want to translate a FOSS application for them, then this is
application is the right way to go.

Its purpose is **fetching** a Git repository containing the **source code**
for an Android application, and once the string resources (`strings.xml` files) in it are
downloaded, you'll be able to **translate it** whenever you want.

Some applications have a large amount of `strings` waiting to be translated.
This is the reason why it's not a desktop application, so it could be done
anywhere (*no internet connection is required* once the `strings` are downloaded).
Once you're done,you can **export** the translations to the SD card, share it to any service,
send it via email, create a GitHub Gist or simply copy its contents to the clipboard.

Of course, once you have completed the translation, drop the author a Pull-Request and
let them know you've translated their application. They'll be really happy!

## Help
Still have questions on how to use the application? You can check out the
online help by going to the [help index](help/index.md).

## Permissions
- **Internet**. Required to be able to fetch a remote GitHub repository in
  order to download its contents. Also used to post a GitHub *Gist* and to
  load the *Discover* activity.
- **Access network state**. Required to warn the user that they're not
  connected to the internet (and to prevent the application from crashing).

## Notes
This application has not yet been tested on pre-Lollipop devices and it
*might* fail on some operations, such as exporting a file to the SD card. If
this is the case, please let me know, or create a new pull request with a fix.

## Screenshots
![Screenshot 1](https://github.com/XyLoNaMiyX/Stringlate-metadata/raw/HEAD/en-GB/phoneScreenshots/01.png)
![Screenshot 2](https://github.com/XyLoNaMiyX/Stringlate-metadata/raw/HEAD/en-GB/phoneScreenshots/02.png)
![Screenshot 3](https://github.com/XyLoNaMiyX/Stringlate-metadata/raw/HEAD/en-GB/phoneScreenshots/03.png)
![Screenshot 4](https://github.com/XyLoNaMiyX/Stringlate-metadata/raw/HEAD/en-GB/phoneScreenshots/04.png)
![Screenshot 4](https://github.com/XyLoNaMiyX/Stringlate-metadata/raw/HEAD/en-GB/phoneScreenshots/05.png)


(*Thanks to [Clean Status Bar](https://f-droid.org/app/com.emmaguy.cleanstatusbar)
for the tidy screenshots*)

## Donations
Please refer to https://lonamiwebs.github.io/donate/. Remember that not only
you can donate money, but also appreciation. Both are very welcome!

## License
The project is licensed under the
[MIT License](https://github.com/LonamiWebs/Stringlate/blob/master/LICENSE).
