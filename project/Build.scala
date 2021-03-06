import com.typesafe.sbt.{GitVersioning, GitBranchPrompt}
import com.typesafe.sbteclipse.plugin.EclipsePlugin._
import sbt.Keys._
import sbt._
import sbtbuildinfo.Plugin._
import com.typesafe.sbt.SbtGit.{git, useJGit}

object BuildSettings {
  val buildOrganization = "ch.unibas.cs.gravis"


  val buildScalaVersion = "2.11.8"
  val publishURL = Resolver.file("file", new File("/export/contrib/statismo/repo/private"))

  val buildSettings = Defaults.defaultSettings ++ Seq(
    scalacOptions ++= Seq("-encoding", "UTF-8", "-Xlint", "-deprecation", "-unchecked", "-feature"),
    organization := buildOrganization,
    scalaVersion := buildScalaVersion,
    crossScalaVersions := Seq("2.11.8", "2.12.1")
  ) ++ buildInfoSettings ++ Seq(
    sourceGenerators in Compile <+= buildInfo,
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, BuildInfoKey.action("buildTime") {
      new java.text.SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z (z)", java.util.Locale.US).format(new java.util.Date)
    }, BuildInfoKey.action("buildTimestamp") {
      System.currentTimeMillis()
    }),
    buildInfoPackage := "scalismo.ui")

}

object Resolvers {
  private val sonatypeSnapshots = "Sonatype SNAPSHOTs" at "https://oss.sonatype.org/content/repositories/snapshots/"
  private val sonatypeRelease = "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"
  private val twitter = "twitter" at "http://maven.twttr.com/"
  val stkResolvers = Seq( Resolver.jcenterRepo, sonatypeSnapshots, sonatypeRelease, twitter)
}

object Creds {
  val scalismoPrivate = Credentials(Path.userHome / ".ivy2" / ".credentials-statismo-private")
}

object Dependencies {
  val scalismo = "ch.unibas.cs.gravis" %% "scalismo" % "0.15.0-RC1"
  val scalismoNative = "ch.unibas.cs.gravis" % "scalismo-native-all" % "4.0.0"
  val scalatest = "org.scalatest" %% "scalatest" % "3.0.1" % "test"
  // the following two dependencies are transitively obtained through other dependencies
  //val scalaReflect = "org.scala-lang" % "scala-reflect" % buildScalaVersion
  //val scalaSwing = "org.scala-lang" % "scala-swing" % buildScalaVersion
  val scalaAsync = "org.scala-lang.modules" %% "scala-async" % "0.9.+"
  val scalaInterpreterPane = "de.sciss" %% "scalainterpreterpane" % "1.7.+"
  val jIconFont = "com.github.jiconfont" % "jiconfont-swing" % "1.0.1"
  val jIconFontAwesome = "com.github.jiconfont" % "jiconfont-font_awesome" % "4.5.0.3"
  //val jIconFontGoogleMaterial = "com.github.jiconfont" % "jiconfont-google_material_design_icons" % "2.2.0.1"
  val jIconFontElusive = "com.github.jiconfont" % "jiconfont-elusive" % "2.0.2"
  val jIconFontEntypo = "com.github.jiconfont" % "jiconfont-entypo" % "2.0.2"
}

object ScalismoUiBuild extends Build {

  import BuildSettings._
  import Dependencies._
  import Resolvers._

  // Sub-project specific dependencies
  val commonDeps = Seq(
    scalismo,
    scalismoNative,
    scalatest,
    // the following two dependencies are transitively obtained through other dependencies.
    //scalaReflect,
    //scalaSwing,
    scalaAsync,
    scalaInterpreterPane,
    jIconFont,
    jIconFontAwesome,
    jIconFontElusive,
    jIconFontEntypo
    //1jIconFontGoogleMaterial
  )

  lazy val scalismo_ui = Project(
    "scalismo-ui",
    file("."),
    settings = buildSettings ++ Seq(
      libraryDependencies ++= commonDeps,
      resolvers ++= stkResolvers,
      credentials += Creds.scalismoPrivate,
      publishTo := Some(publishURL),
      EclipseKeys.withSource := true,
      git.useGitDescribe := false,
      git.baseVersion := "develop",
      useJGit)).enablePlugins(GitBranchPrompt, GitVersioning)

}
