# 1.0.0 (2026-04-03)


### Bug Fixes

* add Google Cloud project ID ([6f4e0c6](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/6f4e0c6fd07dd5d7ab151c9db8d517bebbe8c2c3))
* added config for Bean ObjectMapper ([72c9512](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/72c9512aa8b54e4172d120a5574e7314df755e02))
* added config for Bean ObjectMapper ([26fd60e](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/26fd60e9cafa8bfae2ef9014f70b44d808231142))
* change lobbyid starting from 1 instead of 0 ([0d05f2b](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/0d05f2ba82908d1985b96252e2e0642c46c5a97b))
* Fixed compile errors: many name, signature or type mismatches. Some tests still fail but application is runnable in current state ([62e7c10](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/62e7c1054d327c57ade6a71c7e54fd6efbbb9047))
* **Game:** fix beans and minor details in GameController and GameService ([5b6b9fe](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/5b6b9fe546c3b2e6d6d6b43628d884498a84b01c))
* **Game:** Websocket to same folder, change websocket messages ([06ea85f](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/06ea85f0917637e20b6f46d6b38efbe44ebc910a))
* Make gradlew executable before build step ([cd7c6b2](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/cd7c6b20bb267980e540fd3a8fa6e244ee91aebd))
* merge issues possibly resolved & ([7d2dc64](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/7d2dc6496d8d55e02cdb8c55f8fa4c6ae3b50be1))
* merge issues possibly resolved TrainPositionFetcherTest ([f62b791](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/f62b79191f4b739acf99e30b13d1bd841acdfb65))
* remove semantic release dependency on tests and remove vibe coded app.yml gtfs api inclusion (unused) ([8071a40](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/8071a40608aa805acf4e2567f4d035f07d0c020b))
* rename and error checks ([d6f15c8](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/d6f15c843d91d28ebda9c4b7d7ff2f557d7854b2))
* setupGame after merge from main-preview ([a354ac5](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/a354ac5b9e816a6591b8a9f013f6cfe9c0f0b224))
* **trains, service, rest:** Fixed type, method declaration and variable name mismatches, added a test implementation for the gtfs train service ([6ed421e](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/6ed421eacb10df5a2644d4e4358edb4f7194cd77))
* update gitignore and commit package-lock.json ([cc3843f](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/cc3843ff01a61e9b499c932e0f0874736a96fd90))


### Features

* add automatic versioning and changelog ([76a29b5](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/76a29b5432adc32814d1bede416609a58e640961))
* add GameController Logic ([c3788fa](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/c3788fa96d76815edc8e080adefe2e8ad78bcada))
* add GameController Logic ([a63a3d6](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/a63a3d6dfc2792ac9836e95e2a44bf33acfebd39))
* **auth:** add AuthService, AuthHeader, GET /users/{userId} endpoint ([6e21223](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/6e21223f1de4a63fe6d8905217a08350d0e9c430))
* **auth:** implement full user auth flow with GET profile and logout ([22c2c23](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/22c2c234b660a0d8dbae967bc8784349a2a2dc58))
* **auth:** implement login endpoint ([#21](https://github.com/remoweirich/sopra-fs26-group-15-server/issues/21)) ([04fb6a6](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/04fb6a6ae2ca54eaca44ab8e07af6b8d6e01fe69))
* **auth:** implement login, and fix register ([#17](https://github.com/remoweirich/sopra-fs26-group-15-server/issues/17), [#21](https://github.com/remoweirich/sopra-fs26-group-15-server/issues/21)) ([1933e01](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/1933e010cc3e7b7bf3e21a5380621e6ad9d350ab))
* class framework ([09b1615](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/09b1615446e981a5e87b0af87b612b8fe1025437))
* class framework and new files ([36336f2](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/36336f2297fac82722b2d7cc2150ede319c200e4))
* Created lobby  websocket endpoint so that admin can start the game ([83696d7](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/83696d72af6e9f731b62a46492c18bf402484c93))
* **entity:** add ManyToMany friends, embedded scoreboard, UserRegistration ([5cc8211](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/5cc821132bdce4117a9ed34a44ab68711e99d20d))
* Integrate TrainPositionFetcher into GameService ([cf24862](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/cf248622066c41e248283d5f1d932aa3f3c636ea))
* interpolation of current train position from LineString ([1af79b3](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/1af79b33d71eae591de134b537e0818410545b08))
* **lobbyService:** implement lobbycreation for registered and guest users ([c3c1af7](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/c3c1af7921734e20d6b297cb6c1e94ffa6f6c5e5))
* **lobbyService:** implemented lobbycreation for registered and guest users ([1c32a1e](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/1c32a1eda5d0f51b97269a38da218bd2a395cb91))
* setupGames before id to long change ([2f052ca](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/2f052ca2c246fc3d06d421c1a2486f50eb91330b))
* start on integrating trainfetcher into application ([83b277a](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/83b277a8af7b22dc5672d6702246023c9f49b836))
* updateUserGameStatus implemented ([d4720bd](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/d4720bd90e6fbe41a9f37abf8b1c6b7505dac167))
* **users:** implement GET /users/{userId} with 3 access levels ([f21bfd2](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/f21bfd2ff65f2dd8c509fe4b662adca1672f8eec))
* **users:** implement update user endpoint ([4f50726](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/4f507261cbc2eb022904a3dc5aac1c53025658dd))
