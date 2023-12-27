package com.heima.schedule.service;

import com.heima.model.schedule.dtos.Task;

public interface TaskService {

    /**
     * 添加延迟任务
     * @param task
     * @return 返回long类型的TaskId
     */
    public long addTask(Task task);

    /**
     * 取消任务
     * @param taskId
     * @return
     */
    public boolean cancelTask(long taskId);

    /**
     * 根据类型和优先级拉取任务
     * @param type
     * @param priority
     * @return
     */
    public Task pull(int type, int priority);
}
