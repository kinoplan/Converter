<p align="center">
    <img width="300" src="https://raw.githubusercontent.com/ScalablyTyped/Converter/d946f2a8a894f318f4ad6d4786fadedb08267923/website/static/img/logo-1.svg"/>
</p>
<h1 align="center">ScalablyTyped</h1>
<p align="center"><i>Typescript to Scala.js converter</i></p>
<p align="center">
    <a href="https://scalablytyped.org/docs/readme">
        www.scalablytyped.org  
    </a> 
</p>
<p align="center">
  <a href="https://gitter.im/ScalablyTyped/community">
    <img src="https://badges.gitter.im/ScalablyTyped/community.svg"/>
  </a>
  <a href="https://circleci.com/gh/ScalablyTyped/Converter">
    <img src="https://img.shields.io/circleci/build/github/ScalablyTyped/Converter?logo=circleci&style=flat"/>
  </a>
  <a href="https://github.com/scala/scala/releases">
    <img src="https://img.shields.io/badge/scala.js-1.0.0+-red.svg?logo=scala&logoColor=red"/>
  </a>
  <a href="https://central.sonatype.com/search?namespace=io.kinoplan.scalablytyped">
    <img src="https://img.shields.io/maven-central/v/io.kinoplan.scalablytyped/sbt-converter_2.12_1.0.svg?label=Maven%20Central"/>
  </a>
</p>

## Quick Start

**project/plugins.sbt**
```scala
resolvers += "Maven Central Sonatype Snapshots".at("https://central.sonatype.com/repository/maven-snapshots")

addSbtPlugin("org.scala-js"              % "sbt-scalajs"   % "1.21.0")
addSbtPlugin("io.kinoplan.scalablytyped" % "sbt-converter" % "<version>")
```

**build.sbt**
```scala
project
  .enablePlugins(ScalaJSPlugin, ScalablyTypedConverterExternalNpmPlugin)
  .settings(
    scalaVersion := "2.13.18",
    stFlavour    := Flavour.ScalajsReact, // or Slinky/SlinkyNative
    stIgnore     ++= List("typescript"),
    externalNpm  := baseDirectory.value,
  )
```

`externalNpm` points to the directory containing `package.json`. Run `yarn install` there before the sbt build.
