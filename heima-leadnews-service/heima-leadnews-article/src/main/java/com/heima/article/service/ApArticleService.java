package com.heima.article.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.article.dto.ArticleHomeDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.common.dtos.ResponseResult;

/**
 * @auther 陈彤琳
 * @Description $
 * 2023/12/11 15:32
 */
public interface ApArticleService extends IService<ApArticle> {
    /**
     * 加载文章列表
     * @param dto
     * @param type 1加载更多  2加载最新
     * @return
     */
    public ResponseResult load(ArticleHomeDto dto, Short  type);
}
