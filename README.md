# Weather manager

A simple REST API that is meant to manage a set of weather related data for a user. 

## Architecture

#### Authentication
This application makes use of JWT tokens to perform authentication. JWT tokens must be included in the `Authorization` 
header field (with value `Bearer <token>`) when trying to access protected endpoints. The HS256 algorithm for decoding 
and verifying JWT tokens which relies on secret text being shared.

#### Application
The application uses Akka HTTP and DynamoDB are the backing store. It also interacts with OpenWeather in order to source
weather information.
