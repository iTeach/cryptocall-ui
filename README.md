# CryptoCall UI

TODO: description

# Contribute

Fork CryptoCall and do a merge request. I will merge your changes back into the main project.

# Build

## Build with Ant

1. Have Android SDK in your path
2. Execute ``android update project -p .`` in ``.`` and ``android-libs/ActionBarSherlock``
3. Execute ``ant debug``

## Build with Eclipse

1. File -> Import -> Android -> Existing Android Code Into Workspace, choose "android-libs/ActionBarSherlock"
2. File -> Import -> Android -> Existing Android Code Into Workspace, choose main directory
3. CryptoCall can now be build

# Libraries

The Libraries are provided in the git repository.

* ActionBarSherlock to provide an ActionBar for Android < 3.0
* Spongy Castle Crypto Lib (Android version of Bouncy Castle)
* android-support-v4.jar: Compatibility Lib
* zxing.jar: QR Code generation lib

## Build Barcode ZXing

1. Checkout their SVN (see http://code.google.com/p/zxing/source/checkout)
2. Change directory to core
3. Build using ``ant build``

## Build Spongy Castle

Spongy Castle is the stock Bouncy Castle libraries with a couple of small changes to make it work on Android.

see http://rtyley.github.com/spongycastle/