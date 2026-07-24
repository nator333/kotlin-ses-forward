# kotlin-ses-forward

## Outline 

- Forward an email from a S3 bucket receiving from SES, to another email address by SES

![GitHub Logo](/docs/cloudcraft.png)

## Why SES + S3 + Lambda?

*Reviewed against AWS features as of July 2026.*

AWS still has no "forward this email to that address" action — not in classic SES
receipt rules, and not in [Mail Manager](https://docs.aws.amazon.com/ses/latest/dg/eb.html).
The [April 2026 Mail Manager release](https://aws.amazon.com/about-aws/whats-new/2026/04/ses-mail-manager-introduces-new-features/)
added optional STARTTLS, mTLS on ingress endpoints, and two new rule actions:
**Invoke Lambda function** and **Bounce**. The newest email-processing feature
adds Lambda invocation rather than removing the need for it.

### Why Mail Manager's SMTP relay is not a substitute

[SMTP relay](https://docs.aws.amazon.com/ses/latest/dg/eb-relay.html) targets an
email *infrastructure you administer* — Google Workspace, Microsoft 365, on-prem
Exchange. Two blockers for forwarding to a personal mailbox:

1. Inbound relay requires enabling an inbound gateway and allow-listing Mail
   Manager IP ranges in the destination's admin console. Not possible on a
   consumer `@gmail.com` account.
2. AWS requires the destination to be an SES-verified identity; unverified
   destinations are not attempted. `gmail.com` cannot be verified.

### Cost

| | This setup (classic receiving) | Mail Manager |
| --- | --- | --- |
| Per email | $0.10 / 1,000 | $0.15 / 1,000 processed |
| Ingress endpoint | n/a | **$50 / month** for an open endpoint |

Receiving from the internet means MX records pointing at an *open* ingress
endpoint — the $50/month tier. The free authenticated endpoints are for SMTP
submission, not inbound. See [SES pricing](https://aws.amazon.com/ses/pricing/).

### Why this is unlikely to change

The constraint is structural, not a missing feature. SES only sends from
verified identities, and relaying a message verbatim breaks SPF and DKIM
alignment at the destination. Something has to rewrite the `From` header while
preserving the original sender in `Reply-To` — which is what
`Config.getMailFromForDestination` and `SesService` do here.

The S3 hop is also not incidental: the SES Lambda action does not provide the
full MIME message, and the SNS action caps out below typical attachment sizes.
S3 is how the raw message is obtained.

Classic SES email receiving carries no deprecation notice as of this review, and
Mail Manager continues to expand (GovCloud, May 2026). Worth re-checking
periodically — absence of an announcement is not a guarantee.

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