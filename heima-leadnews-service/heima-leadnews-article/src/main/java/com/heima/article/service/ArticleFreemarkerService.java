package com.heima.article.service;

import com.heima.model.article.pojos.ApArticle;

public interface ArticleFreemarkerService {
    /**
     * 上传静态文件到minIO中
     * @param apArticle
     * @param content
     */
    public void buildArticleToMinIO(ApArticle apArticle, String content);
}
