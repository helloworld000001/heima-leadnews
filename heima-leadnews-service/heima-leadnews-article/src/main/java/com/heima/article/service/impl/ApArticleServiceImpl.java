package com.heima.article.service.impl;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.ApArticleService;
import com.heima.common.constants.ArticleConstants;
import com.heima.model.article.dto.ArticleHomeDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.common.dtos.ResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * @auther 陈彤琳
 * @Description $
 * 2023/12/11 15:34
 */
@Service
@Transactional
@Slf4j
public class ApArticleServiceImpl extends ServiceImpl<ApArticleMapper, ApArticle> implements ApArticleService{
    @Autowired
    private ApArticleMapper apArticleMapper;

    private final static short MAX_PAGE_SIZE = 50;

    /**
     * 加载文章列表
     * @param dto
     * @param type 1加载更多  2加载最新
     * @return
     */
    @Override
    public ResponseResult load(ArticleHomeDto dto, Short type) {
        // 1. 检验参数：对于传入的dto和type都要检验，dto中的所有参数一个个校验
        // 1.1 分页条数的校验
        Integer size = dto.getSize();
        if(size == null || size == 0){
            size = 10;
        }
        // 1.2 分页的值不超过50
        size = Math.min(size, MAX_PAGE_SIZE);
        dto.setSize(size);

        // 1.3 校验type
        if(!type.equals(ArticleConstants.LOADTYPE_LOAD_MORE) && !type.equals(ArticleConstants.LOADTYPE_LOAD_NEW)){
            type = ArticleConstants.LOADTYPE_LOAD_MORE;
        }

        // 1.4 校验频道参数
        if(StringUtils.isBlank(dto.getTag())){
            dto.setTag(ArticleConstants.DEFAULT_TAG);
        }

        // 1.5 校验时间
        if(dto.getMaxBehotTime() == null)dto.setMaxBehotTime(new Date());
        if(dto.getMinBehotTime() == null)dto.setMinBehotTime(new Date());

        // 2. 查询
        List<ApArticle> list = apArticleMapper.loadArticleList(dto, type);
        // 3. 结果返回
        return ResponseResult.okResult(list);
    }
}
