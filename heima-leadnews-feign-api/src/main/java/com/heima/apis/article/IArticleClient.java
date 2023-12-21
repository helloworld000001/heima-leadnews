package com.heima.apis.article;

import com.heima.model.article.dto.ArticleDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.common.dtos.ResponseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import org.springframework.web.bind.annotation.PostMapping;
@FeignClient("leadnews-article")
public interface IArticleClient {


    @PostMapping("/api/v1/article/save")
    public ResponseResult saveArticle(ArticleDto articleDto);
}
