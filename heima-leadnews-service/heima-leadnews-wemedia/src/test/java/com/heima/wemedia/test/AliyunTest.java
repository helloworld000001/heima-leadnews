package com.heima.wemedia.test;

import com.heima.common.aliyun.GreenImageScan;
import com.heima.common.aliyun.GreenTextScan;
import com.heima.file.service.FileStorageService;
import com.heima.wemedia.WemediaApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SpringBootTest(classes = WemediaApplication.class)
@RunWith(SpringRunner.class)
public class AliyunTest {
    @Autowired
    private GreenTextScan greenTextScan;

    @Autowired
    private GreenImageScan greenImageScan;

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * 测试文本审核
     */
    @Test
    public void testGreenTextScan() throws Exception {
        Map map = greenTextScan.greeTextScan("冰毒");
    }

    /**
     * 测试图片审核
     * @throws Exception
     */
    @Test
    public void testGreenImageScan() throws Exception {
        byte[] bytes = fileStorageService.downLoadFile("http://192.168.200.130:9000/leadnews/2021/04/26/9f8a93931ab646c0a754475e0c4b0a98.jpg");

        List<byte []> list = new ArrayList<>();
        list.add(bytes);

        Map map = greenImageScan.imageScan(list);
        System.out.println(map);
    }
}
