package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import ch.uzh.ifi.hase.soprafs26.objects.Lobby;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.rest.dto.*;

/**
 * DTOMapper
 * This class is responsible for generating classes that will automatically
 * transform/map the internal representation
 * of an entity (e.g., the User) to the external/API representation (e.g.,
 * UserGetDTO for getting, UserPostDTO for creating)
 * and vice versa.
 * Additional mappers can be defined for new entities.
 * Always created one mapper for getting information (GET) and one mapper for
 * creating information (POST).
 */
@Mapper
public interface DTOMapper {

	DTOMapper INSTANCE = Mappers.getMapper(DTOMapper.class);

	User convertRegisterPostDTOtoUser(RegisterPostDTO registerPostDTO);

	UserAuthDTO convertUsertoUserAuthDTO(User user);

	@Mapping(source = "username", target = "username")
	User convertUserPostDTOtoEntity(UserPostDTO userPostDTO);


    @Mapping(source = "userScoreboard", target = "userScoreboard")
	@Mapping(source = "username", target = "username")
	@Mapping(source = "email", target = "email")
	@Mapping(source = "userBio", target = "userBio")
	@Mapping(source = "creationDate", target = "creationDate")
	@Mapping(source = "friends", target = "friends")
	MyUserDTO convertUserToMyUserDTO(User user);

	@Mapping(source = "userScoreboard", target = "userScoreboard")
	@Mapping(source = "username", target = "username")
	@Mapping(source = "userBio", target = "userBio")
	@Mapping(source = "creationDate", target = "creationDate")
	@Mapping(source = "friends", target = "friends")
	UserDTO convertUserToUserDTO(User user);

	@Mapping(source = "userId", target = "userId")
	@Mapping(source = "username", target = "username")
	@Mapping(source = "status", target = "status")
	UserGetDTO convertEntityToUserGetDTO(User user);

	@Mapping(source = "lobbyName", target = "lobbyName")
	@Mapping(source = "size", target = "size")
	@Mapping(source = "visibility", target = "visibility")
	@Mapping(source = "maxRounds", target = "maxRounds")
	@Mapping(source = "lobbyState", target = "lobbyState")
	@Mapping(source = "lobbyCode", target = "lobbyCode")
	@Mapping(source = "lobbyId", target = "lobbyId")

	LobbyDTO convertEntityToLobbyDTO(Lobby lobby);

	@Mapping(source = "lobbyId", target = "lobbyId")
	@Mapping(source = "lobbyCode", target = "lobbyCode")
	@Mapping(source = "lobbyName", target = "lobbyName")
	@Mapping(source = "admin", target = "admin")
	@Mapping(source = "size", target = "size")
	@Mapping(source = "visibility", target = "visibility")
	@Mapping(source = "currentRound", target = "currentRound")
	@Mapping(source = "users", target = "users")
	@Mapping(source = "maxRounds", target = "maxRounds")
	@Mapping(source = "scores", target = "scores")
	@Mapping(source = "lobbyState", target = "lobbyState")

	MyLobbyDTO convertEntityToMyLobbyDTO(Lobby lobby);

	@Mapping(source = "lobbyId", target = "lobbyId")
	@Mapping(source = "lobbyCode", target = "lobbyCode")
	LobbyAccessDTO convertEntityToLobbyAccessDTO(Lobby lobby);

	@Mapping(source = "lobbyCode", target = "lobbyCode")

	Lobby convertLobbyCodePostDTOtoEntity(LobbyCodePostDTO lobbyCodePostDTO);

}
