lazy val root = (project in file("."))
  .settings(name := "online-auction-scala")
  .aggregate(itemApi, itemImpl,
    biddingApi, biddingImpl,
    userApi, userImpl,
    searchApi, searchImpl,
    transactionApi, transactionImpl,
    webGateway)
  .settings(commonSettings: _*)

organization in ThisBuild := "com.example"

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.12.8"

version in ThisBuild := "1.0.0-SNAPSHOT"

val playJsonDerivedCodecs = "org.julienrf" %% "play-json-derived-codecs" % "4.0.0"
val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.1" % "provided"
val scalaTest = "org.scalatest" %% "scalatest" % "3.0.5" % Test
val scalaTestPlusPlay = "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
val mockito = "org.mockito" % "mockito-core" % "2.23.4" % Test

val akkaDiscoveryServiceLocator = "com.lightbend.lagom" %% "lagom-scaladsl-akka-discovery-service-locator" % "1.0.0"


lazy val security = (project in file("security"))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      lagomScaladslServer % Optional,
      playJsonDerivedCodecs,
      scalaTest
    )
  )

lazy val itemApi = (project in file("item-api"))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      playJsonDerivedCodecs
    )
  )
  .dependsOn(security)

lazy val itemImpl = (project in file("item-impl"))
  .settings(commonSettings: _*)
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslTestKit,
      lagomScaladslKafkaBroker,
      akkaDiscoveryServiceLocator,
      "com.datastax.cassandra" % "cassandra-driver-extras" % "3.0.0",
      macwire,
      scalaTest
    )
  )
  .settings(lagomForkedTestSettings: _*)
  .dependsOn(itemApi, biddingApi)

lazy val biddingApi = (project in file("bidding-api"))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      playJsonDerivedCodecs
    )
  )
  .dependsOn(security)

lazy val biddingImpl = (project in file("bidding-impl"))
  .settings(commonSettings: _*)
  .enablePlugins(LagomScala)
  .dependsOn(biddingApi, itemApi)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslTestKit,
      lagomScaladslKafkaBroker,
      akkaDiscoveryServiceLocator,
      macwire,
      scalaTest
    ),
    maxErrors := 10000

  )

lazy val searchApi = (project in file("search-api"))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      playJsonDerivedCodecs
    )
  )
  .dependsOn(security)

lazy val searchImpl = (project in file("search-impl"))
  .settings(commonSettings: _*)
  .enablePlugins(LagomScala)
  .dependsOn(searchApi, itemApi, biddingApi)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslKafkaClient,
      lagomScaladslTestKit,
      akkaDiscoveryServiceLocator,
      macwire,
      scalaTest
    )
  )

lazy val transactionApi = (project in file("transaction-api"))
  .settings(commonSettings: _*)
  .dependsOn(itemApi)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      playJsonDerivedCodecs
    )
  )
  .dependsOn(security)

lazy val transactionImpl = (project in file("transaction-impl"))
  .settings(commonSettings: _*)
  .enablePlugins(LagomScala)
  .dependsOn(transactionApi, itemApi)
  .settings(lagomForkedTestSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslKafkaClient,
      lagomScaladslTestKit,
      akkaDiscoveryServiceLocator,
      macwire,
      scalaTest
    )
  )

lazy val userApi = (project in file("user-api"))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      playJsonDerivedCodecs
    )
  )
  .dependsOn(security)

lazy val userImpl = (project in file("user-impl"))
  .settings(commonSettings: _*)
  .enablePlugins(LagomScala)
  .dependsOn(userApi)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      akkaDiscoveryServiceLocator,
      macwire,
      scalaTest
    )
  )

lazy val webGateway = (project in file("web-gateway"))
  .settings(commonSettings: _*)
  .enablePlugins(PlayScala, LagomPlay)
  .dependsOn(biddingApi, itemApi, searchApi, userApi)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslServer,
      macwire,
      scalaTest,
      scalaTestPlusPlay,
      akkaDiscoveryServiceLocator,
      mockito,

      "org.ocpsoft.prettytime" % "prettytime" % "4.0.2.Final",

      "org.webjars" % "foundation" % "6.2.3",
      "org.webjars" % "foundation-icon-fonts" % "d596a3cfb3"
    )
  )

def evictionSettings: Seq[Setting[_]] = Seq(
  // This avoids a lot of dependency resolution warnings to be showed.
  // They are not required in Lagom since we have a more strict whitelist
  // of which dependencies are allowed. So it should be safe to not have
  // the build logs polluted with evictions warnings.
  evictionWarningOptions in update := EvictionWarningOptions.default
    .withWarnTransitiveEvictions(false)
    .withWarnDirectEvictions(false)
)

def commonSettings: Seq[Setting[_]] = evictionSettings ++ Seq(
  javacOptions in Compile ++= Seq("-encoding", "UTF-8", "-source", "1.8"),
  javacOptions in(Compile, compile) ++= Seq("-Xlint:unchecked", "-Xlint:deprecation", "-parameters", "-Werror"),
  scalacOptions ++= List("-encoding", "utf8", "-feature", "-deprecation", "-unchecked"),

  testOptions in Test ++= Seq(
    // Show the duration of tests
    Tests.Argument(TestFrameworks.ScalaTest, "-oD")
  )
)

lagomCassandraCleanOnStart in ThisBuild := false

// ------------------------------------------------------------------------------------------------

// register 'elastic-search' as an unmanaged service on the service locator so that at 'runAll' our code
// will resolve 'elastic-search' and use it. See also com.example.com.ElasticSearch
lagomUnmanagedServices in ThisBuild += ("elastic-search" -> "http://127.0.0.1:9200")
