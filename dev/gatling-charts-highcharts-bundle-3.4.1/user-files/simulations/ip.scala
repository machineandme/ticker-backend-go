package simulations

import scala.concurrent.duration._

import scala.util.Random

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class IPSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://167.99.249.87")
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8") // Here are the common headers
    .doNotTrackHeader("1")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

  var groups = Array(
    Map("group" -> "alert"),
    Map("group" -> "heartbeat"),
    Map("group" -> "info"),
  ).random

  var data = Iterator.continually(Map(
    "sid" -> ( Random.alphanumeric.take(1).mkString ),
  ))

  val scn = scenario("Dolbesh")
    .feed(groups)
    .feed(data)
    .exec(repeat(10) {
      exec(
        http("request")
          .get("/${group}/item-12${sid}id")
      )
      .pause(500 milliseconds, 1 seconds)
    })
  setUp(
    scn.inject(
      rampUsersPerSec(1) to(10) during(10 minutes)
    ).protocols(httpProtocol)
  )
}
