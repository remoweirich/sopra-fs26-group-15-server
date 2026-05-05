package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import ch.guessbb.sopraserver.entity.*;
import ch.guessbb.sopraserver.rest.dto.*;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;


@Mapper
public interface DTOMapper {

	DTOMapper INSTANCE = Mappers.getMapper(DTOMapper.class);

    @Mapping(target = "userProfile.username", source = "username")
    @Mapping(target = "userProfile.email", source = "email")
    @Mapping(target = "userProfile.password", source = "password")
    @Mapping(target = "userProfile.userBio", source = "userBio")
    @Mapping(target = "userScoreboard", ignore = true)
    @Mapping(target = "creationDate", ignore = true)
    @Mapping(target = "token", ignore = true)
    @Mapping(target = "isOnline", ignore = true)
    @Mapping(target = "userId", ignore = true)
    User convertRegisterPostDTOtoUser(RegisterPostDTO registerPostDTO);

    UserAuthDTO convertUsertoUserAuthDTO(User user);

    @Mapping(source = "userProfile.username", target = "username")
    @Mapping(source = "userProfile.userBio", target = "userBio")
    UserDTO convertUserToUserDTO(User user);

    @Mapping(source = "userProfile.username", target = "username")
    @Mapping(source = "userProfile.email", target = "email")
    @Mapping(source = "userProfile.userBio", target = "userBio")
    @Mapping(source = "userScoreboard.totalPoints", target = "userScoreboard.totalPoints")
    @Mapping(source = "userScoreboard.playedGames", target = "userScoreboard.playedGames")
    @Mapping(source = "userScoreboard.playedRounds", target = "userScoreboard.playedRounds")
    @Mapping(source = "userScoreboard.bestRoundPoints", target = "userScoreboard.bestRoundPoints")
    @Mapping(source = "userScoreboard.guessingPrecision", target = "userScoreboard.guessingPrecision")
    @Mapping(target = "friends", ignore = true)
    MyUserDTO convertUserToMyUserDTO(User user);

    @Mapping(source = "lobbyId", target = "lobbyId")
    @Mapping(source = "lobbyName", target = "lobbyName")
    @Mapping(source = "maxPlayers", target = "maxPlayers")
    @Mapping(source = "visibility", target = "visibility")
    @Mapping(source = "maxRounds", target = "maxRounds")
    @Mapping(source = "lobbyState", target = "lobbyState")
    @Mapping(source = "lobbyCode", target = "lobbyCode")
    @Mapping(target = "currentPlayers", ignore = true)
    LobbyDTO convertEntityToLobbyDTO(Lobby lobby);

    @Mapping(source = "admin.userId", target = "adminId")
    @Mapping(source = "players", target = "players")
    @Mapping(target = "currentPlayers", ignore = true)
    MyLobbyDTO convertEntityToMyLobbyDTO(Lobby lobby);









}
