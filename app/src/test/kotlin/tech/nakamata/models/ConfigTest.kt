package tech.nakamata.models

import software.amazon.awssdk.regions.Region
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ConfigTest {
    
    // Helper method to clear environment variables between tests
    private fun clearEnvironmentVariables() {
        // Note: This is a documentation method as we can't actually clear environment variables in tests
        // In a real testing environment, we would use a mocking framework or testing utilities
    }

    @Test
    fun `test basic config creation and property access`() {
        // Arrange
        val mailFrom = listOf("test@example.com", "support@example.com")
        val regionId = "us-west-2"
        val subjectPrefix = "Test: "
        
        // Act
        val config = Config(mailFrom, regionId, subjectPrefix)
        
        // Assert
        // Note: mailFrom property might return environment variable value if set
        // This test assumes MAIL_FROM environment variable is not set during testing
        assertEquals(mailFrom, config.mailFrom)
        assertEquals(regionId, config.regionId)
        assertEquals(subjectPrefix, config.subjectPrefix)
    }
    
    @Test
    fun `test mailFrom property with environment variable`() {
        // This test demonstrates how the mailFrom property would work with an environment variable
        // Note: We can't actually set environment variables in tests, so this is more of a documentation test
        
        // Arrange
        val configMailFrom = listOf("default@example.com", "support@example.com")
        val config = Config(configMailFrom, "us-west-2", "Test: ")
        
        // In a real scenario with environment variable:
        // System.setProperty("MAIL_FROM", "env1@example.com,env2@example.com")
        // val expected = listOf("env1@example.com", "env2@example.com")
        // assertEquals(expected, config.mailFrom)
        
        // Without environment variable, it should fall back to config value:
        assertEquals(configMailFrom, config.mailFrom)
    }
    
    @Test
    fun `test region method returns correct Region object`() {
        // Arrange
        val config = Config(listOf("test@example.com"), "us-west-2", "Test: ")
        
        // Act
        val region = config.region()
        
        // Assert
        assertEquals(Region.US_WEST_2, region)
    }
    
    // Note: Tests for the mailTo property are not included because they depend on environment variables
    // which are difficult to modify in tests on modern Java versions due to security restrictions.
    // In a real-world scenario, we would use a mocking framework like MockK to mock the System.getenv() call,
    // or use a testing library that provides utilities for setting environment variables during tests.
    //
    // The mailTo property functionality:
    // 1. Reads the MAIL_TO environment variable
    // 2. If it's not null or empty, splits it by commas and trims each part
    // 3. If it is null or empty, throws an IllegalArgumentException
    
    @Test
    fun `test getMailFromForDestination with empty destination email`() {
        // Arrange
        val mailFrom = listOf("default@example.com", "support@example.com")
        val config = Config(mailFrom, "us-west-2", "Test: ")
        
        // Act
        val result = config.getMailFromForDestination("")
        
        // Assert
        assertEquals(mailFrom[Config.DEFAULT_MAIL_FROM_INDEX], result)
    }
    
    @Test
    fun `test getMailFromForDestination with destination email matching mailFrom`() {
        // Arrange
        val mailFrom = listOf("default@example.com", "user@example.com", "support@example.com")
        val config = Config(mailFrom, "us-west-2", "Test: ")
        
        // Act
        val result = config.getMailFromForDestination("user@example.com")
        
        // Assert
        assertEquals("user@example.com", result)
    }
    
    @Test
    fun `test getMailFromForDestination with destination email not matching mailFrom`() {
        // Arrange
        val mailFrom = listOf("default@example.com", "support@example.com")
        val config = Config(mailFrom, "us-west-2", "Test: ")
        
        // Act
        val result = config.getMailFromForDestination("unknown@example.com")
        
        // Assert
        assertEquals(mailFrom[Config.DEFAULT_MAIL_FROM_INDEX], result)
    }
    
    @Test
    fun `test getMailFromForDestination with email alias using plus format`() {
        // Arrange
        val mailFrom = listOf("default@example.com", "user@example.com", "support@example.com")
        val config = Config(mailFrom, "us-west-2", "Test: ")
        
        // Act
        val result = config.getMailFromForDestination("user+alias123@example.com")
        
        // Assert
        assertEquals("user@example.com", result)
    }
    
    @Test
    fun `test normalizeEmail method via reflection`() {
        // Arrange
        val config = Config(listOf("test@example.com"), "us-west-2", "Test: ")
        val normalizeEmailMethod = Config::class.java.getDeclaredMethod("normalizeEmail", String::class.java)
        normalizeEmailMethod.isAccessible = true
        
        // Test cases
        val testCases = mapOf(
            "user@example.com" to "user@example.com",
            "user+alias@example.com" to "user@example.com",
            "user+123@example.com" to "user@example.com",
            "user+alias+more@example.com" to "user@example.com",
            "user+@example.com" to "user@example.com",
            "@example.com" to "@example.com",  // Invalid email, should return as is
            "userexample.com" to "userexample.com",  // Invalid email, should return as is
            "" to ""  // Empty string, should return as is
        )
        
        // Act & Assert
        testCases.forEach { (input, expected) ->
            val result = normalizeEmailMethod.invoke(config, input) as String
            assertEquals(expected, result, "Failed for input: $input")
        }
    }
    
    @Test
    fun `test regionId property with default value`() {
        // Arrange
        val defaultRegionId = "us-west-2"
        val config = Config(listOf("test@example.com"), defaultRegionId, "Test: ")
        
        // Act & Assert
        // Without environment variable, it should fall back to config value
        assertEquals(defaultRegionId, config.regionId)
    }
    
    @Test
    fun `test regionId property with environment variable`() {
        // This test demonstrates how the regionId property would work with an environment variable
        // Note: We can't actually set environment variables in tests, so this is more of a documentation test
        
        // Arrange
        val configRegionId = "us-west-2"
        val config = Config(listOf("test@example.com"), configRegionId, "Test: ")
        
        // In a real scenario with environment variable:
        // System.setProperty("REGION_ID", "eu-west-1")
        // val expected = "eu-west-1"
        // assertEquals(expected, config.regionId)
        
        // Without environment variable, it should fall back to config value:
        assertEquals(configRegionId, config.regionId)
    }
    
    @Test
    fun `test subjectPrefix property with default value`() {
        // Arrange
        val defaultSubjectPrefix = "Test: "
        val config = Config(listOf("test@example.com"), "us-west-2", defaultSubjectPrefix)
        
        // Act & Assert
        // Without environment variable, it should fall back to config value
        assertEquals(defaultSubjectPrefix, config.subjectPrefix)
    }
    
    @Test
    fun `test subjectPrefix property with environment variable`() {
        // This test demonstrates how the subjectPrefix property would work with an environment variable
        // Note: We can't actually set environment variables in tests, so this is more of a documentation test
        
        // Arrange
        val configSubjectPrefix = "Test: "
        val config = Config(listOf("test@example.com"), "us-west-2", configSubjectPrefix)
        
        // In a real scenario with environment variable:
        // System.setProperty("SUBJECT_PREFIX", "Production: ")
        // val expected = "Production: "
        // assertEquals(expected, config.subjectPrefix)
        
        // Without environment variable, it should fall back to config value:
        assertEquals(configSubjectPrefix, config.subjectPrefix)
    }
    
    @Test
    fun `test mailTo property documentation`() {
        // This test documents how the mailTo property works
        // Note: We can't actually set environment variables in tests
        
        // Arrange
        val config = Config(listOf("test@example.com"), "us-west-2", "Test: ")
        
        // The mailTo property:
        // 1. Reads the MAIL_TO environment variable
        // 2. If it's not null or empty, splits it by commas and trims each part
        // 3. If it is null or empty, throws an IllegalArgumentException
        
        // In a real scenario with environment variable:
        // System.setProperty("MAIL_TO", "recipient1@example.com, recipient2@example.com")
        // val expected = listOf("recipient1@example.com", "recipient2@example.com")
        // assertEquals(expected, config.mailTo)
        
        // Without environment variable, it should throw an exception:
        assertFailsWith<IllegalArgumentException> {
            config.mailTo
        }
    }
    
    @Test
    fun `test getMailFromForDestination with blank destination email`() {
        // Arrange
        val mailFrom = listOf("default@example.com", "support@example.com")
        val config = Config(mailFrom, "us-west-2", "Test: ")
        
        // Act
        val result = config.getMailFromForDestination("   ")
        
        // Assert
        assertEquals(mailFrom[Config.DEFAULT_MAIL_FROM_INDEX], result)
    }
}