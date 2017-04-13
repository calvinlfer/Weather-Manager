name := "weather-manager"

version := "1.0"

scalaVersion := "2.12.1"

scalacOptions ++= Seq(
  "-deprecation",
  "-Ywarn-unused-import"
)

libraryDependencies ++= {
  val akka = "com.typesafe.akka"
  val akkaV = "2.5.0"
  val akkaHttpV = "10.0.5"
  val circe = "io.circe"
  val circeV = "0.7.1"

  Seq(
    akka                                  %% "akka-actor"         % akkaV,
    akka                                  %% "akka-http"          % akkaHttpV,
    akka                                  %% "akka-http-testkit"  % akkaHttpV,
    circe                                 %% "circe-parser"       % circeV,
    circe                                 %% "circe-generic"      % circeV,
    circe                                 %% "circe-parser"       % circeV,
    "de.heikoseeberger"                   %% "akka-http-circe"    % "1.14.0",
    "ch.megard"                           %% "akka-http-cors"     % "0.2.1",
    "org.scalatest"                       %% "scalatest"          % "3.0.1"   % "test",
    "com.gu"                              %% "scanamo"            % "0.9.2",
    "com.github.t3hnar"                   %% "scala-bcrypt"       % "3.0"
  )
}