package tech.nakamata

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.S3Event
import com.amazonaws.services.simpleemail.model.RawMessage
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.commons.mail.util.MimeMessageParser
import org.apache.logging.log4j.LogManager
import org.jsoup.Jsoup
import tech.nakamata.models.Config
import tech.nakamata.sevices.S3Service
import tech.nakamata.sevices.SesService
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*
import javax.activation.DataHandler
import javax.activation.DataSource
import javax.mail.MessagingException
import javax.mail.Session
import javax.mail.internet.AddressException
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart


class S3ReceiveMailEventHandler : RequestHandler<S3Event, String> {
  
  companion object {
    private val LOG = LogManager.getLogger(S3ReceiveMailEventHandler::class.java)
    private const val SUBJECT_PREFIX = "SES FW: "
  }
  
  override fun handleRequest(s3event: S3Event, context: Context): String {
    if (s3event.records == null || s3event.records.isEmpty()) {
      LOG.info("Event records empty")
      return "ok"
    }
  
    try {
      val config = this.retrieveConfig()
      val s3Service = S3Service(config.region())
      val sesService = SesService(config.region())
      val s3objects = s3Service.getS3Objects(s3event)
      s3objects.forEach { s3object ->
        try {
          //Parse raw email and get the subject, body and attachments.
          val emailMessage = MimeMessage(null, s3object.objectContent)
          val parser = MimeMessageParser(emailMessage)
          parser.parse()
      
      
          val content = when {
            parser.hasHtmlContent() -> Jsoup.parse(parser.htmlContent).outerHtml()
            parser.hasPlainContent() -> parser.plainContent
            else -> null
          }
      
          val subject = parser.subject
          val attachments: List<DataSource>? = parser.attachmentList
      
          //Prepare to send email
          val mailSession = Session.getDefaultInstance(Properties())
          val toAddresses = config.mailTo.map { InternetAddress(it) }.toTypedArray()
      
          // Create an email message and set To, From, Subject, Body & Attachment to it.
          val msg = MimeMessage(mailSession).apply {
            setFrom(InternetAddress(config.mailFrom))
            setRecipients(javax.mail.Message.RecipientType.TO, toAddresses)
            this.subject = SUBJECT_PREFIX + subject!!
          }
      
          //Create message part
          val part = MimeBodyPart()
          part.setContent(content!!.toString(), "text/html")
      
          //Add a MIME part to the message
          val mp = MimeMultipart()
          mp.addBodyPart(part)
      
          //Add attachments
          attachments!!.forEach { source ->
            MimeBodyPart().apply {
              this.dataHandler = DataHandler(source)
              this.fileName = source.name
              mp.addBodyPart(this)
            }
          }
      
          msg.setContent(mp)
      
          // Write the raw email content to stream
          val out = ByteArrayOutputStream()
          msg.writeTo(out)
      
          RawMessage().apply {
            data = ByteBuffer.wrap(out.toString().toByteArray())
            //Send email with Amazon SES client
            sesService.sendEmail(this)
          }
      
          LOG.info("Email forwarded successfully.")
        } catch (e: Exception) {
          LOG.error("Exception : " + e.message)
          return "ERROR"
        }
      }
    } catch(e: Exception) {
      LOG.error("Exception : " + e.message)
      return "ERROR"
    }
    
    
    return "OK"
  }
  
  private fun retrieveConfig(): Config {
    try {
      return jacksonObjectMapper().readValue(object {}.javaClass.getResource("/config.json"))
    } catch (e: Exception) {
      throw e
    }
  }
}
