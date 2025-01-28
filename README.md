# kotlin-ses-forward

## Outline 

- Forward an email from a S3 bucket receiving from SES, to another email address by SES

![GitHub Logo](/docs/cloudcraft.png)

## Tools

- Gradle (Groovy)
- Kotlin
- Serverless Framework

## Prerequisite

- Deployment bucket has to be created
- Configure your own domain and an email address on SES

## Configuration

```
{
  "mailFrom": "******", <- needs to be verified in SES console 
  "mailTo": [
    "******"
  ],
  "regionId": "******", <- needs to match Lambda deploy environment
  "deploymentBucket": "******", <- existing S3 bucket name
  "eventBucket": "******" <- will be created by cloud formation
  "subjectPrefix": "SES FW: " <- adjust to your liking
}
```


## Note

- After a successful deployment, you'd need to manually enable the `ReceiptRule` on the SES console to start receiving emails.
- If a deployment fails, make sure to 
  - Empty the `EventBucket` and delete manually
  - Inactivate the `ReceiptRuleSet` on `Email receiving` page
  - then, run `serverless remove && serverless deploy`