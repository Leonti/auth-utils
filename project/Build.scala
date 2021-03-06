import bintray.BintrayKeys._
import sbt._
import sbt.Keys._
import com.typesafe.sbt._
import com.typesafe.sbt.SbtGit.GitKeys._

object Build extends sbt.Build {
  val akkaVersion = "2.4.17"
  val akkaHttpVersion = "10.0.5"
  val specs2CoreVersion = "3.8.9"

  lazy val coordinateSettings = Seq(
    organization := "de.choffmeister",
    version in ThisBuild := gitDescribedVersion.value.map(_.drop(1)).get)

  lazy val buildSettings = Seq(
    scalaVersion := "2.12.1",
    scalacOptions ++= Seq("-encoding", "utf8"))

  lazy val resolverSettings = Seq(
    resolvers ++= Seq(
      "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"))

  lazy val publishSettings = Seq(
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    bintrayReleaseOnPublish in ThisBuild := false
  )

  lazy val commonSettings = Defaults.coreDefaultSettings ++ coordinateSettings ++ buildSettings ++
    resolverSettings ++ publishSettings

  lazy val common = (project in file("auth-common"))
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "commons-codec" % "commons-codec" % "1.10",
      "io.spray" %% "spray-json" % "1.3.2",
      "org.specs2" %% "specs2-core" % specs2CoreVersion % "test"))
    .settings(name := "auth-common")

  lazy val akkaHttp = (project in file("auth-akka-http"))
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "com.typesafe" % "config" % "1.3.1",
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "test",
      "org.specs2" %% "specs2-core" % specs2CoreVersion % "test"))
    .settings(name := "auth-akka-http")
    .dependsOn(common)

  lazy val root = (project in file("."))
    .settings(commonSettings: _*)
    .settings(packagedArtifacts := Map.empty)
    .settings(name := "auth")
    .enablePlugins(GitVersioning)
    .aggregate(common, akkaHttp)
}
