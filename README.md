# kotlin-ses-forward

## Outline 

- Forward an email from a S3 bucket receiving from SES, to another email address by SES

![GitHub Logo](/docs/cloudcraft.png)

## Tools

- Gradle
- Serverless Framework
- AWS SES
- AWS Lambda

## Configulation

```
{
  "mailFrom": "******", <- needs to be verified in SES console 
  "mailTo": [
    "******"
  ],
  "regionId": "******", <- needs to match Lambda deploy environment
  "deploymentBucket": "******", <- existing S3 bucket name
  "eventBucket": "******" <- will be created by cloud formation
}
```
