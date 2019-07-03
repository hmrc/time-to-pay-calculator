import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-play-26" % "0.41.0"
  )

  val test = Seq(
    "org.scalatest" %% "scalatest" % "3.0.4",
    "org.pegdown" % "pegdown" % "1.6.0",
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2",
    "com.github.tomakehurst" % "wiremock-jre8" % "2.21.0",
    "org.mockito" % "mockito-core" % "2.23.0"
  )


}