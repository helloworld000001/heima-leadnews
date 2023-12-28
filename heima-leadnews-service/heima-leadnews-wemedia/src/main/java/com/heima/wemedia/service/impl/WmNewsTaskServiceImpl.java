package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSON;
import com.heima.apis.schedule.IScheduleClient;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.TaskTypeEnum;
import com.heima.model.schedule.dtos.Task;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.utils.common.ProtostuffUtil;
import com.heima.wemedia.service.WmNewsAutoScanService;
import com.heima.wemedia.service.WmNewsTaskService;
import javafx.concurrent.ScheduledService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@Slf4j
public class WmNewsTaskServiceImpl implements WmNewsTaskService {
    @Autowired
    private IScheduleClient scheduleClient;

    /**
     * 添加任务到延迟队列中
     *
     * @param id          文章id
     * @param publishTime 发布时间，可以作为任务的执行时间
     */
    @Override
    @Async
    public void addNewsToTask(Integer id, Date publishTime) {
        Task task = new Task();
        task.setExecuteTime(publishTime.getTime());
        task.setTaskType(TaskTypeEnum.NEWS_SCAN_TIME.getTaskType());
        task.setPriority(TaskTypeEnum.NEWS_SCAN_TIME.getPriority());

        WmNews wmNews = new WmNews();
        wmNews.setId(id);
        // 加入wmNews序列化后的数据
        task.setParameters(ProtostuffUtil.serialize(wmNews));

        scheduleClient.addTask(task);
    }

    @Autowired
    private WmNewsAutoScanService wmNewsAutoScanService;

    /**
     * 根据任务进行文章审核:每秒钟从list中拉取一次任务
     */
    /*
    * 1. 当前时间为10:00,发布时间设为10:01,那么将存在zset未来消费队列中,pull拉取的是当前消费队列list，拉取不到
    *   当点击发布按钮1min后,由于1s拉取一次list任务,当前任务已经被加到消费队列中,就可以拉取到了,从而进行审核
    * */
    @Scheduled(fixedRate = 1000)
    @Override
    public void scanNewsByTask() {
        // pull()根据类型和优先级从当前消费队列中拉取任务
        ResponseResult responseResult = scheduleClient.pull(TaskTypeEnum.NEWS_SCAN_TIME.getTaskType(), TaskTypeEnum.NEWS_SCAN_TIME.getPriority());
        // responseResult.getData()返回的是一个task
        if(responseResult.getCode().equals(200) && responseResult.getData() != null){
            // responseResult.getData()是一个map集合，先转成json字符串，再转化为Task对象
            Task task = JSON.parseObject(JSON.toJSONString(responseResult.getData()), Task.class);

            // 通过反序列化获取WmNews(在上面的addNewsToTask方法中，已经将WmNews对象序列化并存储到task的parameters属性中)，已经将WmNews对象序列化并存储到task的parameters属性中)
            WmNews wmNews = ProtostuffUtil.deserialize(task.getParameters(), WmNews.class);

            wmNewsAutoScanService.autoScanWmNews(wmNews.getId());
        }
    }
}
