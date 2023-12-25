package com.heima.article.service.impl;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.article.mapper.ApArticleConfigMapper;
import com.heima.article.mapper.ApArticleContentMapper;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.ApArticleService;
import com.heima.article.service.ArticleFreemarkerService;
import com.heima.common.constants.ArticleConstants;
import com.heima.model.article.dto.ArticleDto;
import com.heima.model.article.dto.ArticleHomeDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleConfig;
import com.heima.model.article.pojos.ApArticleContent;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
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
    @Autowired
    private ApArticleConfigMapper apArticleConfigMapper;

    @Autowired
    private ApArticleContentMapper apArticleContentMapper;

    @Autowired
    private ArticleFreemarkerService articleFreemarkerService;

    /**
     * 保存app端相关文章
     * @param articleDto
     * @return
     */
    @Override
    public ResponseResult saveArticle(ArticleDto articleDto) {
/*

        // 测试服务降级：配置中设置超时时间2000ms-2s
        try {
            // 线程sleep3s
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
*/

        // 1. 检查参数
        if(articleDto == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        ApArticle apArticle = new ApArticle();
        BeanUtils.copyProperties(articleDto, apArticle);


        // 2. 判断article表中是否存在id
        if(apArticleMapper.selectOne(Wrappers.<ApArticle>lambdaQuery().eq(ApArticle::getId, articleDto.getId())) == null){
            // 2.1 不存在id, 保存文章 文章配置 文章内容
            // 保存文章
            save(apArticle);

            // 保存文章配置
            ApArticleConfig apArticleConfig = new ApArticleConfig(apArticle.getId());
            apArticleConfigMapper.insert(apArticleConfig);

            // 保存文章内容
            ApArticleContent apArticleContent = new ApArticleContent();
            apArticleContent.setArticleId(apArticle.getId());
            apArticleContent.setContent(articleDto.getContent());
            apArticleContentMapper.insert(apArticleContent);

        } else {
            // 2.2 存在id, 修改文章 文章内容
            updateById(apArticle);

            // 修改文章内容
            // 根据id查article_content表 -- 修改文章内容
            ApArticleContent apArticleContent = apArticleContentMapper.selectOne(Wrappers.<ApArticleContent>lambdaQuery().eq(ApArticleContent::getArticleId, articleDto.getId()));
            apArticleContent.setContent(articleDto.getContent());
            apArticleContentMapper.updateById(apArticleContent);
        }

        // 异步调用，生成静态文件上传到minIO中
        articleFreemarkerService.buildArticleToMinIO(apArticle, articleDto.getContent());

        // 3. 结果返回 文章id
        return ResponseResult.okResult(apArticle.getId());
    }
}
