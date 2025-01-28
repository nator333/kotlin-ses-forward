package tech.nakamata.sevices


import com.amazonaws.services.lambda.runtime.LambdaLogger
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sesv2.SesV2Client
import software.amazon.awssdk.services.sesv2.model.EmailContent
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest


class SesService(region: Region, private val logger: LambdaLogger) {
  private val sesClient = SesV2Client
    .builder()
    .region(region)
    .build()
  
  fun sendEmail(
    emailContent: EmailContent
  ) {
    val request = SendEmailRequest.builder()
      .content(emailContent)
      .build()

    this.sesClient.sendEmail(request)
    logger.log("Sent an email successfully.")
  }
}