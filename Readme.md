Gyro Gearloose: A QR-code manipulator
=====================================

<img src="http://forum.britishv8.org/file.php?2,file=10790"
 alt="Gyro Gearloose" float="right" />
 
(User Manual, version 2013-10-31)

These instructions are still under construction.

Table of contents                                                    {#toc}
-----------------

- [Dependencies](#dependencies)
- [Compiling](#compiling)
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

About the Author                                                   {#about}
----------------

Gyro Gearloose is developed by Sylvain Hallé, currently an Assistant
Professor at [Université du Québec à Chicoutimi,
Canada](http://www.uqac.ca/) and head of [LIF](http://lif.uqac.ca/), the
Laboratory of Formal Computer Science ("Laboratoire d'informatique
formelle").

[Back to top](#toc)