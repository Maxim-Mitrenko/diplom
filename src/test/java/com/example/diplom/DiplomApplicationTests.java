package com.example.diplom;

import com.example.diplom.model.entity.File;
import com.example.diplom.model.entity.FileInfo;
import com.example.diplom.model.entity.User;
import com.example.diplom.model.login.LoginRequest;
import com.example.diplom.repository.FileRepository;
import com.example.diplom.repository.UserRepository;
import com.example.diplom.security.JwtTokenFilter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class DiplomApplicationTests {

    @Container
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:latest")
            .withExposedPorts(3306)
            .withDatabaseName("cloud")
            .withUsername("root")
            .withPassword("mysql");

    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FileRepository fileRepository;
    @Autowired
    private JwtTokenFilter filter;
    @Autowired
    private WebApplicationContext context;
    private MockMvc mockMvc;

    private static final String fileUrl = "/file";
    private static final String listUrl = "/list";
    private static final String loginUrl = "/login";
    private static final String header = "auth-token";
    private static final String query = "filename";
    private static String token;
    private static final User user = new User("Test", "Test", ".");


    @DynamicPropertySource
    public static void datasourceConfig(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
    }

    @BeforeEach
    public void before() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilter(filter).build();
        if (token == null) {
            token = "Bearer .";
            userRepository.save(user);
        }
    }

    @Test
    void contextLoads() {
    }

    @Test
    public void uploadTest() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "1".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(MockMvcRequestBuilders.multipart(fileUrl)
                .file(file)
                .header(header, token)
                .queryParam(query, file.getOriginalFilename()))
                .andExpect(MockMvcResultMatchers.status().isOk());
        Assertions.assertNotNull(fileRepository.findByFileInfoFilenameAndUser(file.getOriginalFilename(), user));
    }

    @Test
    public void getTest() throws Exception {
        String filename = "file.txt";
        byte[] bytes = "Hello, world!".getBytes(StandardCharsets.UTF_8);
        fileRepository.save(new File(bytes, new FileInfo(filename, 0), user));
        Assertions.assertNotNull(fileRepository.findByFileInfoFilenameAndUser(filename, user));
        mockMvc.perform(MockMvcRequestBuilders.get(fileUrl)
                .header(header, token)
                .queryParam(query, filename))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().bytes(bytes));
    }


    @Test
    public void editTest() throws Exception {
        String filename = "edit.txt";
        fileRepository.save(new File("edit".getBytes(StandardCharsets.UTF_8), new FileInfo(filename, 0), user));
        Assertions.assertNotNull(fileRepository.findByFileInfoFilenameAndUser(filename, user));
        mockMvc.perform(MockMvcRequestBuilders.put(fileUrl)
                .content("{\"filename\":\"1.txt\"}")
                .header(header, token)
                .queryParam(query, filename))
                .andExpect(MockMvcResultMatchers.status().isOk());
        Assertions.assertNotNull(fileRepository.findByFileInfoFilenameAndUser("1.txt", user));
        Assertions.assertNull(fileRepository.findByFileInfoFilenameAndUser(filename, user));
    }

    @Test
    public void deleteTest() throws Exception {
        String filename = "delete.txt";
        fileRepository.save(new File("000".getBytes(StandardCharsets.UTF_8), new FileInfo(filename, 0), user));
        Assertions.assertNotNull(fileRepository.findByFileInfoFilenameAndUser(filename, user));
        mockMvc.perform(MockMvcRequestBuilders.delete(fileUrl)
                .header(header, token)
                .queryParam(query, filename));
        Assertions.assertNull(fileRepository.findByFileInfoFilenameAndUser(filename, user));
    }

    @Test
    public void listTest() throws Exception {
        String filename = "list.txt";
        File file = new File("Часть list".getBytes(StandardCharsets.UTF_8), new FileInfo(filename, 0), user);
        fileRepository.save(file);
        Assertions.assertNotNull(fileRepository.findByFileInfoFilenameAndUser(filename, user));
        String response = mockMvc.perform(MockMvcRequestBuilders.get(listUrl)
                .header(header, token)
                .queryParam("limit", "10"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<FileInfo> list = mapper.readValue(response, new TypeReference<List<FileInfo>>() {});
        Assertions.assertFalse(list.isEmpty());
        Assertions.assertTrue(list.contains(file.getFileInfo()));
    }

    @Test
    public void testLimit() throws Exception {
        String response = mockMvc.perform(MockMvcRequestBuilders.get(listUrl)
                .header(header, token)
                .queryParam("limit", "0"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<FileInfo> list = mapper.readValue(response, new TypeReference<List<FileInfo>>() {});
        Assertions.assertTrue(list.isEmpty());
    }

    @Test
    public void testLogin() throws Exception {
        String response = mockMvc.perform(MockMvcRequestBuilders.post(loginUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new LoginRequest("admin@mycloud.ru", "admin"))))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String token = response.split(":")[1].replace("\"", "").replace("}", "").trim();
        mockMvc.perform(MockMvcRequestBuilders.get(listUrl)
                .header(header, "Bearer " + token)
                .queryParam("limit", "0"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void testWrongLogin() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(loginUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new LoginRequest("0", "0"))))
                        .andExpect(MockMvcResultMatchers.status().is(HttpStatus.UNAUTHORIZED.value()));
    }
}
