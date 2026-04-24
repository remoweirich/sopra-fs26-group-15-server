# [1.3.0](https://github.com/remoweirich/sopra-fs26-group-15-server/compare/v1.2.0...v1.3.0) (2026-04-24)


### Bug Fixes

* **DTOMapper:** MyLobbyDTO sanitizes users to UserDTO ([29badf9](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/29badf9ddc7513c07ab9ff74b5cf4ce46fc716ea))
* **DTOMapper:** remove UserPostDTO from template ([6d1ff10](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/6d1ff10fb459c2c2e32b8236df74358938499b50))
* **GameResult entity persistence:** fix Round getters and setters to work with Jackson ([bcb46e5](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/bcb46e5a7c9657aa4c9e907df4bdf23526be3679))
* **GameService:** ScoresPublished is now gameSpecific, gameTeardown now exists. ([3043bba](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/3043bba09de292dfb8157a92759876a3ba3fb4ce))
* **JoinLobby:** CreateGuestUser ([a932987](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/a932987086dc4b94117dc4bd87d1b192099a3d85))
* **JoinLobby:** CreateGuestUser ([1a82204](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/1a822049e62492f5f2eebeb28f4ec0e823fef059))
* **LobbyController:** changed ")" to "}" ([9e554b5](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/9e554b5e24f7d3057ef450ee984f66c20c9ceb71))
* **lobby:** enable rejoin ([c92b096](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/c92b096a221e4f655a8c5aa6d4b69fde134bb7d0))
* **lobbyService,lobbyRESTController:** Removing id and token from DTO and put it in header ([284314a](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/284314acef3cb2eb0337101667de94220d083ba4))
* **LobbyWebSocketController:** debug messages & Empty constructor added for objectMapper ([0445d17](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/0445d17906c7f1dba4d8f759a27f4b4e58134cf0))
* **LobbyWebSocketController:** UserAuth and AdminAuth hold back GAME_START message (decision to be made about messageBody) ([ccd7eff](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/ccd7eff90fcc2d641d33e3af95978b66eecbfa0a))
* **processGuess:** save guess as km rounded. ([a2e728e](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/a2e728ea4638fcbb715623741c1b6a23e9ae9e10))
* removed test mode, added userGetDTO ([6743c4b](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/6743c4bdc0da6bcc101ae4881b0c3444a3a8c4ac))
* **Scoring Logic:** Change how guesses are scored ([92564fd](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/92564fdc3246850e126222e81abd4e72899e998f))
* **Scoring Logic:** Include an absolute distance dampener, so both absolute and relative (to start and end station) distances are part of scoring ([8bb2445](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/8bb2445a0fec7dfa2d32674b5e816cc9d5952712))
* **StartGame:** reimplement Auth on startGame message ([7254a53](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/7254a531a319826508f06501ea968775bf869552))
* temp fix ([2847fd6](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/2847fd6e5ab4621b69b305874f34efdab6e674a4))
* timer is now removed from activeTimers correctly, scores are saved and retrieved from updated game instance, train is copied instead of altered for sending incomplete version in Roundstart message, fixed UserGameStatus constructor ([5627ef0](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/5627ef064185eb0a3410ba29cb47fa0164246254))
* **WebSocket:** add AuthService to TopicSubscriptionInterceptor ([edb9784](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/edb9784037f28e848fb7effcb1bfe83cddb8d312))
* **WebSocket:** Handle circular Dependency I created (sorry) ([9f96b45](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/9f96b459115265ba0fb10692107340c0979236ad))
* **WebSocketInterceptor:** fixed logic. I threw exception if user is authenticated (sorry) ([286de1b](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/286de1bb74591ae57d0352787bb6423bb5a09094))


### Features

* Changed Trainpositionfetcher to also include departure and final arrival times at line destintation and origin ([165e150](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/165e150968d5678b6a54f3437325302767fd6382))
* **Game Leaderboard:** Game Leaderboard now exists and shows, player scores and distances to Train ([d6acd26](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/d6acd268622826df711a33affeee821c5f69f8ed))
* **gameTearDown, LobbyTearDown:** Tearing down our games ([f0b648f](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/f0b648fc926f349ff96194d94d63e21b96f47162))
* implement lobby creation logic and websocket infrastructure ([5049e6a](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/5049e6ab92aa6f2a874258a01eef5ac36ed40689))
* Improved robustness of TrainPositionFetcher for use with real API: enrichment of trains is retried if unsuccessful ([4814e11](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/4814e11d3ff847e832024031b79979759a685d72))
* **JoinLobby:** sendMessage GAME_START ([7d6217a](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/7d6217ab461c4a79dae13de84e49a2e2826d7f67))
* **PostGameResult:** Game Results are now persisted. corresponding id with game and lobby. ([250d410](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/250d410626e44d4c030d67af602151e3446ad895))
* **PostGameResult:** Game Results are now persisted. corresponding id with game and lobby. ([e770fbf](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/e770fbf1a934f9aeb8dcbff1d34ebfc89792dcc1))
* Testing TrainPositionFetcher ([4978d36](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/4978d36a8b56988c91fc98eb164c7f253448eca0))
* Testing-Tree initialization ([0c6955c](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/0c6955c7df9e7618d5dad1754e9bf843bf540d85))
* Tests for game-, lobbyrest-, lobbywebsockets- & usercontroller ([daa7e69](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/daa7e69aafbfa5a0cdddc9f9b8366081b6207168))
* **WebSocket:** add WebsocketInterceptors that intercept websocket publishes and subscriptions to check for authorization. Also refactor users list in lobby to be a Map<userId, user> so checking if a user is part of a lobby is easier (: ([35839f0](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/35839f0292b0bd462a646ee6fe19ab591c30b0c3))
* **WebSocket:** implement connect auth through authUser since even guests have a user by then ([2edc539](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/2edc5391f61625c91047e2d57db57ac132f107a9))

# [1.2.0](https://github.com/remoweirich/sopra-fs26-group-15-server/compare/v1.1.0...v1.2.0) (2026-04-14)


### Features

* added websockets endpoint in LobbyWebSocketController that removes User from lobby ([f01f7cb](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/f01f7cb2abd34118a6a280eedb3791b402dd35b6))

# [1.1.0](https://github.com/remoweirich/sopra-fs26-group-15-server/compare/v1.0.0...v1.1.0) (2026-04-13)


### Bug Fixes

* **Game start:** fix broken logic of updateUserGameStatus before any round has started. ([4cc0e8e](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/4cc0e8ec45e8535e10b9f09a2a4d394c54ef5133))
* **GameService:** fix distance never getting added to round and fix logicbug in publishScores ([832c93d](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/832c93dd2d83316a60f4e24b0e24d3f88a6a9bf7))
* **GameService:** resultDTO now always receive/sends a value instead of null ([fff189f](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/fff189fec82bdae86c352433b2b28390a282a05b))
* **Scoring:** update Overall game Score for a lobby ([276d108](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/276d10883eada834bd0ebd2f896ccbeae3920f20))


### Features

* **game scoring:** implemented absolute distance scoring → to be replaced by dynamic system scoring formula ([539d51c](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/539d51c3a1156b11aeb93a8300a88017f838e2d8))
* **GameService:** updateGameStatus & publishScores & more ([b28dfe0](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/b28dfe0b50d57cdbfbca10df10c897897d461b3a))
* publishScores unfinished ([920f556](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/920f55605ab5e830c1372dfc952bcf8e5024cbfc))
* RoundStart method to publish relevant data to client and RoundStartDTO with said data stored created ([03b226c](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/03b226c4c6826ac27451dc6ad4a7fe90b666f3b0))
* timer logic implemented ([0769a54](https://github.com/remoweirich/sopra-fs26-group-15-server/commit/0769a54baa5342cbb206f44870b29a159957735b))

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
