# auth-utils

[![build](https://img.shields.io/circleci/project/choffmeister/auth-utils.svg)](https://circleci.com/gh/choffmeister/auth-utils)
[![maven Central](https://img.shields.io/maven-central/v/de.choffmeister/auth-common.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22de.choffmeister%22%20AND%20a%3A%22auth-common%22)
[![maven Central](https://img.shields.io/maven-central/v/de.choffmeister/auth-spray.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22de.choffmeister%22%20AND%20a%3A%22auth-spray%22)
[![license](http://img.shields.io/badge/license-MIT-lightgrey.svg)](http://opensource.org/licenses/MIT)

## Usage

Add the following lines to you `build.sbt` file:

~~~ scala
// build.sbt
libraryDependencies += "de.choffmeister" %% "auth-common" % "0.1.0"

libraryDependencies += "de.choffmeister" %% "auth-akka-http" % "0.1.0"
~~~

Here is an example, that uses HTTP basic authentication as well as JWT authentication:

~~~ scala
// UsageExample.scala
import java.util.Date

import akka.actor._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import de.choffmeister.auth.akkahttp.Authenticator
import de.choffmeister.auth.common.{JsonWebToken, OAuth2AccessTokenResponse, OAuth2AccessTokenResponseFormat}
import spray.json.JsString

import scala.concurrent._
import scala.concurrent.duration._

case class User(id: Int, userName: String)
object UserDatabase {
  private val users = User(1, "user1") :: User(2, "user2") :: Nil
  private val passwords = Map(1 -> "pass1", 2 -> "pass2")

  def findById(id: String)(implicit ec: ExecutionContext) =
    Future(users.find(_.id.toString == id))
  def findByUserName(userName: String)(implicit ec: ExecutionContext) =
    Future(users.find(_.userName == userName))
  def validatePassword(user: User, password: String)(implicit ec: ExecutionContext) =
    Future(passwords(user.id) == password)
}

class UsageExample(implicit val system: ActorSystem, val executor: ExecutionContext, val materializer: Materializer) {
  val bearerTokenSecret = "secret-no-one-knows".getBytes
  val bearerTokenLifetime = 5.minutes

  val authenticator = new Authenticator[User](
    realm = "Example realm",
    bearerTokenSecret = bearerTokenSecret,
    findUserById = UserDatabase.findById,
    findUserByUserName = UserDatabase.findByUserName,
    validateUserPassword = UserDatabase.validatePassword)

  val route =
    pathPrefix("token" / "create") {
      get {
        // here we can send valid username/password via HTTP basic authentication and get a JWT for it
        authenticator.basic()(user => completeWithToken(user))
      }
    } ~
    path("token" / "renew") {
      get {
        // here we can send an expired JWT via HTTP bearer authentication and get a renewed JWT for it
        authenticator.bearerToken(acceptExpired = true)(user => completeWithToken(user))
      }
    } ~
    path("state") {
      get {
        // here we get greeted, if we have either a valid username/password or JWT sent via HTTP basic resp. HTTP bearer
        authenticator()(user => complete(s"Welcome, ${user.userName}!"))
      }
    }

  private def completeWithToken(user: User): Route = {
    val secret = bearerTokenSecret
    val lifetime = bearerTokenLifetime.toSeconds
    val now = System.currentTimeMillis / 1000L * 1000L

    val token = JsonWebToken(
      createdAt = new Date(now),
      expiresAt = new Date(now + lifetime * 1000L),
      subject = user.id.toString,
      claims = Map("name" -> JsString(user.userName))
    )

    val response = OAuth2AccessTokenResponse("bearer", JsonWebToken.write(token, secret), lifetime)
    complete(OAuth2AccessTokenResponseFormat.write(response).compactPrint)
  }
}
~~~

## License

Published under the permissive [MIT](http://opensource.org/licenses/MIT) license.
