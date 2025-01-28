package tech.nakamata.sevices


import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.events.S3Event
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest

class S3Service(
  region: Region,
  private val logger: LambdaLogger
  ) {
  private val s3Client = S3Client.builder().region(region).build()
  
  fun getS3ObjectsInByte(event: S3Event): List<ByteArray> {
    val resultList = mutableListOf<ByteArray>()
    event.records.forEach {
      val s3Entity = it.s3
      val bucket = s3Entity.bucket.name
      val key = s3Entity.`object`.key

      val request = GetObjectRequest.builder().bucket(bucket).key(key).build()

      resultList.add(s3Client.getObject(request).readAllBytes())
    }
    logger.log("S3: S3Object downloaded records: ${resultList.size}")
    
    return resultList
  }
}