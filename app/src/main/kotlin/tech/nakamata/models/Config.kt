package tech.nakamata.models


import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import software.amazon.awssdk.regions.Region

@JsonIgnoreProperties(ignoreUnknown=true)
data class Config(
    private val _mailFrom: List<String> = emptyList(),
    private val _regionId: String = "",
    private val _subjectPrefix: String = "") {
    val subjectPrefix: String
        get() {
            val subjectPrefix = System.getenv("SUBJECT_PREFIX")
            if (!subjectPrefix.isNullOrEmpty()) {
                return subjectPrefix
            } else {
                return _subjectPrefix
            }
        }
    val regionId: String
        get() {
            val regionId = System.getenv("REGION_ID")
            if (!regionId.isNullOrEmpty()) {
                return regionId.trim()
            } else {
                return _regionId
            }
        }
    val mailFrom: List<String>
        get() {
            val envMailFrom = System.getenv("MAIL_FROM")
            if (!envMailFrom.isNullOrEmpty()) {
                return envMailFrom.split(",").map { it.trim() }
            } else {
                return _mailFrom
            }
        }
    
    /**
     * Get the list of recipient email addresses from the MAIL_TO environment variable.
     * 
     * @return The list of recipient email addresses
     * @throws IllegalArgumentException if the MAIL_TO environment variable is not set
     */
    val mailTo: List<String>
        get() {
            val envMailTo = System.getenv("MAIL_TO")
            if (!envMailTo.isNullOrEmpty()) {
                return envMailTo.split(",").map { it.trim() }
            } else {
                throw IllegalArgumentException("MAIL_TO environment variable is not defined or empty. This variable is required for the application to function correctly.")
            }
        }
        
    /**
     * Get the list of recipient email addresses safely for testing purposes.
     * This method doesn't throw exceptions if the environment variable is not set.
     * 
     * @return The list of recipient email addresses or an empty list if not set
     */
    fun getMailToSafe(): List<String> {
        val envMailTo = System.getenv("MAIL_TO")
        return if (!envMailTo.isNullOrEmpty()) {
            envMailTo.split(",").map { it.trim() }
        } else {
            emptyList()
        }
    }
  
    fun region(): Region {
        val regionIdValue = this.regionId
        return if (regionIdValue.isNotBlank()) {
            Region.of(regionIdValue)
        } else {
            // Default to us-east-1 if no region is specified
            Region.US_EAST_1
        }
    }
    
    /**
     * Get the appropriate mailFrom address based on the destination email.
     * Handles email aliases with +xxx format by removing the +xxx part.
     * 
     * @param destinationEmail The original destination email address
     * @return The appropriate mailFrom address
     */
    fun getMailFromForDestination(destinationEmail: String?): String {
        // Handle null or empty destination email
        if (destinationEmail == null || destinationEmail.isBlank()) {
            return getDefaultMailFrom()
        }
        
        // Normalize the email address by removing the +xxx part if present
        val normalizedEmail = normalizeEmail(destinationEmail)

        return if (mailFrom.contains(normalizedEmail)) {
            normalizedEmail
        } else {
            getDefaultMailFrom()
        }
    }
    
    /**
     * Get the default mail from address, safely handling empty lists
     * 
     * @return The default mail from address or a placeholder if none exists
     */
    private fun getDefaultMailFrom(): String {
        return if (mailFrom.isNotEmpty()) {
            mailFrom[DEFAULT_MAIL_FROM_INDEX]
        } else {
            "noreply@example.com" // Default fallback when no mail from addresses are configured
        }
    }
    
    /**
     * Normalize an email address by removing the +xxx part if present.
     * For example, "user+alias@example.com" becomes "user@example.com".
     * 
     * @param email The email address to normalize
     * @return The normalized email address
     */
    private fun normalizeEmail(email: String): String {
        val atIndex = email.lastIndexOf('@')
        if (atIndex <= 0) return email
        
        val plusIndex = email.indexOf('+')
        if (plusIndex <= 0 || plusIndex > atIndex) return email
        
        return email.substring(0, plusIndex) + email.substring(atIndex)
    }

    companion object {
        const val DEFAULT_MAIL_FROM_INDEX = 0
    }
}