# GuesSBB

## Introduction
This project is the server backend for a Swiss-train based game.
For many Swiss commuters, long train rides are part of their daily routine. This can feel repetitive and tedious, so we wanted to create a fun way to engage with that experience.
It brings together Switzerland’s train enthusiasts and geography enthusiasts alike, allowing them to explore Switzerland together.
Finally, the project should also show our genuine appreciation for the Swiss transport network.

## Technologies used
- **Java 21** (Gradle toolchain)
- **Spring Boot** (Web, WebSocket, Data JPA, H2 console)
- **H2 in-memory database** (default for local development)
- **Gradle build system** (Spring Boot plugin)
- **GTFS / reactor-netty** for train position fetching
- **Dockerfile** for containerized runs

## High-level components
1) **Application entrypoint**
   - [`Application.java`](src/main/java/ch/uzh/ifi/hase/soprafs26/Application.java) Spring Boot main class and application bootstrap.
2) **REST & WebSocket API**
   - REST controllers (game/lobby/user): [`GameRESTController.java`](src/main/java/ch/uzh/ifi/hase/soprafs26/controller/GameRESTController.java), [`LobbyRESTController.java`](src/main/java/ch/uzh/ifi/hase/soprafs26/controller/LobbyRESTController.java), [`UserController.java`](src/main/java/ch/uzh/ifi/hase/soprafs26/controller/UserController.java) provide HTTP endpoints for client interactions.
   - WebSocket controller: [`LobbyWebSocketController.java`](src/main/java/ch/uzh/ifi/hase/soprafs26/controller/LobbyWebSocketController.java) handles real-time game messages.
3) **Services & Business Logic**
   - [`GameService.java`](src/main/java/ch/uzh/ifi/hase/soprafs26/service/GameService.java) game lifecycle and scoring.
   - [`LobbyService.java`](src/main/java/ch/uzh/ifi/hase/soprafs26/service/LobbyService.java) lobby creation/joining and state management.
   - [`UserService.java`](src/main/java/ch/uzh/ifi/hase/soprafs26/service/UserService.java) user lifecycle and persistence.
4) **Persistence / Repositories**
   - JPA entities in [`src/main/java/ch/uzh/ifi/hase/soprafs26/entity/`](src/main/java/ch/uzh/ifi/hase/soprafs26/entity) and Spring Data repositories such as [`LobbyRepository.java`](src/main/java/ch/uzh/ifi/hase/soprafs26/repository/LobbyRepository.java) and [`UserRepository.java`](src/main/java/ch/uzh/ifi/hase/soprafs26/repository/UserRepository.java).
5) **Train position & external data**
   - [`TrainPositionFetcher.java`](src/main/java/ch/uzh/ifi/hase/soprafs26/trains/TrainPositionFetcher.java) code that fetches train/GTFS information (configurable via properties).

How these components relate
- The `Application` starts the Spring context and exposes HTTP / WebSocket endpoints. Controllers forward requests to Services, which coordinate domain entities and persist via Repositories. The [`TrainPositionFetcher`](src/main/java/ch/uzh/ifi/hase/soprafs26/trains/TrainPositionFetcher.java) is used by the game/lobby services to provide the location and map data needed for rounds. WebSocket messages are configured through [`WebSocketConfig.java`](src/main/java/ch/uzh/ifi/hase/soprafs26/websocket/WebSocketConfig.java) and are meant to be intercepted/secured by [`WebSocketAuthInterceptor.java`](src/main/java/ch/uzh/ifi/hase/soprafs26/websocket/WebSocketAuthInterceptor.java) in a future release.

## Launch & Deployment

### Prerequisites
- Java 21 (the Gradle toolchain will try to use it, but having it installed locally is recommended)

### Environment variables (optional)
- `GEOPS_API_KEY` (optional) API key for geops service used to fetch maps (if unset the service can run in mock mode [`geops.mock=true`])
- `BROKER_HOST`, `BROKER_USER`, `BROKER_PASS` RabbitMQ broker settings if you enable our message broker features.

### Run locally (development)
1) Install dependencies and build (from project root):

```bash
./gradlew build
```

2) Run the application directly with Gradle (recommended for development):

```bash
./gradlew bootRun
```


3) The server listens on port 8080 by default. The H2 console is available at http://localhost:8080/h2-console/ (credentials are in [`application.properties`](src/main/resources/application.properties)).

### Run tests

```bash
./gradlew test
```



### External dependencies
- By default the project uses an in-memory H2 database (see [`application.properties`](src/main/resources/application.properties)). 
- RabbitMQ is referenced via broker properties (only needed if you enable broker features).

### Releases
- This repository contains a [`package.json`](package.json) with semantic-release tooling (used by CI) to automatically create releases when configured.

## Roadmap and future improvements

1. **Add game history:** expose per-user game history with statistics, average distance, and per-round overviews.
2. **Improve matchmaking and lobby discovery:** add friend invites, filters for public/private lobbies, player count, and game state.
3. **Train retrieval:** Improve [`TrainPositionFetcher.java`](src/main/java/ch/uzh/ifi/hase/soprafs26/trains/TrainPositionFetcher.java) with better caching, rate-limiting and robust fallback to mock data. This would make the server resilient to external GTFS outages.

## Authors & Acknowledgments
This project was created by:
- **Claude Stark** [@ClaudeStark](https://github.com/ClaudeStark)
- **Remo Weirich** [@remoweirich](https://www.github.com/remoweirich)
- **Michael Jankovic** [@T-N-T-O](https://github.com/T-N-T-O)
- **Dorian Rother** [@dorianrother](https://github.com/dorianrother)
- **Shadi Vandeventer** [@snowjademusic](https://www.github.com/snowjademusic)

Special thanks geOps for providing unlimited credits for train fetching during our development phase and to the fly on the wall for listening to our ramblings.

## License
- This project is licensed under GNU AGPLv3. See [`LICENSE`](LICENSE) for the full text.
- Commercial licensing information is available in [`COMMERCIAL.md`](COMMERCIAL.md).
- Contribution terms are described in [`CLA.md`](CLA.md) and [`CONTRIBUTING.md`](CONTRIBUTING.md).
