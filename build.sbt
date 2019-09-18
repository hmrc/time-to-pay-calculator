import TestPhases.oneForkedJvmPerTest
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences._
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, integrationTestSettings, scalaSettings}
import uk.gov.hmrc.SbtArtifactory
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import wartremover.{Wart, wartremoverErrors, wartremoverExcluded, wartremoverWarnings}

lazy val appName = "time-to-pay-calculator"

val scalaCompilerOptions = Seq(
  "-Xfatal-warnings",
  "-Xlint:-missing-interpolator,_",
  "-Yno-adapted-args",
  "-Ywarn-value-discard",
  "-Ywarn-dead-code",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-language:implicitConversions",
  "-Ypartial-unification" //required by cats
)

lazy val scalariformSettings: Def.SettingsDefinition = {
  // description of options found here -> https://github.com/scala-ide/scalariform
  ScalariformKeys.preferences := ScalariformKeys.preferences.value
    .setPreference(AlignArguments, true)
    .setPreference(AlignParameters, true)
    .setPreference(AlignSingleLineCaseStatements, true)
    .setPreference(AllowParamGroupsOnNewlines, true)
    .setPreference(CompactControlReadability, false)
    .setPreference(CompactStringConcatenation, false)
    .setPreference(DanglingCloseParenthesis, Force)
    .setPreference(DoubleIndentConstructorArguments, true)
    .setPreference(DoubleIndentMethodDeclaration, true)
    .setPreference(FirstArgumentOnNewline, Force)
    .setPreference(FirstParameterOnNewline, Force)
    .setPreference(FormatXml, true)
    .setPreference(IndentLocalDefs, true)
    .setPreference(IndentPackageBlocks, true)
    .setPreference(IndentSpaces, 2)
    .setPreference(IndentWithTabs, false)
    .setPreference(MultilineScaladocCommentsStartOnFirstLine, false)
    .setPreference(NewlineAtEndOfFile, true)
    .setPreference(PlaceScaladocAsterisksBeneathSecondAsterisk, false)
    .setPreference(PreserveSpaceBeforeArguments, true)
    .setPreference(RewriteArrowSymbols, false)
    .setPreference(SpaceBeforeColon, false)
    .setPreference(SpaceBeforeContextColon, false)
    .setPreference(SpaceInsideBrackets, false)
    .setPreference(SpaceInsideParentheses, false)
    .setPreference(SpacesAroundMultiImports, false)
    .setPreference(SpacesWithinPatternBinders, true)
}

lazy val wartRemoverWarning = {
  val warningWarts = Seq(
    Wart.JavaSerializable,
    Wart.StringPlusAny,
    Wart.AsInstanceOf,
    Wart.IsInstanceOf
    //Wart.Any
  )
  wartremoverWarnings in(Compile, compile) ++= warningWarts
}

lazy val wartRemoverError = {
  // Error
  val errorWarts = Seq(
    Wart.ArrayEquals,
    Wart.AnyVal,
    Wart.EitherProjectionPartial,
    Wart.Enumeration,
    Wart.ExplicitImplicitTypes,
    Wart.FinalVal,
    Wart.JavaConversions,
    Wart.JavaSerializable,
    //Wart.LeakingSealed,
    Wart.MutableDataStructures,
    Wart.Null,
    //Wart.OptionPartial,
    //Wart.Recursion,
    Wart.Return,
    //Wart.TraversableOps,
    //Wart.TryPartial,
//    Wart.Var, //TODO: if you have time uncomment it and fix compilation error
    Wart.While)
  wartremoverErrors in(Compile, compile) ++= errorWarts
}

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := "<empty>;.*BuildInfo.*;Reverse.*;app.Routes.*;prod.*;testOnlyDoNotUseInProd.*;manualdihealth.*;forms.*;config.*;",
    ScoverageKeys.coverageExcludedFiles := ".*microserviceGlobal.*;.*microserviceWiring.*;.*ApplicationLoader.*;.*ApplicationConfig.*;.*package.*;.*Routes.*;.*TestOnlyController.*;.*WebService.*",
    ScoverageKeys.coverageMinimum := 80,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val commonSettings = Seq(
  scalaVersion := "2.11.11",
  majorVersion := 0,
  scalacOptions ++= scalaCompilerOptions,
  resolvers ++= Seq(Resolver.bintrayRepo("hmrc", "releases"), Resolver.jcenterRepo),
  evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
  wartremoverExcluded ++=
    (baseDirectory.value / "it").get ++
      (baseDirectory.value / "test").get ++
      Seq(sourceManaged.value / "main" / "sbt-buildinfo" / "BuildInfo.scala"),
  scalariformSettings
)
  .++(wartRemoverError)
  .++(wartRemoverWarning)
  .++(Seq(
    wartremoverErrors in(Test, compile) --= Seq(Wart.Any, Wart.Equals, Wart.Null, Wart.NonUnitStatements, Wart.PublicInference)
  ))
  .++(scoverageSettings)
  .++(scalaSettings)
  .++(uk.gov.hmrc.DefaultBuildSettings.defaultSettings())

lazy val microservice = Project(appName, file("."))
  .enablePlugins(
    play.sbt.PlayScala,
    SbtAutoBuildPlugin,
    SbtGitVersioning,
    SbtDistributablesPlugin,
    SbtArtifactory
  )
  .settings(commonSettings: _*)
  .settings(SbtDistributablesPlugin.publishingSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      ws,
      "uk.gov.hmrc" %% "bootstrap-play-26" % "0.41.0",
      "uk.gov.hmrc" %% "domain" % "5.6.0-play-26",
      "org.scalatest" %% "scalatest" % "3.0.4" % Test,
      "org.pegdown" % "pegdown" % "1.6.0" % Test,
      "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test,
      "com.github.tomakehurst" % "wiremock-jre8" % "2.21.0" % Test,
      "org.mockito" % "mockito-core" % "2.23.0" % Test
    ),
    routesGenerator := InjectedRoutesGenerator,
    majorVersion := 0,
    PlayKeys.playDefaultPort := 8886,
    wartremoverExcluded ++= routes.in(Compile).value,
    routesImport ++= Seq(
      "timetopaycalculator.cor.model._"
    )
  )
  .dependsOn(cor)
  .aggregate(cor)


lazy val cor = Project(appName + "-cor", file("cor"))
  .enablePlugins(
    SbtAutoBuildPlugin,
    SbtGitVersioning,
    SbtArtifactory
  )
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= List(
      "com.typesafe.play" %% "play" % play.core.PlayVersion.current % Provided,
      "uk.gov.hmrc" %% "bootstrap-play-26" % "0.41.0" % Provided
    )
  )
