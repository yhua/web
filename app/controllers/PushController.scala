package controllers

import com.baidu.yun.channel.auth.ChannelKeyPair
import com.baidu.yun.channel.client.BaiduChannelClient
import com.baidu.yun.channel.model._
import com.baidu.yun.core.log.{YunLogEvent, YunLogHandler}
import com.baidu.yun.channel.exception.{ChannelServerException, ChannelClientException}
import play.Logger
import play.api.mvc._
import play.api.libs.json.{JsError, Json}
import models.json_models.CheckingMessage
import scala.Predef._
import models._
import play.api.Play
import models.json_models.CheckInfo
import scala.Some
import models.json_models.CheckNotification
import models.JsonResponse

object PushController extends Controller {
  implicit val write = Json.writes[CheckNotification]
  implicit val write3 = Json.writes[ErrorResponse]
  implicit val write4 = Json.writes[SuccessResponse]
  implicit val read = Json.reads[CheckInfo]

  def test = Action {
    val msg = new CheckNotification(System.currentTimeMillis, 1, "1_93740362_374", "925387477040814447", "", "袋鼠", 3)
    DailyLog.create(msg, CheckInfo(93740362L, "0001234569", 2, 0, "", System.currentTimeMillis))
    Ok(runWithLog(msg, triggerSinglePush))
  }

  type Trigger[A] = (A) => ChannelResponse

  def testGroup(newsId: Option[Long]) = Action {
    News.findById(93740362L, newsId.getOrElse(2L)) match {
      case Some(n: News) =>
        Ok(Json.toJson(runWithLog(n, triggerTagPush)))
      case None =>
        Ok(Json.toJson(new ErrorResponse("No such news")))
    }

  }

  def runWithLog[A](msg: A, trigger: Trigger[A]) = {
    try {
      trigger(msg)
      Json.toJson(new SuccessResponse)
    }
    catch {
      case e: ChannelClientException => {
        Json.toJson(new ErrorResponse(e.printStackTrace.toString))
      }
      case e: ChannelServerException => {
        val error = "request_id: %d, error_code: %d, error_message: %s".format(e.getRequestId, e.getErrorCode, e.getErrorMsg)
        Logger.info(error)
        Json.toJson(new ErrorResponse(error))
      }
    }
  }


  def deployStatus: Integer = {
    // DeployStatus => 1: Developer 2: Production
    Play.current.configuration.getString("ios.deployment") match {
      case Some("prod") => new Integer(2)
      case _ => new Integer(1)
    }
  }

  def messageType(device: Int): Integer = {
    // ios is using notification => 1
    // android is using message => 0
    device match {
      case 3 => new Integer(0)
      case 4 => new Integer(1)
    }
  }


  def triggerSinglePush(check: CheckNotification) = {
    val channelClient = getClient
    val request: PushUnicastMessageRequest = new PushUnicastMessageRequest
    //device_type => 1: web 2: pc 3:android 4:ios 5:wp
    request.setDeviceType(new Integer(check.device))
    request.setDeployStatus(deployStatus)
    request.setUserId(check.pushid)
    request.setMessageType(messageType(check.device))
    Logger.info(Json.toJson(check).toString)
    request.setMessage(Json.toJson(check).toString)
    request.setMsgKey(check.timestamp.toString)
    val response: PushUnicastMessageResponse = channelClient.pushUnicastMessage(request)
    Logger.info("push amount : " + response.getSuccessAmount)
    response
  }

  def triggerTagPush(news: News) = {
    val channelClient = getClient
    val request: PushTagMessageRequest = new PushTagMessageRequest
    request.setDeviceType(3)
    request.setTagName(news.school_id.toString)
    request.setMessageType(0)
    request.setMessage("{\"notice_title\":\"" + news.title + "\",\"notice_body\":\"" + news.content + "\", \"notice_type\":2, \"timestamp\": " + System.currentTimeMillis + ", \"publisher\":\"老师\"}")
    request.setMsgKey(System.currentTimeMillis.toString)
    request.setMessageExpires(7 * 86400)
    val response: PushTagMessageResponse = channelClient.pushTagMessage(request)
    Logger.info("push amount : " + response.getSuccessAmount)
    response
  }

  def getClient = {

    val apiKey = Play.current.configuration.getString("push.ak").getOrElse("")
    val secretKey = Play.current.configuration.getString("push.sk").getOrElse("")

    val pair: ChannelKeyPair = new ChannelKeyPair(apiKey, secretKey)

    val channelClient: BaiduChannelClient = new BaiduChannelClient(pair)

    channelClient.setChannelLogHandler(new YunLogHandler {
      def onHandle(event: YunLogEvent) {
        Logger.info(event.getMessage)
      }
    })
    channelClient
  }

  def createSwipeMessage(check: CheckNotification) = {
    runWithLog(check, triggerSinglePush)
  }

  def forwardSwipe(kg: Long) = Action(parse.json) {
    request =>
      Logger.info("checking : " + request.body)
      request.body.validate[CheckInfo].map {
        case (check) =>
          val messages = CheckingMessage.convert(check)
          Logger.info("messages : " + messages)
          messages map {
            m =>
              DailyLog.create(m, check)
              createSwipeMessage(m)
          }
          Ok(Json.toJson(new SuccessResponse))
      }.recoverTotal {
        e => BadRequest("Detected error:" + JsError.toFlatJson(e))
      }
  }
}
