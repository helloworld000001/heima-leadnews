package com.heima.schedule.service.impl;

import com.alibaba.fastjson.JSON;
import com.heima.common.constants.ScheduleConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.schedule.dtos.Task;
import com.heima.model.schedule.pojos.Taskinfo;
import com.heima.model.schedule.pojos.TaskinfoLogs;
import com.heima.schedule.mapper.TaskinfoLogsMapper;
import com.heima.schedule.mapper.TaskinfoMapper;
import com.heima.schedule.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;

@Service
@Transactional
@Slf4j
public class TaskServiceImpl implements TaskService {
    /**
     * 添加延迟任务
     *
     * @param task
     * @return 返回long类型的TaskId
     */
    @Override
    public long addTask(Task task) {
        // 1. 添加任务到数据库中
        boolean success = addTaskToDb(task);

        // 2. 添加任务到redis -- 添加任务成功判断
        if (success) {
            // 2.1 如果任务的执行时间 小于等于 当前时间，存入list

            // 2.2 如果任务的执行时间 大于等于 当前时间 && 小于等于预设时间（未来5min） 存入redis中
            addTaskToCache(task);
        }

        return task.getTaskId();
    }

    @Autowired
    private CacheService cacheService;

    /**
     * 把任务添加到redis中
     *
     * @param task
     */
    private void addTaskToCache(Task task) {
        String key = task.getTaskType() + "_" + task.getPriority();

        // 获取 5min 之后的值 毫秒值
        Calendar instance = Calendar.getInstance();
        instance.add(Calendar.MINUTE, 5);
        long nextScheduleTime = instance.getTimeInMillis();

        // 2.1 如果任务的执行时间 小于等于 当前时间，存入list --- 当前数据key
        if (task.getExecuteTime() <= System.currentTimeMillis()) {
            cacheService.lLeftPush(ScheduleConstants.TOPIC + key, JSON.toJSONString(task));
        } else if (task.getExecuteTime() <= nextScheduleTime) {
            /* 2.2 如果任务的执行时间 大于等于 当前时间
             && 小于等于预设时间（未来5min） 存入redis中,预设时间是我们自己设定的，为了性能考虑，如果数据量过大zset可能会出现阻塞
             所以只把未来5min要执行的数据存入redis中，其余的仍然放在db中
             --- 未来数据key */
            // task.getExecuteTime() 是优先级，越小越优先，所以把执行时间设为优先级
            cacheService.zAdd(ScheduleConstants.FUTURE + key, JSON.toJSONString(task), task.getExecuteTime());
        }


    }

    @Autowired
    private TaskinfoMapper taskinfoMapper;

    @Autowired
    private TaskinfoLogsMapper taskinfoLogsMapper;

    /**
     * 添加任务到数据库中
     *
     * @param task
     * @return
     */
    private boolean addTaskToDb(Task task) {
        boolean flag = false;
        try {
            // 保存任务表
            // 在主键上面加：@TableId(type = IdType.ID_WORKER):生成19位值，数字类型使用这种策略，比如long。
            Taskinfo taskinfo = new Taskinfo();
            // 属性拷贝
            taskinfo.setParameters(task.getParameters());

            taskinfo.setPriority(task.getPriority());
            taskinfo.setTaskType(task.getTaskType());
            // taskInfo -- Date类型； task -- long类型
            taskinfo.setExecuteTime(new Date(task.getExecuteTime()));

            taskinfoMapper.insert(taskinfo);

            // 设置taskId
            task.setTaskId(taskinfo.getTaskId());

            // 保存任务日志数据
            TaskinfoLogs taskinfoLogs = new TaskinfoLogs();
            taskinfoLogs.setTaskId(taskinfo.getTaskId());
            taskinfoLogs.setExecuteTime(taskinfo.getExecuteTime());
            taskinfoLogs.setParameters(taskinfo.getParameters());
            taskinfoLogs.setPriority(taskinfo.getPriority());
            taskinfoLogs.setTaskType(taskinfo.getTaskType());

            taskinfoLogs.setVersion(1);
            taskinfoLogs.setStatus(ScheduleConstants.SCHEDULED); // 初始化状态

            taskinfoLogsMapper.insert(taskinfoLogs);
            flag = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }
}
