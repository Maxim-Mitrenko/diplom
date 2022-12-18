package com.example.diplom;

import com.example.diplom.model.entity.FileInfo;
import com.example.diplom.model.login.LoginRequest;
import com.example.diplom.model.login.LoginResponse;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.classic.methods.HttpGet;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.entity.mime.FileBody;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.ClassicHttpRequest;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.ContentType;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpEntity;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class DiplomApplicationTests {

    private final Network network = Network.newNetwork();

    @Container
    private final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:latest")
            .withNetwork(network)
            .withExposedPorts(3306)
            .withDatabaseName("cloud")
            .withUsername("root")
            .withPassword("mysql");

    @Container
    private final GenericContainer<?> app = new GenericContainer<>("backend:1.0")
            .withNetwork(network)
            .withExposedPorts(8090)
            .dependsOn(mysql);

    @Autowired
    private TestRestTemplate testRestTemplate;
    private static final File file = new File("src\\test\\java\\com\\example\\diplom\\testphoto.jpg");
    private static final String urlStart = "http://localhost:";
    private static final String urlFile = "/file?filename=";
    private String token;


    @BeforeEach
    public void before() {
        token = login();
    }

    @Test
    void contextLoads() {
    }

    public CloseableHttpResponse uploadFile() throws Exception {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            FileBody body = new FileBody(file, ContentType.IMAGE_JPEG);
            HttpEntity entity = MultipartEntityBuilder.create().addPart(body.getFilename(), body).build();
            ClassicHttpRequest request = ClassicRequestBuilder.post(urlStart + app.getMappedPort(8090) + urlFile + "testphoto.jpg")
                    .setEntity(entity)
                    .addHeader("auth-token", token)
                    .build();
            return httpClient.execute(request);
        }
    }

    public CloseableHttpResponse getFile(String name) throws IOException {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            return httpClient.execute(new HttpGet(urlStart + app.getMappedPort(8090) + urlFile + name));
        }
    }
    public String login() {
        return testRestTemplate.postForObject(urlStart + app.getMappedPort(8090) + "/login", new LoginRequest("admin@mycloud.ru", "admin"), LoginResponse.class).getAuthToken();
    }

    public void setHeaderTestRestTemplate() {
        testRestTemplate.getRestTemplate().setInterceptors(
                Collections.singletonList((request, body, execution) -> {
                    request.getHeaders()
                            .add("auth-token", token);
                    return execution.execute(request, body);
                }));

    }

    public byte[] fileBytes() throws IOException {
        try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(file))) {
            return is.readAllBytes();
        }
    }

    @Test
    public void loginTest() throws Exception {
        CloseableHttpResponse notLogin = uploadFile();
        setHeaderTestRestTemplate();
        CloseableHttpResponse login = uploadFile();
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED.value(), notLogin.getCode());
        Assertions.assertEquals(HttpStatus.OK.value(), login.getCode());
    }

    @Test
    public void uploadTest() throws Exception {
        CloseableHttpResponse response = uploadFile();
        System.out.println(response.getCode());
        Assertions.assertEquals(HttpStatus.OK.value(), response.getCode());
    }

    @Test
    public void getTest() throws Exception {
        uploadFile();
        CloseableHttpResponse response = getFile(file.getName());
            Assertions.assertEquals(HttpStatus.OK.value(), response.getCode());
            Assertions.assertArrayEquals(fileBytes(), response.getEntity().getContent().readAllBytes());
    }

    @Test
    public void deleteTest() throws Exception {
        setHeaderTestRestTemplate();
        uploadFile();
        testRestTemplate.delete(urlStart + app.getMappedPort(8090) + urlFile + file.getName());
        CloseableHttpResponse response = getFile(file.getName());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), response.getCode());
    }

    @Test
    public void editTest() throws Exception {
        setHeaderTestRestTemplate();
        uploadFile();
        testRestTemplate.put(urlStart + app.getMappedPort(8090) + urlFile + file.getName(), "{\"filename\":\"1.jpg\"}");
        CloseableHttpResponse wrong = getFile(file.getName());
        CloseableHttpResponse correct = getFile("1.jpg");
        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), wrong.getCode());
        Assertions.assertEquals(HttpStatus.OK.value(), correct.getCode());
        Assertions.assertArrayEquals(fileBytes(), correct.getEntity().getContent().readAllBytes());
    }

    @Test
    public void listTest() throws Exception {
        setHeaderTestRestTemplate();
        uploadFile();
        var response =  testRestTemplate.getForEntity(urlStart + app.getMappedPort(8090) + "/list?limit=1", List.class);
        List<FileInfo> list = response.getBody();
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(file.getName(), list.get(0).getFilename());
    }

    @Test
    public void testLimit() throws Exception {
        setHeaderTestRestTemplate();
        uploadFile();
        var response = testRestTemplate.getForEntity(urlStart + app.getMappedPort(8090) + "/list?limit=0", List.class);
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertTrue(response.getBody().isEmpty());
    }
}
