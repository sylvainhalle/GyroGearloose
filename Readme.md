Gyro Gearloose: A QR-code manipulator
=====================================

<img src="http://forum.britishv8.org/file.php?2,file=10790"
 alt="Gyro Gearloose" height="350" float="right" />
 
(User Manual, version 2015-02-12)

These instructions are still under construction.

Table of contents                                                    {#toc}
-----------------

- [Dependencies](#dependencies)
- [Compiling](#compiling)
- [Command-line Usage](#cli)
- [About the Author](#about)

Dependencies                                                {#dependencies}
------------

To compile and run Gyro Gearloose, a number of libraries (jar files) must
also be installed. They are listed below, along with instructions on how
to install them.

Overall, the best way is to download the jars and to put them in Java's
extension folder. This location varies according to the operating system
you use:

- Solaris™ Operating System: `/usr/jdk/packages/lib/ext`
- Linux: `/usr/java/packages/lib/ext`
- Microsoft Windows: `%SystemRoot%\Sun\Java\lib\ext`

and do **not** create subfolders there (i.e. put all archives directly
in that folder).

### BufferTannen

[BufferTannen](https://github.com/sylvainhalle/BufferTannen) is a library
implementing a protocol for broadcast-only, low-bandwidth data streams. The
contents of QR codes read and written by GyroGearloose are created using
this protocol. Follow its installation instructions, and copy the resulting
file `BufferTannen.jar` in the extension directory.

### Zxing

[Zxing](https://github.com/zxing/zxing) is a library that allows one to
create QR codes from contents, and to decode QR codes from an image.
Precompiled JAR files can be [downloaded](https://github.com/zxing/zxing/wiki/Getting-Started-Developing).
Look for a file in the "core" folder. *(Tested with version 2.3)*

### OpenCV

[OpenCV](http://opencv.org/downloads.html) is required to use a camera. This
is a hefty download, which then requires some extensive installation steps
for [Linux](http://docs.opencv.org/doc/tutorials/introduction/linux_install/linux_install.html#linux-installation)
or [Windows](http://docs.opencv.org/doc/tutorials/introduction/windows_install/windows_install.html#windows-installation).
Once the build is over, follow further [instructions to configure it for
Eclipse](http://docs.opencv.org/trunk/doc/tutorials/introduction/java_eclipse/java_eclipse.html).
*(Tested with version 2.4.6. Look out! Will not work with OpenCV 3.x, as
it contains [API-breaking changes](http://stackoverflow.com/a/25943085).)*

### Xuggle

[Xuggle](http://www.xuggle.com) is required to read video files. We
recommend downloading an archive of a precompiled version of Xuggle.
The archive for the latest distribution comes with many files; normally,
you only need to copy the "main" jar (probably called
`xuggle-xuggler-x.x.jar`) and you're good to go. *(Tested with version
6.4.)*

### slf4j

[slf4j](http://www.slf4j.org) is a dependency required by Xuggle.
Gyro Gearloose does not use it directly. The [archive](http://www.slf4j.org/download.html)
for the latest distribution comes with many files; you must copy
to the extension folder the "main" jar (called slf4j-api) and only
*one* of the remaining jars (we recommend slf4j-nop). *(Tested with
version 1.7.5.)*

### ImageMagick + JMagick

[ImageMagick](http://imagemagick.org) and [JMagick](http://www.jmagick.org/)
are used to generate animated GIF files from a trace of events. The
installation instructions below are for Ubuntu. They are copy-pasted from
[Rori.me](http://www.rori.me/tech/installing-imagemagick-jmagick-with-eclipse-ubuntu/).

    sudo apt-get install imagemagick
    sudo apt-get install jmagick

...this will, obviously install ImageMagick and JMagick. The JMagick jar
file is located in folder `/usr/share/java`, and is likely to be called
something like `jmagick6-6.2.6-0.jar`. You must copy this jar to the
extension folder.

To test which version of ImageMagick is installed, run “convert -version” on
the command line. Gyro Gearloose was tested with version 6.2.6 of JMagick.

#### Setup in Eclipse (optional)

Open up your Java project in Eclipse, and add the JMagic jar to the build
path as an external file. In my case, the jar was located at
/usr/share/java/jmagick6-6.2.6-0.jar

In Eclipse, go to Project -> Properties -> Java Build Path -> Libraries
and click on the JMagick jar. You should see a field called “native library
location”. Enter the directory of the libJMagick.so file. In my case it was
‘/lib/jni/’.

If the native library location is not set, you’ll get an exception:
Exception in thread "main" java.lang.UnsatisfiedLinkError: no JMagick in
java.library.path.

### Apache Commons

[Commons](http://commons.apache.org/) is an excellent set of general purpose
libraries for Java. Gyro Gearloose uses two of these libraries:

- [CLI](http://commons.apache.org/proper/commons-cli/) to handle command-line
  parameters *(tested with version 1.2)*
- [Codec](http://commons.apache.org/proper/commons-codec/) to generate Base64
  strings from binary strings when writing QR codes *(tested with version
  1.8)*

You may want to install only these two jars, or the whole Commons API if
you're a Java developer.

[Back to top](#toc)

Compiling
---------

Once the dependencies are all installed, make sure you also have the
following installed:

- The Java Development Kit (JDK) to compile. BeepBeep was developed and
  tested on version 6 of the JDK, but it is probably safe to use any
  later version. Look out for the requirements of the other libraries in
  such a case.
- [Ant](http://ant.apache.org) to automate the compilation and build process

Download the sources for Gyro Gearloose by cloning the repository using Git:

    git clone git://github.com/sylvainhalle/GyroGearloose.git

Compile the sources by simply typing:

    ant

This will produce a file called `GyroGearloose.jar` in the `dist` subfolder.
This file is runnable and stand-alone, so it can be moved around to the
location of your choice (provided you have the other dependencies in the
extension folder, see instructions above).

You can also generate the documentation by typing:

    ant javadoc

This will produce a file called `GyroGearloose-doc.jar` in the `dist`
subfolder.

[Back to top](#toc)

Command-line Usage                                                   {#cli}
------------------

GyroGearloose can be used in two modes: animate and read, which are
described below.

### Animate mode

In the animate mode, the program takes as input as source of data (a file,
pipe, etc.), and produces a sequence of QR codes. You can choose to display
that sequence "live" in a window, or export it to an animated GIF. The
general command-line syntax is the following:

    java -jar GyroGearloose.jar animate [options] [file [schema1 [schema2 ...]]]

The argument `file` is optional; it specifies the source file to read, and
from which QR codes will be generated.

The arguments `schema1`, etc. are also optional. The contain schema files
that will be used to encode the contents of `file`.

When called without the `--file` option, the program will display an
empty window. The animation of the source contents into QR codes can be
started by pressing `S` on the keyboard; it can be paused/resumed by
pressing `S`. The window can be resized; however this will only change the
amount of white space around the code, and not resize the code itself.
Closing the window at any moment closes the program.

Command-line switches are:

`--binary`
:   Encode input file as BufferTannen blob segments (i.e., as a meaningless
    stream of binary data)

`--format <f>`
:   Write codes using format f, which can either be
    [qr](http://en.wikipedia.org/wiki/QR_code),
    [aztec](http://en.wikipedia.org/wiki/Aztec_Code) or
    [datamatrix](http://en.wikipedia.org/wiki/Data_Matrix). The default is
    qr.

`-h`, `--help`
:   Display command line usage

`-l`, `--level <x>`
:   Set error correction level to x (either L (7%), M (15%), Q (25%), or H
    (30%)). The default is L.

`--lake`
:   Display source contents in BufferTannen's "lake" mode

`--noloop`
:   Don't loop through frames when sending in lake mode

`--output <file>`
:   Output GIF animation to file. If not specified, the output is displayed
    in a window onscreen

`-p`, `--pipe`
:   Specifies that the input file is a pipe (not a regular file)

`-z`, `--framesize <x>`
:   Set maximum frame size to x bits (default: 2000). This is not the
    resolution of the code (i.e. number of pixels), but the amount of data
    each code can contain at most.

 `-r`, `--framerate <x>`
:   Set animation speed to x codes per second (default: 8)

`-s`, `--size <x>`
:   Set output image size to a square of side x pixels (default: 300)

`--stdin`
:   Read input from stdin

### Read mode

The read mode is the opposite of the animate mode: the program receives as
input a sequence of pictures (either from a video file, or captured live
from a camera), interprets the codes found in each picture, and outputs
back the contents of the code stream.

    java -Djava.library.path=%LIBPATH% -jar GyroGearloose.jar read [options] [file]

where `%LIBPATH%` must be replaced by the location of the *native* library
folder where OpenCV resides (the same folder you used for the OpenCV Eclipse
configuration in the instructions above; in Linux this is typically
`/usr/local/share/OpenCV/java`). Alas, there is no way to put this parameter
in some configuration file as it is system-dependent; you may want to
consider creating a batch file.

The `file` argument is optional. If specified, input will be read from that
file, which can either be a video (MP4, AVI or MKV) or a sequence of images
(in that case specify multiple file names). If not given, the input will be
read from the USB camera.

By default, the decoded contents are sent to the standard output. Use a
redirection to save it to a file, or use the `--mute` option to discard it.
    
Command-line switches are:

`--binary`
:   Tells the reader that the codes contain BufferTannen blob segments

`-h`, `--help`
:   Display command line usage

`--mute`
:   Don't output decoded contents to stdout, just print stats. This option
    is useful if one wants only to test the decoding, without caring about
    the received contents.

`--purecode`
:   Tells reader that input is a set of pure binary images of codes

`-r`, `--framerate <x>`
:   When reading from a camera, process images at x fps (default: 8)

`--threshold <x>`
:   Set binarization threshold to x ('guess', or between 0 and 255, default
    128). Binarization is the process of converting a colour image to a
    strictly black-and-white (i.e. 1-bit) image before processing its
    contents. The threshold is the maximum amount of brightness a pixel can
    have to be converted to full-black; otherwise it will become full-white.
    The `guess` option has the program attempt to find the threshold that
    maximizes the probability of finding a code in each picture. This
    consumes much more time and CPU than using a fixed value.

`--verbosity <x>`
:   Verbose messages with level x

[Back to top](#toc)

About the Author                                                   {#about}
----------------

Gyro Gearloose is developed by Sylvain Hallé, currently an Associate
Professor at [Université du Québec à Chicoutimi,
Canada](http://www.uqac.ca/) and head of [LIF](http://lif.uqac.ca/), the
Laboratory of Formal Computer Science ("Laboratoire d'informatique
formelle").

[Back to top](#toc)