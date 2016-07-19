//senablePlugins(SbtNativePackager)

enablePlugins(JavaAppPackaging)

name := "3DnP-Server"

jfxSettings

JFX.mainClass := Some("com.misiunas.np.Main")

// for packaging with 'sbt package-javafx'
JFX.nativeBundles := "image"

version := "1.0"

scalaVersion := "2.11.7"


// Libraries

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.11"

libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.3.11"

libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"

libraryDependencies += "org.controlsfx" % "controlsfx" % "8.40.9"

libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "2.0.0"

libraryDependencies += "com.typesafe" % "config" % "1.3.0"

// USB serial communication -> complex
//libraryDependencies += "org.usb4java" % "usb4java-javax" % "1.2.0"

// much simpler!
libraryDependencies += "org.scream3r" % "jssc" % "2.8.0"



// local library
libraryDependencies += "com.misiunas" % "geoscala_2.11" % "0.2.0"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.3"

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.4.2"

// some cool font-icons: http://www.jensd.de/wordpress/?p=733 and http://www.jensd.de/wordpress/?p=692
libraryDependencies += "de.jensd" % "fontawesomefx" % "8.6"
