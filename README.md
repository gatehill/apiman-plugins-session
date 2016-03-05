# apiman-plugins-session [![Build Status](https://travis-ci.org/outofcoffee/apiman-plugins-session.svg?branch=master)](https://travis-ci.org/outofcoffee/apiman-plugins-session)

A suite of _apiman_ plugins providing simple web session management.

## Overview

With these plugins installed in your _apiman_ instance, you can issue, validate and revoke web session cookies. 

## Issuing session cookies

Authentication is delegated to a back-end service, which is expected to provide a trivially simple response, such as:

    HTTP/1.1 200 OK
    Content-Type: application/json
    
    {
      "authenticatedUser": "some-userId"
    }

You then configure the 'Cookie Issue Policy' to look for this successful response and issue a session cookie.

You can optionally extract a field from the authentication response (for example, 'authenticatedUser', above), which will be added to the request headers of subsequent incoming requests, so that your back-end services know which user made the request.

## Validating session cookies

Once a session cookie has been issued, the gateway remembers the session until it expires.

On receipt of an incoming request, the 'Cookie Validator Policy' looks for the presence of the session cookie, then validates the corresponding session. If:

  * validation fails, an _HTTP 401 Unauthorized_ response is returned to the caller
  * validation succeeds, the request is passed-on to the back-end API, optionally containing the value of the response field extracted during the authentication flow

Expiration of the session means the configured timeout period has elapsed and no requests have been received within this time.

## Revoking session cookies

The 'Cookie Remove Policy' revokes cookies and optionally invalidates the session corresponding to that cookie's value.

# Policies

There are three policies:

* Cookie Issue Policy
* Cookie Validate Policy
* Cookie Remove Policy

# Building

If you want to compile the policies yourself, use:

    mvn clean install
    
...and look under the `target` directories.

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
