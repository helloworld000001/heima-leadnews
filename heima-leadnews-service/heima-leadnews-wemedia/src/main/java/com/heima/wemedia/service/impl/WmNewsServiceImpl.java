package com.heima.wemedia.service.impl;


import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.exception.CustomException;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.common.enums.WemediaConstants;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmNewsMaterial;
import com.heima.utils.thread.WmThreadLocalUtil;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmNewsMaterialMapper;
import com.heima.wemedia.service.WmNewsAutoScanService;
import com.heima.wemedia.service.WmNewsService;
import com.heima.wemedia.service.WmNewsTaskService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class WmNewsServiceImpl extends ServiceImpl<WmNewsMapper, WmNews> implements WmNewsService {


    @Override
    public ResponseResult findList(WmNewsPageReqDto dto) {
        // 1. 检查参数
        // 分页参数检查
        dto.checkParam();

        // 2. 分页条件查询
        IPage page = new Page(dto.getPage(), dto.getSize());

        LambdaQueryWrapper<WmNews> queryWrapper = new LambdaQueryWrapper<>();
        // 状态精确查询
        if (dto.getStatus() != null) {
            queryWrapper.eq(WmNews::getStatus, dto.getStatus());
        }

        // 频道精确查询
        if (dto.getChannelId() != null) {
            queryWrapper.eq(WmNews::getChannelId, dto.getChannelId());
        }

        // 时间范围查询
        if(dto.getBeginPubDate() != null && dto.getEndPubDate() != null){
            queryWrapper.between(WmNews::getCreatedTime,  dto.getBeginPubDate(), dto.getEndPubDate());
        }

        // 关键字的模糊查询
        if(StringUtils.isNoneBlank(dto.getKeyword())){
            queryWrapper.like(WmNews::getTitle, dto.getKeyword());
        }

        // 查询当前登录人的文章
        queryWrapper.eq(WmNews::getUserId, WmThreadLocalUtil.getUser().getId());

        // 按照发布时间倒序查询
        queryWrapper.orderByDesc(WmNews::getPublishTime);

        page = page(page, queryWrapper);

        // 3. 返回结果
        ResponseResult result = new PageResponseResult(dto.getPage(), dto.getSize(), (int) page.getTotal());
        result.setData(page.getRecords());

        return result;
    }

    @Autowired
    private WmNewsAutoScanService wmNewsAutoScanService;

    @Autowired
    private WmNewsTaskService wmNewsTaskService;

    @Override
    public ResponseResult submitNews(WmNewsDto dto) {
        // 1. 验证参数
        if (dto == null || dto.getContent() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        // 2. 保存数据
        // 2.1 保存或修改文章到wm_news
        WmNews news = new WmNews();

        // 属性拷贝：使用spring框架的工具类，匹配传来对象和想要存入的类对象中名称和类型匹配的参数
        BeanUtils.copyProperties(dto, news);

        /* 封面图片：dto(List<String>) --- news(String)
        传来的dto的images参数是列表，需要转化
        */
        if (dto.getImages() != null && dto.getImages().size() > 0) {
            // 功能：["abc.jpg","xyz.jpg"] --- "abc.jpg,xyz.jpg"
            String imageStr = StringUtils.join(dto.getImages(), ",");
            // 复制进news对象中
            news.setImages(imageStr);
        }

        /* 如果当前封面类型是自动 -1。WmNews中的type字段是没有定义-1的，也不能存入负值 */
        if (dto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)) {
            // 暂时设置为null,后面会根据匹配规则匹配为单图多图无图等
            news.setType(null);
        }

        saveOrUpdateNews(news);

        // 2.2 判断是否为草稿
        if (dto.getStatus().equals(WmNews.Status.NORMAL)) {
            // 2.2.1 是就结束当前方法
            return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
        }

        // 2.2.2 不是就将素材和文章关联信息进行保存
        // 提取文章内容中的图片信息
        List<String> materials = extractUrlInfo(dto.getContent());
        // 保存素材和文章的关系
        saveRelativeInfoContent(materials, news.getId());

        // 如果不是草稿，保存文章和封面图片与素材的关系
        // 若当前布局为自动，需要匹配出封面单图多图无图等
        saveRelativeInfoForCover(dto, news, materials);

        // 审核文章
        // wmNewsAutoScanService.autoScanWmNews(news.getId());
        wmNewsTaskService.addNewsToTask(news.getId(), news.getPublishTime());


        // 3. 返回结果
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 如果当前封面类型为自动
     * 1. 内容图片 >= 1 且 < 3  匹配单图 type=1
     * 2. 内容图片 >= 3 匹配多图 type=3
     * 3. 内容图片 = 0 匹配无图 type=0
     *
     * @param dto
     * @param news
     * @param materials
     */
    private void saveRelativeInfoForCover(WmNewsDto dto, WmNews news, List<String> materials) {
        // 如果当前封面类型为自动
        List<String> images = dto.getImages();
        if (dto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)) {
            // 1. 内容图片 >= 3 匹配多图 type=3
            if(materials.size() >= 3){
                news.setType(WemediaConstants.WM_NEWS_MANY_IMAGE);
                // 素材中选取三张图片赋值给images参数，作为封面图片
                images = materials.stream().limit(3).collect(Collectors.toList());
            } else if (materials.size() >= 1 ) {
                // 2. 内容图片 >= 1 且 < 3  匹配单图 type=1
                news.setType(WemediaConstants.WM_NEWS_SINGLE_IMAGE);
                // 素材中选取一张图片赋值给images参数，作为封面图片
                images = materials.stream().limit(1).collect(Collectors.toList());
            } else {
                // 3. 内容图片 = 0 匹配无图 type=0
                news.setType(WemediaConstants.WM_NEWS_NONE_IMAGE);
            }

            if(images.size() > 0 && images != null){
                news.setImages(StringUtils.join(images, ","));
            }
            // 修改文章
            updateById(news);
        }
        if(images != null && images.size() > 0){
            saveRelativeInfo(images, news.getId(), WemediaConstants.WM_COVER_REFERENCE);
        }
    }

    /**
     * 提取文章中content中的图片信息
     *
     * @param content
     * @return
     */

    private List<String> extractUrlInfo(String content) {
        List<String> materials = new ArrayList<>();

        // 1. 将json转列表
        List<Map> maps = JSON.parseArray(content, Map.class);

        // 2. 遍历获取img的url并存入对象
        for (Map map : maps) {
            if (map.get("type").equals("image")) {
                String imgUrl = (String) map.get("value");
                materials.add(imgUrl);
            }
        }

        // 3. 返回结果
        return materials;
    }

    /**
     * 处理文章和素材的关系
     *
     * @param materials
     * @param newsId
     */

    private void saveRelativeInfoContent(List<String> materials, Integer newsId) {
        saveRelativeInfo(materials, newsId, WemediaConstants.WM_CONTENT_REFERENCE);
    }

    @Autowired
    private WmMaterialMapper materialMapper;

    /**
     * 保存文章所有图片和素材的关系到数据库中
     * @param materials
     * @param newsId
     * @param wmContentReference
     */

    private void saveRelativeInfo(List<String> materials, Integer newsId, Short wmContentReference) {
        // 当materials不为空值时再执行以下操作
        if (materials != null && !materials.isEmpty()) {
            // 根据materials获取素材id：由于materials是一个list，有多个对象，所以查询时使用in不要用eq
            List<WmMaterial> dbMaterials = materialMapper.selectList(Wrappers.<WmMaterial>lambdaQuery().in(WmMaterial::getUrl, materials));

            // 判断素材是否有效
            if (dbMaterials == null || dbMaterials.size() == 0) {
                // 手动抛出异常：提示调用者素材失效，同时回滚之前将素材数据存入库中的操作
                throw new CustomException(AppHttpCodeEnum.MATERIAL_REFERENCE_FAIL);
            }

            // 素材数量与查询出的素材数量不一致
            if (materials.size() != dbMaterials.size()) {
                throw new CustomException(AppHttpCodeEnum.MATERIAL_REFERENCE_FAIL);
            }

            // 只要获取dbMaterials中的ids
            List<Integer> idList = dbMaterials.stream().map(WmMaterial::getId).collect(Collectors.toList());

            // 批量保存
            newsMaterialMapper.saveRelations(idList, newsId, wmContentReference);
        }
    }


    @Autowired
    private WmNewsMaterialMapper newsMaterialMapper;

    /**
     * 保存或修改文章
     *
     * @param wmNews
     */
    private void saveOrUpdateNews(WmNews wmNews) {
        // 1. 补全属性
        wmNews.setUserId(WmThreadLocalUtil.getUser().getId());
        wmNews.setCreatedTime(new Date());
        wmNews.setSubmitedTime(new Date());
        // 默认上架
        wmNews.setEnable((short) 1);

        // 2. 保存/修改
        if (wmNews.getId() == null) {
            // 保存操作
            save(wmNews);
        } else {
            // 修改操作
            // 删除文章与原先素材的关系
            newsMaterialMapper.delete(Wrappers.<WmNewsMaterial>lambdaQuery().eq(WmNewsMaterial::getNewsId, wmNews.getId()));

            // 修改
            updateById(wmNews);
        }
    }
}