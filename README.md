# Google Keep API access

The Google Keep API (see overview [here](https://developers.google.com/keep/api/guides)) is available to "enterprise" customers only, meaning that if you pay for Google Workspace (to f.e. use GMail with your own personal DNS domain name) you'll have different features/limitations than someone using Keep with a plain `@gmail.com` account.

## If you are a `@gmail.com` user

Making the Google Keep API accessible to custom scripts/programs requires the workaround described in the `keep-it-markdown`'s project [README](https://github.com/djsudduth/keep-it-markdown/blob/main/README.md).

* When logged into GMail from the browser, extract a particular cookie
* Process the cookie to get an OAuth token

The issue here is that this token can do anything your `@gmail.com` user (or your Android phone) can do to your Google account, it's got full access.

## If you pay for Google Workspace

I initially tried to follow the official [Java quickstart guide](https://developers.google.com/keep/api/guides/java) for Google Keep but the code there doesn't work (or I wasn't able to make it work). I can't even tell if getting a Service Account authorized by a human (two-legged OAuth) is supported at all.

The [Using OAuth 2.0 for Server to Server Applications](https://developers.google.com/identity/protocols/oauth2/service-account) doc contains an example for the Google Cloud SQL Admin API that also works for Google Keep.

These are the prerequisite steps (just summarizing them here, do follow the linked docs when setting up):

* Enable the Google Keep API (as per the [Java quickstart guide](https://developers.google.com/keep/api/guides/java))
* Create a Service Account for your script/program to use
* Create a key for the account (this step will give you a `.json` file with the required keymatter and metadata)
* Delegate domain-wide authority to the Service Account. This will effectively grant the Service Account full access to all the users in the Google Workspace organization. **But**: Service Accounts (Client IDs) can be restricted to specific OAuth scopes (I'm using `https://www.googleapis.com/auth/keep.readonly`), which might be preferable than the the previous method.

This repo contains example code to list all the Google Keep notes for an impersonated user (`USER_TO_IMPERSONATE`) in the Google Workspace organization.

## How to run the code

* Bring your own `src/main/resources/credentials.json`. This file gets downloaded when creating a key for the Service Account.
* Set `USER_TO_IMPERSONATE`.
* Run `gradle run`.

## Troubleshooting

When emitting a `401` during authorization, the Google OAuth endpoint puts some useful troubleshooting information in the response body. Annoyingly though, the standard Java HTTP client (which is the [recommended](https://cloud.google.com/java/docs/reference/google-http-client/latest/com.google.api.client.http.HttpTransport) transport) closes the connection before consuming that data. This seems to happen [here](https://github.com/openjdk/jdk/blob/5b05f8e0c459d879b302728ce89c2012d198faec/src/java.base/share/classes/sun/net/www/protocol/http/HttpURLConnection.java#L1796), deep inside `sun.net.www.protocol.http.HttpURLConnection`.

The Google API client is nice enough to output `curl` commands (when logging is enabled, like in this example) that can be used to replay requests and see the full responses.

Alternatively you can toggle `USE_APACHE_HTTP_TRANSPORT`. This causes the OAuth stuff to use the Apache HTTP transport, which doesn't gobble up a 401 response body.

In my case I was using the wrong scope (`KeepScopes.KEEP` instead of `KeepScopes.KEEP_READONLY`, which is what I've granted my Service Account). With manual `curl` replay or `USE_APACHE_HTTP_TRANSPORT` I was able to see this:

```json
{
  "error": "unauthorized_client",
  "error_description": "Client is unauthorized to retrieve access tokens using this method, or client not authorized for any of the scopes requested."
}
```
