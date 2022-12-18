package com.example.diplom;

import com.example.diplom.model.entity.File;
import com.example.diplom.model.entity.FileInfo;
import com.example.diplom.model.entity.User;
import com.example.diplom.model.login.LoginRequest;
import com.example.diplom.repository.FileRepository;
import com.example.diplom.repository.UserRepository;
import com.example.diplom.service.CloudService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public class ServiceTest {

    public static final String anyToken = ". .";
    private UserRepository userRepository;
    private FileRepository fileRepository;
    private AuthenticationManager authenticationManager;
    private User user;
    private File file;
    private CloudService cloudService;

    @BeforeEach
    public void before() {
        this.userRepository = Mockito.mock(UserRepository.class);
        this.fileRepository = Mockito.mock(FileRepository.class);
        this.authenticationManager = Mockito.mock(AuthenticationManager.class);
        this.user = Mockito.mock(User.class);
        this.file = Mockito.mock(File.class);
        this.cloudService = new CloudService(userRepository, fileRepository, authenticationManager);
        Mockito.when(userRepository.findByEmail(Mockito.any())).thenReturn(user);
        Mockito.when(userRepository.findByAuthToken(Mockito.any())).thenReturn(user);
        Mockito.when(fileRepository.findByFileInfoFilenameAndUser(Mockito.anyString(), Mockito.any())).thenReturn(file);
        Mockito.when(fileRepository.findByUser(Mockito.any())).thenReturn(List.of(file));
    }

    @Test
    public void loginTest() {
        var auth = Mockito.mock(Authentication.class);
        Mockito.when(authenticationManager.authenticate(Mockito.any())).thenReturn(auth);
        try (MockedStatic<SecurityContextHolder> mock = Mockito.mockStatic(SecurityContextHolder.class)) {
            mock.when(SecurityContextHolder::getContext).thenReturn(Mockito.mock(SecurityContext.class));
        }
        Mockito.when(auth.getPrincipal()).thenReturn(Mockito.mock(UserDetails.class));
        cloudService.login(Mockito.mock(LoginRequest.class));
        Mockito.verify(userRepository, Mockito.times(1)).findByEmail(Mockito.any());
        Mockito.verify(user, Mockito.times(1)).createAuthToken();
        Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any());
    }

    @Test
    public void logoutTest() {
        cloudService.logout(anyToken);
        Mockito.verify(userRepository, Mockito.times(1)).findByAuthToken(Mockito.any());
        Mockito.verify(user, Mockito.times(1)).setAuthToken(null);
        Mockito.verify(userRepository, Mockito.times(1)).save(user);
    }

    @Test
    public void uploadTest() throws IOException {
        cloudService.upload(anyToken, Mockito.mock(MultipartFile.class), "");
        Mockito.verify(userRepository, Mockito.times(1)).findByAuthToken(Mockito.anyString());
        Mockito.verify(fileRepository, Mockito.times(1)).save(Mockito.any());
    }

    @Test
    public void deleteTest() {
        cloudService.delete(anyToken, "");
        Mockito.verify(userRepository, Mockito.times(1)).findByAuthToken(Mockito.anyString());
        Mockito.verify(fileRepository, Mockito.times(1)).findByFileInfoFilenameAndUser(Mockito.anyString(), Mockito.any());
        Mockito.verify(fileRepository, Mockito.times(1)).delete(file);
    }

    @Test
    public void getTest() {
        cloudService.get(anyToken, "");
        Mockito.verify(userRepository, Mockito.times(1)).findByAuthToken(Mockito.anyString());
        Mockito.verify(fileRepository, Mockito.times(1)).findByFileInfoFilenameAndUser(Mockito.anyString(), Mockito.any());
        Mockito.verify(file, Mockito.times(1)).getFile();
    }

    @Test
    public void editNameTest() {
        var fileInfo = Mockito.mock(FileInfo.class);
        Mockito.doCallRealMethod().when(fileInfo).setFilename(Mockito.anyString());
        Mockito.when(fileInfo.getFilename()).thenCallRealMethod();
        Mockito.when(file.getFileInfo()).thenReturn(fileInfo);
        cloudService.editName(anyToken, ".", "");
        Mockito.verify(userRepository, Mockito.times(1)).findByAuthToken(Mockito.anyString());
        Mockito.verify(fileRepository, Mockito.times(1)).findByFileInfoFilenameAndUser(Mockito.anyString(), Mockito.any());
        Mockito.verify(fileRepository, Mockito.times(1)).delete(Mockito.any());
        Mockito.verify(file, Mockito.times(1)).getFileInfo();
        Mockito.verify(fileInfo, Mockito.times(1)).setFilename(Mockito.anyString());
        Assertions.assertEquals(".", fileInfo.getFilename());
        Mockito.verify(fileRepository, Mockito.times(1)).save(Mockito.any());
    }

    @Test
    public void listTest() {
        var fileInfo = Mockito.mock(FileInfo.class);
        Mockito.when(file.getFileInfo()).thenReturn(fileInfo);
        var result = cloudService.list(anyToken, 1);
        Mockito.verify(userRepository, Mockito.times(1)).findByAuthToken(Mockito.anyString());
        Mockito.verify(fileRepository, Mockito.times(1)).findByUser(Mockito.any());
        Assertions.assertEquals(List.of(fileInfo), result);
    }

    @Test
    public void emptyListTest() {
        var fileInfo = Mockito.mock(FileInfo.class);
        Mockito.when(file.getFileInfo()).thenReturn(fileInfo);
        var result = cloudService.list(anyToken, 0);
        Assertions.assertTrue(result.isEmpty());
    }
}
