package com.heima.article.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.article.dto.ArticleDto;
import com.heima.model.article.dto.ArticleHomeDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.common.dtos.ResponseResult;
import org.springframework.web.bind.annotation.RequestBody;

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

    /**
     * 保存app端相关文章
     * @param articleDto
     * @return
     */
    public ResponseResult saveArticle(@RequestBody ArticleDto articleDto);
}
