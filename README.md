# Weather manager

A simple REST API that is meant to manage a set of weather related data for a user. 

## Architecture

#### Authentication
This application makes use of JWT tokens to perform authentication. JWT tokens must be included in the `Authorization` 
header field (with value `Bearer <token>`) when trying to access protected endpoints. The HS256 algorithm for decoding 
and verifying JWT tokens which relies on secret text being shared.

Reset code via email is made possible by Courier. We rely on an existing SMTP compatible mail server (like GMail) to 
send out emails to users.

#### Application
The application uses Akka HTTP to serve HTTP requests and DynamoDB as the backing store. It also interacts with 
OpenWeather in order to source weather information. ScalaCache + Caffeine is also used to cache OpenWeather queries for
30 minutes to minimize latency and bandwidth.

It uses 3 DynamoDB tables: 
- Member: responsible for member authentication (passwords are bcrypted and then stored)
- Forecast: OpenWeather forecast weather ID data is stored for each user
- Password Reset: Table that is used to facilitate password reset functionality

### Table Creation

If you are using [local DynamoDB](http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.html), hop on to the shell 
(eg. if Local DynamoDB runs on port 8000, visit http://localhost:8000/shell) and execute the following commands to 
configure the tables:

Table to store forecast data
```javascript
var params = {
    TableName: 'forecast',
    KeySchema: [ // The type of of schema.  Must start with a HASH type, with an optional second RANGE.
        { // Required HASH type attribute
            AttributeName: 'username',
            KeyType: 'HASH',
        },
        { // Optional RANGE key type for HASH + RANGE tables
            AttributeName: 'id', 
            KeyType: 'RANGE', 
        }
    ],
    AttributeDefinitions: [ // The names and types of all primary and index key attributes only
        {
            AttributeName: 'username',
            AttributeType: 'S', // (S | N | B) for string, number, binary
        },
        {
            AttributeName: 'id',
            AttributeType: 'N', // (S | N | B) for string, number, binary
        }
    ],
    ProvisionedThroughput: { // required provisioned throughput for the table
        ReadCapacityUnits: 1, 
        WriteCapacityUnits: 1, 
    }
};
dynamodb.createTable(params, function(err, data) {
    if (err) ppJson(err); // an error occurred
    else ppJson(data); // successful response
});
```

Table to store member data
```javascript
var params = {
    TableName: 'forecast-members',
    KeySchema: [ // The type of of schema.  Must start with a HASH type, with an optional second RANGE.
        { // Required HASH type attribute
            AttributeName: 'email',
            KeyType: 'HASH',
        }
    ],
    AttributeDefinitions: [ // The names and types of all primary and index key attributes only
        {
            AttributeName: 'email',
            AttributeType: 'S', // (S | N | B) for string, number, binary
        }
    ],
    ProvisionedThroughput: { // required provisioned throughput for the table
        ReadCapacityUnits: 1, 
        WriteCapacityUnits: 1, 
    }
};
dynamodb.createTable(params, function(err, data) {
    if (err) ppJson(err); // an error occurred
    else ppJson(data); // successful response
});
```

Table to store password reset data
```javascript
var params = {
    TableName: 'forecast-password-reset',
    KeySchema: [ // The type of of schema.  Must start with a HASH type, with an optional second RANGE.
        { // Required HASH type attribute
            AttributeName: 'resetCode',
            KeyType: 'HASH',
        }
    ],
    AttributeDefinitions: [ // The names and types of all primary and index key attributes only
        {
            AttributeName: 'resetCode',
            AttributeType: 'S', // (S | N | B) for string, number, binary
        }
    ],
    ProvisionedThroughput: { // required provisioned throughput for the table
        ReadCapacityUnits: 1, 
        WriteCapacityUnits: 1, 
    }
};
dynamodb.createTable(params, function(err, data) {
    if (err) ppJson(err); // an error occurred
    else ppJson(data); // successful response
});
```

## Running the application
The easiest way to run the application is using `sbt run`, if you want to run this against local DynamoDB then run:

```sbtshell
sbt -Dsecrets.jwt-key=examplesecretgoes here -Ddynamodb.aws-access-key-id=dev -Ddynamodb.aws-secret-access-key=dev -Ddynamodb.endpoint=http://localhost:8000 -Demail.sender-email=youremail@gmail.com -Demail.password=yourpassword -Dopenweather.api-key=youropenweatherapikey run
```

You can also use the universal packager which is more geared for production deployment
```sbtshell
sbt clean universal:packageBin 
```

Navigate to `target/universal` and unzip `weather-manager-1.0.zip` and execute `./bin/weather-manager`
