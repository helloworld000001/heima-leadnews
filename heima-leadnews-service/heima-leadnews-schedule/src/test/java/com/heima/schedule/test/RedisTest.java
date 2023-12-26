package com.heima.schedule.test;

import com.heima.common.redis.CacheService;
import com.heima.schedule.ScheduleApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Set;

@SpringBootTest(classes = ScheduleApplication.class)
@RunWith(SpringRunner.class)
public class RedisTest {
    @Autowired
    private CacheService cacheService;
    @Test
    public void testList(){
        // 在list的左边添加元素
        // cacheService.lLeftPush("list_001", "helloRedis");

        // 在list的右边查找并删除这个元素
        String list001 = cacheService.lRightPop("list_001");
        System.out.println("list001 = " + list001);

    }

    @Test
    public void testZset(){
        // 添加数据到zset中 分值
//        cacheService.zAdd("zset_001", "hello zeset 001", 1000);
//        cacheService.zAdd("zset_001", "hello zeset 002", 5555);
//        cacheService.zAdd("zset_001", "hello zeset 003", 33);
//        cacheService.zAdd("zset_001", "hello zeset 004", 22222);

        // 按照分值获取数据
        Set<String> zset001 = cacheService.zRangeByScore("zset_001", 0, 20000);
        System.out.println("zset001 = " + zset001);// zset001 = [hello zeset 003, hello zeset 001, hello zeset 002]

    }
}
