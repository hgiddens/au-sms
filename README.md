## A Scala client for sending SMS to Australian numbers

[![Build Status](https://api.travis-ci.org/hgiddens/au-sms.svg)](https://travis-ci.org/hgiddens/au-sms)

Supports:

* [The Telstra SMS API](https://dev.telstra.com/content/sms-api-0)
* [SMS Central REST API](http://www.smscentral.com.au/sms-api/rest-api/)

---

### Telstra SMS API

Add the following to your `build.sbt`:

    resolvers += Resolver.bintrayRepo("hgiddens", "maven")
    libraryDependencies ++= Seq(
      "com.github.hgiddens" %% "au-sms-telstra" % <version>,
      "org.http4s" %% "http4s-blaze-client" % "0.10.1
    )

You can then send SMS as follows:

    import com.github.hgiddens.ausms._
    import com.github.hgiddens.ausms.telstra._
    import org.http4s.client.blaze.defaultClient
    
    val sms = TelstraSmsClient(defaultClient, "your client id", "your client secret")
    val send = sms.sendMessage(PhoneNumber("04xxxxxxxx"), Message("Hello, world!"))
    println(s"Message sent with id of ${id.run}")

### SMS Central

Add the following to your `build.sbt`:

    resolvers += Resolver.bintrayRepo("hgiddens", "maven")
    libraryDependencies ++= Seq(
      "com.github.hgiddens" %% "au-sms-smscentral" % <version>,
      "org.http4s" %% "http4s-blaze-client" % "0.10.1
    )

You can then send SMS as follows:

    import com.github.hgiddens.ausms._
    import com.github.hgiddens.ausms.smscentral._
    import org.http4s.client.blaze.defaultClient
    
    val config = SmsCentralClient.Config("your username", "your password", "shared").get
    val sms = SmsCentralClient(defaultClient, config)
    val send = sms.sendMessage(PhoneNumber("04xxxxxxxx"), Message("Hello, world!"))
    println(s"Message sent with id of ${id.run}")
