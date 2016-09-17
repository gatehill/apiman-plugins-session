# apiman-plugins-session [![Build Status](https://travis-ci.org/outofcoffee/apiman-plugins-session.svg?branch=master)](https://travis-ci.org/outofcoffee/apiman-plugins-session)

A suite of [apiman](http://apiman.io) plugins providing simple web session management using [cookies](https://tools.ietf.org/html/rfc6265).

## Overview

With these plugins installed in your _apiman_ instance, you can issue, validate and revoke web session cookies.

# Policies

There are three policies:

* Cookie Issue Policy
* Cookie Validate Policy
* Cookie Remove Policy

The policies are described in more detail below. There are many configuration options that allow you to tailor behaviour to your environment.

## Issuing session cookies

Authentication is delegated to a back-end service, which is expected to provide a [JSON Web Token](https://jwt.io) (JWT) in its response, such as:

    HTTP/1.1 200 OK
    Content-Type: application/json
    
    {
      "access_token": "your-jwt-here"
    }

You configure the 'Cookie Issue Policy' to look for this token and issue a session cookie.

You can optionally extract a Claim from the JWT response (for example, 'sub’; the subject), which will be added to the request headers of subsequent incoming requests to your back-end services. This allows your services to know which user made the request. If you don't explicitly choose a Claim to extract, the whole JWT will be passed to your back-end service as a header instead.

> Note: The JWT should be signed using the _HS256_ algorithm, and using the _Signing secret_ set in the plugin configuration.

## Validating session cookies

Once a session cookie has been issued, the gateway remembers the session until it expires.

On receipt of an incoming request, the 'Cookie Validator Policy' looks for the presence of the session cookie, then validates the corresponding session. If:

  * validation fails, an _HTTP 401 Unauthorized_ response is returned to the caller
  * validation succeeds, the request is passed-on to the back-end API, optionally containing the value of the JWT (or
  Claim), extracted during the authentication flow

Expiration of the session means the configured timeout period has elapsed and no requests have been received within this
time.

## Revoking session cookies

The 'Cookie Remove Policy' revokes cookies and optionally invalidates the session corresponding to that cookie's value.

# Building

If you want to compile the policies yourself, use:

    mvn clean install
    
...and see the JAR files under the `target` directories.

Importing into your favourite IDE is easy, as long as it supports Maven projects.

## Tests
If you want to run unit tests, run:

    mvn clean test

# Recent changes

For recent changes see the [Changelog](CHANGELOG.md).

# Contributing

Pull requests are welcome.

# Author

Pete Cornish (outofcoffee@gmail.com)
