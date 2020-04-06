# WellMet - Social Distance Monitor

## Features
- Track meet with another user via Bluetooth LE iBeacon technology.
- Alert when another user getting to close to keep distance.
- Dashboard to summary meet history and social distance level.
- View detail of user meet history e.g. time and place.
- Using offline database but can export for future analysis and all user cannot trace back individual user id more than a day.

## Security Note
- Beacon UUID generate from user's phone number hashing with timestamp of user creation called `WellKey`.
WelMet use `WellKey` to hash with date of the advertise beacon to generate beacon UUID of the day and periodically change daily.
- If a user infected, the authority can request data from WellMet app on the user phone and create a database (or service) for people to lookup.
Other users can lookup by using their `WellKey` hashing with dates to lookup if they UUID ever get close to the infected.
This way we can achieve data anonymity. No users id leak to 3rd parties even to the authority.
- WellMet is a completely offline app. No internet connection needed.