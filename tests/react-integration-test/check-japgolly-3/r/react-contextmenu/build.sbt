organization := "org.scalablytyped"
name := "react-contextmenu"
version := "2.13.0-d31395"
scalaVersion := "3.3.6"
enablePlugins(ScalaJSPlugin)
libraryDependencies ++= Seq(
  "com.github.japgolly.scalajs-react" %%% "core" % "3.0.0",
  "com.olvind" %%% "scalablytyped-runtime" % "2.4.2",
  "org.scalablytyped" %%% "react" % "16.9.2-cde879",
  "org.scalablytyped" %%% "std" % "0.0-unknown-1546a8")
publishArtifact in packageDoc := false
scalacOptions ++= List("-encoding", "utf-8", "-feature", "-language:implicitConversions", "-language:higherKinds", "-language:existentials", "-no-indent")
licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
