package tech.nakamata.sevices

import com.amazonaws.regions.Regions
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder
import com.amazonaws.services.simpleemail.model.RawMessage
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest
import org.apache.logging.log4j.LogManager

class SesService(region: Regions) {
  private val sesClient = AmazonSimpleEmailServiceClientBuilder.defaultClient()
  
  fun sendEmail(rowMessage: RawMessage) {
    this.sesClient.sendRawEmail(SendRawEmailRequest().withRawMessage(rowMessage))
  }
  
  companion object {
    private val LOG = LogManager.getLogger(SesService::class.java)
  }
}