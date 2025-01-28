package tech.nakamata.models


import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import software.amazon.awssdk.regions.Region

@JsonIgnoreProperties(ignoreUnknown=true)
data class Config(
    val mailFrom: String,
    val mailTo: List<String>,
    val regionId: String,
    val subjectPrefix: String) {


    init {
        if (mailFrom.isEmpty() || mailFrom == PLACEHOLDER) {
            throw IllegalArgumentException("Populate the config.json first.")
        }
    }
  
  fun region(): Region {
    return Region.of(this.regionId)
  }

    companion object {
        private const val PLACEHOLDER = "*****"
    }
}