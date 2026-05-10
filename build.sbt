import sbt.Def.spaceDelimited
import sbt.Reference.display

import scala.collection.Seq
import scala.sys.process.stringToProcess

lazy val latestTag =
  "git tag -l --sort=committerdate".!!.linesIterator.toVector.lastOption.fold("no-version")(_.drop( /* 'v' */ 1))

ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always,
  "org.scala-lang.modules" %% "scala-parser-combinators" % VersionScheme.Always,
  "org.scala-lang.modules" %% "scala-collection-compat" % VersionScheme.Always,
)

lazy val scala212 = "2.12.20"
lazy val scala3   = "3.3.4"

val scala2Versions:     Seq[String] = Seq(scala212)
val scala2And3Versions: Seq[String] = scala2Versions ++ Seq(scala3)

lazy val rawAllAggregates =
  logging.projectRefs ++
    core.projectRefs ++
    phases.projectRefs ++
    ts.projectRefs ++
    scalajs.projectRefs ++
    `importer-portable`.projectRefs ++
    `sbt-converter`.projectRefs ++
    importer.projectRefs ++
    cli.projectRefs

lazy val allAggregates = rawAllAggregates

val scopesDescription = "Scala version can be: 2.12, 3; platform: JVM"

val cleanScoped = inputKey[Unit](
  s"Run clean in the given scope. Usage: cleanScoped [scala version] [platform]. $scopesDescription",
)

val compileScoped = inputKey[Unit](
  s"Compiles sources in the given scope. Usage: compileScoped [scala version] [platform]. $scopesDescription",
)

val testScoped = inputKey[Unit](
  s"Run tests in the given scope. Usage: testScoped [scala version] [platform]. $scopesDescription",
)

val scalafmtCheckScoped = inputKey[Unit](
  s"Check sources by scalafmt in the given scope. Usage: scalafmtCheckScoped [scala version] [platform]. $scopesDescription",
)

def filterProject(p: String => Boolean) =
  ScopeFilter(inProjects(allAggregates.filter(pr => p(display(pr.project))) *))

def filterByVersionAndPlatform(scalaVersionFilter: String, platformFilter: String) =
  filterProject { projectName =>
    val byPlatform =
      if (platformFilter == "JVM") !projectName.contains("JS")
      else projectName.contains(platformFilter)
    val byVersion = scalaVersionFilter match {
      case "2.13" => !projectName.contains("2_12") && !projectName.contains("3")
      case "2.12" => projectName.contains("2_12")
      case "3"    => projectName.contains("3")
    }

    byPlatform && byVersion
  }

lazy val core = projectMatrix
  .in(file("core"))
  .jvmPlatform(scala2And3Versions)
  .configure(baseSettings)
  .settings(
    libraryDependencies ++= Seq(
      Deps.osLib.cross(CrossVersion.for3Use2_13),
      Deps.sourcecode.exclude("org.scala-lang.modules", "scala-collection-compat_3"),
      Deps.ammoniteOps.cross(CrossVersion.for3Use2_13),
    ) ++ Deps.circe.map(_.exclude("org.scala-lang.modules", "scala-collection-compat_3")),
  )

lazy val logging = projectMatrix
  .in(file("logging"))
  .jvmPlatform(scala2And3Versions)
  .configure(baseSettings)
  .settings(libraryDependencies ++= Seq(Deps.sourcecode, Deps.fansi))

lazy val ts = projectMatrix
  .in(file("ts"))
  .jvmPlatform(scala2And3Versions)
  .configure(baseSettings, optimize)
  .dependsOn(core, logging)
  .settings(libraryDependencies += Deps.parserCombinators)

lazy val docs = project
  .in(file("converter-docs"))
  .settings(
    mdocVariables := Map("VERSION" -> latestTag),
    moduleName := "converter-docs",
    publish / skip := true,
  )
  .enablePlugins(MdocPlugin, DocusaurusPlugin)

lazy val scalajs = projectMatrix
  .in(file("scalajs"))
  .jvmPlatform(scala2And3Versions)
  .dependsOn(core, logging)
  .configure(baseSettings, optimize)
  .settings(libraryDependencies ++= Seq(Deps.scalaXml))

lazy val phases = projectMatrix
  .in(file("phases"))
  .jvmPlatform(scala2And3Versions)
  .dependsOn(core, logging)
  .configure(baseSettings, optimize)

lazy val `importer-portable` = projectMatrix
  .in(file("importer-portable"))
  .jvmPlatform(scala2And3Versions)
  .configure(baseSettings, optimize)
  .dependsOn(ts, scalajs, phases)
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoPackage := "org.scalablytyped.converter.internal",
    buildInfoKeys := Seq[BuildInfoKey](
      "gitSha" -> "git rev-parse -1 HEAD".!!.split("\n").last.trim,
      "version" -> version.value,
    ),
  )

lazy val importer = projectMatrix
  .in(file("importer"))
  .jvmPlatform(scala2And3Versions)
  .dependsOn(`importer-portable`)
  .configure(baseSettings, optimize)
  .settings(
    libraryDependencies ++= {
      val base = Seq(
        Deps.coursier.cross(CrossVersion.for3Use2_13).exclude("org.scala-lang.modules", "scala-xml_2.13"),
        Deps.scalaXml,
        Deps.scalatest % Test,
      )
      // Parallel collections are built-in for Scala 2.12, need external library for 2.13+
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 12)) => base
        case _             => base :+ (Deps.parallelCollections % Test)
      }
    },
    Test / fork := true,
    assembly / test := {},
    assembly / mainClass := Some("org.scalablytyped.converter.Main"),
    /* meh meh meh */
    assembly / assemblyMergeStrategy := {
      case foo if foo.contains("io/github/soc/directories/")         => MergeStrategy.first
      case foo if foo.contains("reflect.properties")                 => MergeStrategy.first
      case foo if foo.contains("scala-collection-compat.properties") => MergeStrategy.first
      case foo if foo.endsWith("module-info.class")                  => MergeStrategy.discard
      case foo if foo.contains("org/fusesource")                     => MergeStrategy.first
      case foo if foo.contains("META-INF/native/")                   => MergeStrategy.first
      case foo if foo.contains("scala/annotation")                   => MergeStrategy.last
      case foo if foo.contains("META-INF/sisu/javax.inject.Named")   => MergeStrategy.discard
      case other                                                     => (assembly / assemblyMergeStrategy).value(other)
    },
    Test / testOptions += Tests.Argument("-P4"),
  )

lazy val cli = projectMatrix
  .in(file("cli"))
  .jvmPlatform(scala2And3Versions)
  .dependsOn(importer)
  .configure(baseSettings)
  .settings(
    libraryDependencies += Deps.scopt,
  )

lazy val `sbt-converter` = projectMatrix
  .in(file("sbt-converter"))
  .jvmPlatform(scala2Versions)
  .dependsOn(`importer-portable` % "compile->compile;test->test")
  .enablePlugins(ScriptedPlugin)
  .configure(baseSettings)
  .settings(
    name := "sbt-converter",
    sbtPlugin := true,
    addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.21.1"),
    addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.20.1"),
    scriptedBufferLog := false,
    scriptedLaunchOpts ++= Seq("-Xmx2048M", "-Dplugin.version=" + version.value),
    watchSources ++= {
      (sourceDirectory.value ** "*").get
    },
    libraryDependencies ++= Seq(Deps.awssdkS3),
  )

lazy val `import-scalajs-definitions` = projectMatrix
  .in(file("import-scalajs-definitions"))
  .jvmPlatform(scala2And3Versions)
  .configure(baseSettings)
  .dependsOn(importer)
  .settings(
    libraryDependencies ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 12)) =>
          List(
            "org.scala-lang" % "scalap" % scala212,
            Deps.coursier,
          )
        case _ => // Scala 3
          List(
            "org.scala-lang" % "scalap" % "2.13.16",
            Deps.coursier.cross(CrossVersion.for3Use2_13).exclude("org.scala-lang.modules", "scala-xml_2.13"),
          )
      }
    },
    publish / skip := true,
  )

lazy val root = project
  .in(file("."))
  .settings(
    name := "converter-root",
    publish / skip := true,
  )
  .settings(
    cleanScoped :=
      Def.inputTaskDyn {
        val args = spaceDelimited("<arg>").parsed
        Def.taskDyn(clean.all(filterByVersionAndPlatform(args.head, args(1))))
      }.evaluated,
    compileScoped :=
      Def.inputTaskDyn {
        val args = spaceDelimited("<arg>").parsed
        Def.taskDyn((Compile / compile).all(filterByVersionAndPlatform(args.head, args(1))))
      }.evaluated,
    testScoped :=
      Def.inputTaskDyn {
        val args = spaceDelimited("<arg>").parsed
        Def.taskDyn((Test / test).all(filterByVersionAndPlatform(args.head, args(1))))
      }.evaluated,
    scalafmtCheckScoped :=
      Def.inputTaskDyn {
        val args = spaceDelimited("<arg>").parsed
        Def.taskDyn((Compile / scalafmtCheck).all(filterByVersionAndPlatform(args.head, args(1))))
      }.evaluated,
  )
  .aggregate(allAggregates *)

lazy val baseSettings: Project => Project =
  _.settings(
    organization := "io.kinoplan.scalablytyped",
    licenses += ("GPL-3.0", url("https://opensource.org/licenses/GPL-3.0")),
    homepage := Some(url("https://github.com/kinoplan/Converter")),
    developers := List(
      Developer(
        "oyvindberg",
        "Øyvind Raddum Berg",
        "elacin@gmail.com",
        url("https://github.com/oyvindberg"),
      ),
    ),
    scalacOptions ~= (_.filterNot(Set("-Ywarn-unused:imports", "-Ywarn-unused:params", "-Xfatal-warnings"))),
    scalacOptions ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((3, _)) =>
          Seq("-no-indent", "-source:3.3")
        case _ =>
          Seq()
      }
    },
    /* disable scaladoc */
    Compile / doc / sources := Nil,
  )

lazy val optimize: Project => Project =
  _.settings(
    scalacOptions ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, _)) if insideCI.value || !isSnapshot.value =>
          Seq(
            "-opt:l:inline",
            "-opt:l:method",
            "-opt:simplify-jumps",
            "-opt:compact-locals",
            "-opt:copy-propagation",
            "-opt:redundant-casts",
            "-opt:box-unbox",
            "-opt:nullness-tracking",
            "-opt-inline-from:org.scalablytyped.converter.internal.**",
            "-opt-warnings",
          )
        case _ => Nil
      }
    },
  )
