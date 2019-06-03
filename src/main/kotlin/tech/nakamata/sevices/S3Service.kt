package tech.nakamata.sevices

import com.amazonaws.regions.Regions
import com.amazonaws.services.lambda.runtime.events.S3Event
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.transfer.TransferManager
import com.amazonaws.services.s3.transfer.TransferManagerBuilder
import com.amazonaws.services.s3.transfer.Upload
import org.apache.logging.log4j.LogManager
import java.io.InputStream

class S3Service(region: Regions) {
  private val s3Client: AmazonS3 = AmazonS3ClientBuilder
      .defaultClient()
  
  fun getS3Objects(event: S3Event): List<S3Object> {
    val resultList = mutableListOf<S3Object>()
    event.records.forEach {
      val s3Entity = it.s3
      val bucket = s3Entity.bucket.name
      val key = s3Entity.`object`.key
      resultList.add(s3Client.getObject(bucket, key))
    }
    LOG.info("S3: S3Object downloaded records: ${resultList.size}")
    
    return resultList
  }
  
  fun transferObject(stream: InputStream, metaData: ObjectMetadata, bucket: String, key: String) {
    val transferManager: TransferManager = TransferManagerBuilder.standard().withS3Client(s3Client).build()
    LOG.debug("S3: Uploading to $bucket, with key: $key")
    val upload: Upload = transferManager.upload(bucket, key, stream, metaData)
    upload.waitForUploadResult()
    LOG.debug("Upload successful")
    stream.close()
    transferManager.shutdownNow(false)
  }
  
  companion object {
    private val LOG = LogManager.getLogger(S3Service::class.java)
  }
}