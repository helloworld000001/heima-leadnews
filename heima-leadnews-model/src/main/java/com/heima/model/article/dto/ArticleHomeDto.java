package com.heima.model.article.dto;

import lombok.Data;

import java.util.Date;

/**
 * @auther 陈彤琳
 * @Description $
 * 2023/12/11 15:00
 */
@Data
public class ArticleHomeDto {
    // 最大时间
    Date maxBehotTime;
    // 最小时间
    Date minBehotTime;
    // 分页size
    Integer size;
    // 频道ID
    String tag;
}
