package com.heima.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.model.article.pojos.ApArticleContent;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;

/**
 * @auther 陈彤琳
 * @Description $
 * 2023/12/13 0:35
 */
@Mapper
public interface ApArticleContentMapper extends BaseMapper<ApArticleContent> {
}
