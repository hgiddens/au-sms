# A Scala client for the [Telstra SMS API](https://dev.telstra.com/content/sms-api-0)

## Getting started

Add the following to your `build.sbt`:

    resolvers += Resolver.bintrayRepo("hgiddens", "maven")
    libraryDependencies ++= Seq(
      "com.github.hgiddens" %% "telstra-sms-http4s" % <version>,
      "org.http4s" %% "http4s-blaze-client" % "0.10.1
    )

You can then send SMS as follows:

    import com.github.hgiddens.telstrasms._
    import com.github.hgiddens.telstrasms.http4s._
    import org.http4s.client.blaze.defaultClient
    
    val sms = Http4sSmsClient(defaultClient, "your client id", "your client secret")
    val send = for {
      token ← sms.token
      id ← sms.sendMessage(token, PhoneNumber("04xxxxxxxx"), Message("Hello, world!"))
    } yield id
    
    println(s"Message sent with id of ${id.run}")
