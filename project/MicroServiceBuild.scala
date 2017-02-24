import sbt._

object MicroServiceBuild extends Build with MicroService {

  val appName = "time-to-pay-calculator"

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {
  import play.sbt.PlayImport._
  import play.core.PlayVersion

  private val logbackJsonLoggerVersion = "3.1.0"
  private val playUrlBindersVersion = "1.1.0"
  private val scalaTestVersion = "2.2.6"
  private val pegdownVersion = "1.6.0"

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "microservice-bootstrap" %  "5.12.0",
    "uk.gov.hmrc" %% "play-authorisation" %  "4.3.0",
    "uk.gov.hmrc" %% "play-health" %  "2.1.0",
    "uk.gov.hmrc" %% "play-url-binders" %  "2.1.0",
    "uk.gov.hmrc" %% "play-config" %  "3.1.0",
    "uk.gov.hmrc" %% "logback-json-logger" % logbackJsonLoggerVersion,
    "uk.gov.hmrc" %% "domain" %  "4.0.0"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" %  "2.3.0",
        "org.scalatest" %% "scalatest" % scalaTestVersion % scope,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "com.github.tomakehurst" % "wiremock" % "1.58" % scope
      )
    }.test
  }

  object IntegrationTest {
    def apply() = new TestDependencies {

      override lazy val scope: String = "it"

      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" %  "1.9.0",
        "org.scalatest" %% "scalatest" % scalaTestVersion % scope,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % scope
      )
    }.test
  }

  def apply() = compile ++ Test() ++ IntegrationTest()
}

