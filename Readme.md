QRMan: A QR-code manipulator
============================

Install ImageMagick + JMagick
-----------------------------

The instructions below are for Ubuntu. They are copy-pasted from
[Rori.me](http://www.rori.me/tech/installing-imagemagick-jmagick-with-eclipse-ubuntu/).

> sudo apt-get install imagemagick
> sudo apt-get install jmagick

...this will, obviously install ImageMagick and JMagick.

To test which version of ImageMagick is installed, run “convert -version”

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