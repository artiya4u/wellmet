# WellMet - Social Distance Monitor

## Features
- Track close contact with another user via Bluetooth LE iBeacon technology.
- Alert when another user getting to close to keep the distance.
- Dashboard to summary met history number and social distance level.
- View detail of user close contact history e.g. time and place.
- Using an offline database but can export for further analysis and cannot trace back an individual more than a day.

## Security Note
- WelMet's `USER_CODE` (256bits) generates from the user's phone number hashing with a random big number.
- Beacon UUID generated from `USER_CODE` hashing with the date of the advertise beacon to generate beacon UUID of the day and periodically change daily.
- If a user is infected. The authority can request data from the WellMet app on the infected user phone and create a database (or service) for other users to lookup.
- A user can search user close history from the database using their `USER_CODE` hashing with the dates of selection to get their beacon UUIDs and see if it in close contact with the infected user's database.
- Cannot track users back from the beacon UUIDs for more than a day to provide some level of privacy but still can produce a useful statistic number for the user.
- WellMet is a completely offline app. No internet connection needed, No SMS confirmation.