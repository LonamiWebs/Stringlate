# Stringlate help
- [I am new to GitHub. Where do I start?](#im-new)
- [How do I tell the developers that I translated their app?](#how-to-let-know)
- [Do I need this app to help the developers?](#app-required)
- [How do I load my previous work?](#load-progress)
- [What does `%s` or `%1$d/%2$d` mean?](#string-symbols)
- [Why do some locales have more strings than others?](#locale-missing-strings)
- [I will use this app to cause chaos!](#chaos)

## I am new to GitHub. Where do I start? <a name="im-new"></a>
If you're new to GitHub and still want to help the FOSS community, that's
great! You've taken a great decision. The first step is to get your own
[GitHub account](https://github.com/join) (it's free!). This step is required
to later let the apps' developers know that you've translated their application.

Once this is done, get *Stringlate*, **add** the repository of your favourite
application and **start translating** all the *strings* to your own locale.

## How do I tell the developers that I translated their app? <a name="how-to-let-know"></a>
You've finished? That was quick! From *Stringlate*, you're able to export
the result of your hard work by tapping `Menu > Export…`. You can export
the resulting file to the *SD card*, to a *GitHub Gist*, or simply sharing
the content of the file by any other way (even *email*).

Once this is done, get the resulting file or URL from wherever you exported it
and head to the [issues](https://github.com/LonamiWebs/Stringlate/issues) page
of the repository you chose. Click on the green `New issue` button (you need
to be logged in), and title your issue something like *"Added XXX translation"*.
Provide some link or another way for the author to get your translation, and
you're done!

Please note that the author might have another way for people to translate
their application, (for example, they might use a different
[online platform](https://www.transifex.com/)). However, don't give up if
they close your issue telling you this. Every help is appreciated!

## Do I need this app to help the developers? <a name="is-app-required"></a>
Absolutely **not**. This application was made to make it easier, but it's not
the only way. You can head to any repository, for example,
[this](https://github.com/LonamiWebs/Stringlate) repository, press the
<kbd>T</kbd> key (to search for a file) and type `strings.xml`. This will find
all the strings in that repository.

The `res/values/strings.xml` file is the **original file** containing all the
strings. The `res/values-xx/strings.xml` paths are **other locales** (for
example, `en` for *English*) that someone else already translated.

Click the *original file*, select `Raw` and save it on your computer.

Once this is done, rename the file as, for instance, `strings-xx.xml`.
Open it in a text editor of your choice and start translating all the
strings which do *not* contain `translatable="false"`. If any *tag* contains
`translatable="false"`, delete it from the file.

After you've finished, let the developers know that you have a new translation
available as explained above.

## How do I load my previous work? <a name="load-progress"></a>
The first time you add a repository, the `strings.xml` files contained in
it are downloaded to the **internal storage** of your device, in the directory
of the application (unless you're a root user, you won't notice this).

If you **clear the application data**, these files will be **gone**. Make sure you
didn't have any translation left before doing this or uninstalling!

Every time you open a previously saved repository, these files are **loaded
automatically**, without the need for you to take any further action.

When you edit the translation string, these changes are conserved on the phone
RAM. For them to **persist**, make sure you click the **Save** button on screen.
Next time you open the application, you will see these changes.

## What does `%s` or `%1$d/%2$d` mean? <a name="string-symbols"></a>
`%s` is used to "insert" another **s**tring on that position. For example,
imagine you had to greet your users with "*Hello Username, welcome!*".
*Username* would be a value that can change, so we would write
"*Hello `%s`, welcome!*" and the developer would insert the right value there.

The `%1$d` syntax, albeit being a bit more complex, simply indicates the
position to insert a **d**ecimal number. For example, when showing the progress
"*42 out of 100*", you would write "*`%1$d` out of `%2$d`*", because in some
languages the order might change, and thus the position is required.

## Why do some locales have more strings than others? <a name="locale-missing-strings"></a>
Actually, all the locales have the same amount of strings. By default,
those strings which have already been translated are *not* shown not to
disturb (if they are translated already, usual thing is you want to translate
the strings left!). However, if you still want to see all the strings, for
example, to review them looking for typos, you can open the menu and enable
"*Show translated strings*".

## I will use this app to cause chaos! <a name="chaos"></a>
Please **don't**. Application developers are people like you, with good
intentions and often busy lives. Don't make them waste their time (and users)
on incorrect, incomplete, wrong, or even offensive, translations.

There is no way to prevent these things from happening from the application,
or even if this application didn't exist. Developers trust on the good will
of the people who help them. If you're troll, please don't waste your time
on this. There are thousands of websites where you can go to have some fun
instead.
