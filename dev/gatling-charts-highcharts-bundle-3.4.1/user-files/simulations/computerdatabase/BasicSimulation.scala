/*
 * Copyright 2011-2020 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package computerdatabase

import scala.concurrent.duration._

import scala.util.Random

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class BasicSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8078") // Here is the root for all relative URLs
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
    .exec(repeat(20) {
      exec(
        http("request")
          .get("/${group}/item-12${sid}id")
      )
      .pause(500 milliseconds, 1 seconds)
    })
  setUp(
    scn.inject(
      rampUsersPerSec(100) to(1000) during(3 minutes)
    ).protocols(httpProtocol)
  )
}
