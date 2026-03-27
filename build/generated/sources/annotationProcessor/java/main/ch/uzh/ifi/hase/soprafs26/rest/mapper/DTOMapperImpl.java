package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.Score;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyAccessDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyCodePostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.MyLobbyDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.RegisterPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserAuthDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-27T23:11:51+0100",
    comments = "version: 1.5.5.Final, compiler: IncrementalProcessingEnvironment from gradle-language-java-9.2.1.jar, environment: Java 17.0.18 (Ubuntu)"
)
public class DTOMapperImpl implements DTOMapper {

    @Override
    public User convertRegisterPostDTOtoUser(RegisterPostDTO registerPostDTO) {
        if ( registerPostDTO == null ) {
            return null;
        }

        User user = new User();

        user.setUsername( registerPostDTO.getUsername() );
        user.setEmail( registerPostDTO.getEmail() );
        user.setPassword( registerPostDTO.getPassword() );
        user.setUserBio( registerPostDTO.getUserBio() );

        return user;
    }

    @Override
    public UserAuthDTO convertUsertoUserAuthDTO(User user) {
        if ( user == null ) {
            return null;
        }

        UserAuthDTO userAuthDTO = new UserAuthDTO();

        if ( user.getUserId() != null ) {
            userAuthDTO.setUserId( String.valueOf( user.getUserId() ) );
        }
        userAuthDTO.setToken( user.getToken() );

        return userAuthDTO;
    }

    @Override
    public User convertUserPostDTOtoEntity(UserPostDTO userPostDTO) {
        if ( userPostDTO == null ) {
            return null;
        }

        User user = new User();

        user.setUsername( userPostDTO.getUsername() );

        return user;
    }

    @Override
    public UserGetDTO convertEntityToUserGetDTO(User user) {
        if ( user == null ) {
            return null;
        }

        UserGetDTO userGetDTO = new UserGetDTO();

        userGetDTO.setUserId( user.getUserId() );
        userGetDTO.setUsername( user.getUsername() );
        userGetDTO.setStatus( user.getStatus() );

        return userGetDTO;
    }

    @Override
    public LobbyDTO convertEntityToLobbyDTO(Lobby lobby) {
        if ( lobby == null ) {
            return null;
        }

        LobbyDTO lobbyDTO = new LobbyDTO();

        lobbyDTO.setLobbyName( lobby.getLobbyName() );
        lobbyDTO.setSize( lobby.getSize() );
        lobbyDTO.setVisibility( lobby.getVisibility() );
        lobbyDTO.setMaxRounds( lobby.getMaxRounds() );
        lobbyDTO.setLobbyState( lobby.getLobbyState() );
        lobbyDTO.setLobbyCode( lobby.getLobbyCode() );
        lobbyDTO.setLobbyId( lobby.getLobbyId() );

        return lobbyDTO;
    }

    @Override
    public MyLobbyDTO convertEntityToMyLobbyDTO(Lobby lobby) {
        if ( lobby == null ) {
            return null;
        }

        MyLobbyDTO myLobbyDTO = new MyLobbyDTO();

        myLobbyDTO.setLobbyId( lobby.getLobbyId() );
        myLobbyDTO.setLobbyCode( lobby.getLobbyCode() );
        myLobbyDTO.setLobbyName( lobby.getLobbyName() );
        myLobbyDTO.setAdmin( lobby.getAdmin() );
        myLobbyDTO.setSize( lobby.getSize() );
        myLobbyDTO.setVisibility( lobby.getVisibility() );
        List<User> list = lobby.getUsers();
        if ( list != null ) {
            myLobbyDTO.setUsers( new ArrayList<User>( list ) );
        }
        myLobbyDTO.setCurrentRound( lobby.getCurrentRound() );
        myLobbyDTO.setMaxRounds( lobby.getMaxRounds() );
        List<Score> list1 = lobby.getScores();
        if ( list1 != null ) {
            myLobbyDTO.setScores( new ArrayList<Score>( list1 ) );
        }
        myLobbyDTO.setLobbyState( lobby.getLobbyState() );

        return myLobbyDTO;
    }

    @Override
    public LobbyAccessDTO convertEntityToLobbyAccessDTO(Lobby lobby) {
        if ( lobby == null ) {
            return null;
        }

        LobbyAccessDTO lobbyAccessDTO = new LobbyAccessDTO();

        lobbyAccessDTO.setLobbyId( lobby.getLobbyId() );
        lobbyAccessDTO.setLobbyCode( lobby.getLobbyCode() );

        return lobbyAccessDTO;
    }

    @Override
    public Lobby convertLobbyCodePostDTOtoEntity(LobbyCodePostDTO lobbyCodePostDTO) {
        if ( lobbyCodePostDTO == null ) {
            return null;
        }

        Lobby lobby = new Lobby();

        lobby.setLobbyCode( lobbyCodePostDTO.getLobbyCode() );

        return lobby;
    }
}
