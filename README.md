# Weather manager

A REST API that is meant to manage a set of weather related data for a user. 

## Architecture

#### Authentication
This application makes use of JWT tokens to perform authentication. JWT tokens must be included in the `Authorization` 
header field (with value `Bearer <token>`) when trying to access protected endpoints. The HS256 algorithm for decoding 
and verifying JWT tokens which relies on secret text being shared.

Reset code via email is made possible by Courier. We rely on an existing SMTP compatible mail server (like GMail) to 
send out emails to users.

#### Application
The application uses Akka HTTP to serve HTTP requests, Cassandra as the event journal to track member movements such as 
adding and removing of weather data, tracking when a member has signed in or reset their password. For now, it uses
DynamoDB to store authentication and password reset information. It also interacts with OpenWeather in order to source 
weather information. ScalaCache + Caffeine is also used to cache OpenWeather queries for 30 minutes to minimize latency 
and bandwidth. The plan is to eventually move everything over to Cassandra since it is well equipped to handle the 
operational side. 

It uses 2 DynamoDB tables: 
- Member: responsible for member authentication (passwords are bcrypted and then stored)
- Password Reset: Table that is used to facilitate password reset functionality

It uses a Cassandra table as an event journal to track when a member adds or removes weather data and also when a 
member logs in or resets their password. The idea behind doing this is to apply the principle of CQRS (Command Query
Responsibility Segregation) wherein the Command Side is stored in a way that is optimal for the operational side and
Query Side(s) is stored in a way that is optimal for the analytics side. The drawback of doing this is that the query
side is now eventually consistent. Usually the command side is focused on a per-entity basis whereas the query sides are
focused on answering questions that span multiple entities (e.g. How many members have added city with ID=1234 or 
How many members have tried to login on 2017-04-15). Currently, the Persistence Query Example feeds off the raw events 
from the command side but does not feed those events to any read-side databases optimized to answer specific questions.

### DynamoDB Table Creation

If you are using [local DynamoDB](http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.html), 
hop on to the shell (eg. if Local DynamoDB runs on port 8000, visit http://localhost:8000/shell) and execute the 
following commands to configure the tables:

Table to store member data
```javascript
var params = {
    TableName: 'forecast-members',
    KeySchema: [ // The type of of schema.  Must start with a HASH type, with an optional second RANGE.
        { // Required HASH type attribute
            AttributeName: 'email',
            KeyType: 'HASH'
        }
    ],
    AttributeDefinitions: [ // The names and types of all primary and index key attributes only
        {
            AttributeName: 'email',
            AttributeType: 'S' // (S | N | B) for string, number, binary
        }
    ],
    ProvisionedThroughput: { // required provisioned throughput for the table
        ReadCapacityUnits: 1, 
        WriteCapacityUnits: 1 
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
            KeyType: 'HASH'
        }
    ],
    AttributeDefinitions: [ // The names and types of all primary and index key attributes only
        {
            AttributeName: 'resetCode',
            AttributeType: 'S' // (S | N | B) for string, number, binary
        }
    ],
    ProvisionedThroughput: { // required provisioned throughput for the table
        ReadCapacityUnits: 1, 
        WriteCapacityUnits: 1 
    }
};
dynamodb.createTable(params, function(err, data) {
    if (err) ppJson(err); // an error occurred
    else ppJson(data); // successful response
});
```

### Cassandra Table Creation
Akka Persistence Cassandra takes care of creating the tables by default. Be warned that it uses some questionable 
defaults so you definitely do not want to use these defaults in production. You want a Replication Factor greater than 1
along with a read and write consistency of quorum or any other configuration that yields consistent results. 

## Running the application
Execute the universal packager and have it build the application
```sbtshell
sbt clean universal:packageBin 
```

Navigate to `target/universal` and unzip `weather-manager-1.0.zip` and execute `./bin/weather-manager` and ensure the 
following environment variables are present: 

- MEMBER_TABLE: name of table to manage user authentication
- PASSWORD_RESET_TABLE: name of table to handle password resets for users
- SMTP_SERVER: domain of SMTP server (e.g. smtp.gmail.com)
- SMTP_PORT: port of SMTP server
- SENDER_EMAIL: email that is used to send password resets
- SENDER_PASSWORD: password belonging to the email
- JWT_SECRET: the secret used to encrypt JWT messages
- JWT_EXPIRY: the time (in seconds) that the JWT token is valid for

If you want to run this against local DynamoDB then ensure you have these system properties as well:

`-Ddynamodb.aws-access-key-id=dev -Ddynamodb.aws-secret-access-key=dev -Ddynamodb.endpoint=http://localhost:8000`

If you are using a local Cassandra, it is already setup to connect to it. If you are not, then change the contact points
to point to your Cassandra cluster.

If you prefer to use system properties:

For the command side
```bash
./bin/weather-manager -Ddynamodb.aws-access-key-id=dev -Ddynamodb.aws-secret-access-key=dev -Ddynamodb.endpoint=http://localhost:8000 -Demail.sender-email=youremail -Demail.password=yourpassword -Dopenweather.api-key=yourapikey command
```

For the query side
```bash
./bin/weather-manager query
```