import com.example.auction.bidding.api.BiddingService
import com.example.auction.item.api.ItemService
import com.example.auction.user.api.UserService
import com.lightbend.lagom.internal.client.CircuitBreakerMetricsProviderImpl
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.api.{ ServiceAcl, ServiceInfo, ServiceLocator }
import com.lightbend.lagom.scaladsl.client.LagomServiceClientComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.softwaremill.macwire._
import controllers.{ Assets, AssetsComponents, ItemController, Main, ProfileController }
import play.api.ApplicationLoader.Context
import play.api.i18n.I18nComponents
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.EssentialFilter
import play.api.{ ApplicationLoader, BuiltInComponentsFromContext, Mode }
import router.Routes

import scala.collection.immutable
import scala.concurrent.ExecutionContext

abstract class WebGateway(context: Context) extends BuiltInComponentsFromContext(context)
  with I18nComponents
  with AhcWSComponents
  with LagomServiceClientComponents
  with AssetsComponents {

  override lazy val serviceInfo: ServiceInfo = ServiceInfo(
    "web-gateway",
    Map(
      "web-gateway" -> immutable.Seq(ServiceAcl.forPathRegex("(?!/api/).*"))
    )
  )
  override implicit lazy val executionContext: ExecutionContext = actorSystem.dispatcher
  override lazy val router = {
    val prefix = "/"
    wire[Routes]
  }

  lazy val userService = serviceClient.implement[UserService]
  lazy val itemService = serviceClient.implement[ItemService]
  lazy val biddingService = serviceClient.implement[BiddingService]

  lazy val main = wire[Main]
  lazy val itemController = wire[ItemController]
  lazy val profileController = wire[ProfileController]
  lazy val myAssets = wire[Assets]
}

class WebGatewayLoader extends ApplicationLoader {
  override def load(context: Context) = context.environment.mode match {
    case Mode.Dev =>
      new WebGateway(context) with LagomDevModeComponents {
        override def httpFilters: Seq[EssentialFilter] = Nil
      }.application
    case _ =>
      new WebGateway(context) {
        override lazy val circuitBreakerMetricsProvider = new CircuitBreakerMetricsProviderImpl(actorSystem)

        override def httpFilters: Seq[EssentialFilter] = Nil

        override def serviceLocator: ServiceLocator = NoServiceLocator
      }.application
  }
}
