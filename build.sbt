name := "weather-manager"

version := "1.0"

scalaVersion := "2.12.1"

scalacOptions ++= Seq(
  "-deprecation",
  "-Ywarn-unused-import"
)

enablePlugins(JavaAppPackaging)

resolvers += Resolver.bintrayRepo("lightshed", "maven")

libraryDependencies ++= {
  val akka = "com.typesafe.akka"
  val akkaV = "2.4.17"
  val akkaHttpV = "10.0.5"
  val circe = "io.circe"
  val circeV = "0.7.1"

  Seq(
    akka                                  %% "akka-actor"           % akkaV,
    akka                                  %% "akka-slf4j"           % akkaV,
    akka                                  %% "akka-http"            % akkaHttpV,
    akka                                  %% "akka-http-testkit"    % akkaHttpV,
    circe                                 %% "circe-parser"         % circeV,
    circe                                 %% "circe-generic"        % circeV,
    circe                                 %% "circe-parser"         % circeV,
    "de.heikoseeberger"                   %% "akka-http-circe"      % "1.14.0",
    "ch.megard"                           %% "akka-http-cors"       % "0.2.1",
    "org.scalatest"                       %% "scalatest"            % "3.0.1"   % "test",
    "com.gu"                              %% "scanamo"              % "0.9.2",
    "com.github.t3hnar"                   %% "scala-bcrypt"         % "3.0",
    "com.pauldijou"                       %% "jwt-circe"            % "0.12.1",
    "ch.lightshed"                        %% "courier"              % "0.1.4",
    "com.github.cb372"                    %% "scalacache-caffeine"  % "0.9.3",
    "org.jvnet.mock-javamail"              % "mock-javamail"        % "1.9"     % "test",
    "ch.qos.logback"                       % "logback-classic"      % "1.2.3",
    "org.codehaus.groovy"                  % "groovy"               % "2.4.10"
  )
}
