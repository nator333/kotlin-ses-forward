package tech.nakamata.models

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.model.Region

data class Config(
    val mailFrom: String,
    val mailTo: List<String>,
    val regionId: String) {
  
  fun region(): Regions {
    return Regions.fromName(this.regionId)
  }
  
}