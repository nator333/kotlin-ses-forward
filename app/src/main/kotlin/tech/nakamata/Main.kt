package tech.nakamata


import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.S3Event
import com.amazonaws.services.lambda.runtime.logging.LogLevel
import org.apache.commons.mail2.javax.util.MimeMessageParser
import org.jsoup.Jsoup
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.sesv2.model.EmailContent
import software.amazon.awssdk.services.sesv2.model.RawMessage
import tech.nakamata.models.Config
import tech.nakamata.models.Config.Companion.DEFAULT_MAIL_FROM_INDEX
import tech.nakamata.sevices.S3Service
import tech.nakamata.sevices.SesService
import java.io.ByteArrayOutputStream
import java.util.*
import javax.activation.DataHandler
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

class Main : RequestHandler<S3Event, String> {
  private val config = Config()
  private lateinit var s3Service: S3Service
  private lateinit var sesService: SesService
  private lateinit var logger: LambdaLogger

  enum class Result {
    OK, ERROR
  }

  override fun handleRequest(s3event: S3Event, context: Context): String {
    logger = context.logger
    s3Service = S3Service(config.region(), logger)
    sesService = SesService(config.region(), logger)
    if (s3event.records == null || s3event.records.isEmpty()) {
      logger.log("Event records empty")
      return Result.OK.name
    }
  
    try {
      val s3ObjectsInByte = s3Service.getS3ObjectsInByte(s3event)
      s3ObjectsInByte.forEach(::processEachObject)
    } catch(e: Exception) {
      logger.log("Exception : " + e.message, LogLevel.ERROR)
      return Result.ERROR.name
    }
    
    return Result.OK.name
  }

  private fun processEachObject(s3Object: ByteArray) {
    // Parse raw email and get the subject, body and attachments.
    val emailMessage = MimeMessage(null, s3Object.inputStream())
    val parser = MimeMessageParser(emailMessage)
    parser.parse()

    val s3ObjContent = when {
      parser.hasHtmlContent() -> Jsoup.parse(parser.htmlContent).outerHtml()
      parser.hasPlainContent() -> parser.plainContent
      else -> throw IllegalArgumentException("Invalid content.")
    }

    // Extract the original destination email address
    val originalDestination = extractOriginalDestination(emailMessage)
    logger.log("Original destination: $originalDestination")
    
    // Get the appropriate mailFrom address based on the original destination
    val selectedMailFrom = if (originalDestination != null) {
      config.getMailFromForDestination(originalDestination)
    } else {
      config.mailFrom[DEFAULT_MAIL_FROM_INDEX]
    }
    logger.log("Selected mailFrom: $selectedMailFrom")

    val toAddresses = config.mailTo.map { InternetAddress(it) }.toTypedArray()
    val mimeMessage = createMimeMessage(
      parser,
      toAddresses,
      s3ObjContent,
      selectedMailFrom
    )

    sendEmail(mimeMessage)
    logger.log("Email forwarded successfully.")
  }
  
  /**
   * Extract the original destination email address from the MimeMessage.
   * Returns the first TO recipient, or null if none is found.
   */
  private fun extractOriginalDestination(emailMessage: MimeMessage): String? {
    try {
      val recipients = emailMessage.getRecipients(javax.mail.Message.RecipientType.TO)
      if (recipients != null && recipients.isNotEmpty()) {
        return (recipients[0] as? InternetAddress)?.address
      }
    } catch (e: Exception) {
      logger.log("Error extracting original destination: ${e.message}", LogLevel.WARN)
    }
    return null
  }

  private fun createMimeMessage(
    mimeMessageParser: MimeMessageParser,
    toAddresses: Array<InternetAddress>,
    s3ObjContent: String,
    selectedMailFrom: String = config.mailFrom[DEFAULT_MAIL_FROM_INDEX]): MimeMessage {

    //Add a MIME part to the message
    val multipart = MimeMultipart().apply {
      //Create message part
      val part = MimeBodyPart()
      // For Japanese characters
      part.setContent(s3ObjContent, "text/html; charset=ISO-2022-JP")
      addBodyPart(part)
      //Add attachments
      mimeMessageParser.attachmentList.forEach { attachmentDs ->
        MimeBodyPart().apply {
          this.dataHandler = DataHandler(attachmentDs)
          this.fileName = attachmentDs.name
          addBodyPart(this)
        }
      }
    }

    val mailSession = Session.getDefaultInstance(Properties())
    // Create an email message and set To, From, Subject, Body & Attachment to it.
    return MimeMessage(mailSession).apply {
      // Set the From address based on the selected mailFrom
      setFrom(InternetAddress(selectedMailFrom))
      replyTo = InternetAddress.parse(mimeMessageParser.from)
      setRecipients(javax.mail.Message.RecipientType.TO, toAddresses)
      setSubject(config.subjectPrefix + mimeMessageParser.subject, Charsets.UTF_8.name())
      setContent(multipart, "text/html;")
    }
  }

  //Send email with Amazon SES client
  private fun sendEmail(mimeMessage: MimeMessage) {
    // Write the raw email content to stream
    val byteArray = ByteArrayOutputStream().apply {
      mimeMessage.writeTo(this)
    }.toByteArray()

    val sdkBytes = SdkBytes.fromByteArray(byteArray)
    val rawMessage = RawMessage.builder().data(sdkBytes).build()

    val emailContent = EmailContent.builder()
      .raw(rawMessage)
      .build()
    sesService.sendEmail(emailContent)
  }
}
