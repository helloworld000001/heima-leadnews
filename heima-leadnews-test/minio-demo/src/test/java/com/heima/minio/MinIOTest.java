package com.heima.minio;

import com.heima.file.service.FileStorageService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * @auther 陈彤琳
 * @Description $
 * 2023/12/12 3:19
 */
@SpringBootTest(classes = MinIOApplication.class)
@RunWith(SpringRunner.class)
public class MinIOTest {
    @Autowired
    private FileStorageService fileStorageService;

    /**
     * 把list.html文件上传到minio中，并且可以在浏览器中进行访问
     */
    @Test
    public void test() throws FileNotFoundException {
        FileInputStream inputStream = new FileInputStream("D:\\list.html");
        /* 最终实现的效果是在minio控制台中创建文件夹：年/月/日/文件名 */
        String path = fileStorageService.uploadHtmlFile("", "list.html", inputStream);
        System.out.printf(path);
    }

    /**
     * 把list.html文件上传到minio中，并且可以在浏览器中进行访问
     * @param args
     */
    public static void main(String[] args) throws IOException, ServerException, InvalidBucketNameException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        FileInputStream inputStream = new FileInputStream("D:\\tmp\\js\\index.js");

        // 1. 获取minio的链接信息，创建一个minio的客户端
        MinioClient minioClient = MinioClient.builder().credentials("minio", "minio123").endpoint("http://192.168.200.130:9000").build();

        // 2. 上传
        PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                        .object("plugins/js/index.js") // 文件名称
                        .contentType("text/js") // 文件类型
                        .bucket("leadnews") // 桶名称：与自己创建的bucket名称一样
                        // inputStream.available()表示把流中所有可用的都进行传输
                        .stream(inputStream, inputStream.available(), -1).build();
        minioClient.putObject(putObjectArgs);

        // 访问路径
        // System.out.println("http://192.168.200.130:9000/leadnews/list.html");
    }




}
