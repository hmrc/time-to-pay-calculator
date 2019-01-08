
import sbt._

object MicroServiceBuild extends Build with MicroService {

  val appName = "time-to-pay-calculator"

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {
  import play.sbt.PlayImport._

  private val scalaTestVersion = "3.0.5"
  private val pegdownVersion = "1.6.0"

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "microservice-bootstrap" %  "9.1.0",
    "uk.gov.hmrc" %% "play-ui" % "7.27.0-play-25",
    "uk.gov.hmrc" %% "domain" %  "5.2.0"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test = Seq(
        "org.scalatest" %% "scalatest" % scalaTestVersion % scope,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "uk.gov.hmrc" %% "hmrctest" %  "3.3.0",
        "com.github.tomakehurst" % "wiremock" % "1.58" % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % scope,
        "org.mockito" % "mockito-core" % "2.7.0" % scope
      )
    }.test
  }

  object IntegrationTest {
    def apply(): Seq[ModuleID] = new TestDependencies {

      override lazy val scope: String = "it"

      override lazy val test = Seq(
        "org.scalatest" %% "scalatest" % scalaTestVersion % scope,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "uk.gov.hmrc" %% "hmrctest" %  "3.3.0",
        "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % scope,
        "org.mockito" % "mockito-core" % "2.7.0" % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()
}

