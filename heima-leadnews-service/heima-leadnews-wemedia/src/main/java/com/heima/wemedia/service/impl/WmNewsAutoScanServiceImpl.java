package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.apis.article.IArticleClient;
import com.heima.common.aliyun.GreenImageScan;
import com.heima.common.aliyun.GreenTextScan;
import com.heima.common.tess4j.Tess4jClient;
import com.heima.file.service.FileStorageService;
import com.heima.model.article.dto.ArticleDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmChannel;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmSensitive;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.utils.common.SensitiveWordUtil;
import com.heima.wemedia.mapper.WmChannelMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmSensitiveMapper;
import com.heima.wemedia.mapper.WmUserMapper;
import com.heima.wemedia.service.WmNewsAutoScanService;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.TesseractException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class WmNewsAutoScanServiceImpl implements WmNewsAutoScanService {
    @Autowired
    private WmNewsMapper wmNewsMapper;

    /**
     * 自媒体文章审核
     *
     * @param id 自媒体文章id
     */
    @Override
    @Async // 异步审核
    public void autoScanWmNews(Integer id) {
        // 1. 查询wemeida文章
        WmNews wmNews = wmNewsMapper.selectById(id);
        if (wmNews == null) {
            throw new RuntimeException("WmNewsAutoScanServiceImpl-文章不存在");
        }
        // 先确保审核的状态是待审核的状态
        if (wmNews.getStatus().equals(WmNews.Status.SUBMIT.getCode())) {
            // 从content中提取文本内容和图片
            Map<String, Object> textAndImages = handleTextAndImage(wmNews);

            // 自管理的敏感词过滤
            boolean isSensitive = handleSensitiveScan((String) textAndImages.get("content"), wmNews);
            if(!isSensitive) return;

            // 2. 审核文本内容 阿里云接口
            boolean isTextScan = handleTextScan((String) textAndImages.get("content"), wmNews);
            // 审核失败，返回
            if (!isTextScan) return;

            // 3. 审核图片 阿里云接口
            boolean isImagesScan = handleImagesScan((List<String>)textAndImages.get("images"), wmNews);
            // 审核失败，返回
            if (!isImagesScan) return;

            // 4. 审核成功，调用接口保存app端相关的文章数据
            ResponseResult responseResult = saveAppArticle(wmNews);
            if(!responseResult.getCode().equals(200)){
                throw new RuntimeException("WmNewsAutoScanServiceImpl-文章审核，保存app端相关文章失败");
            }

            // 回填article_id
            wmNews.setArticleId((Long)responseResult.getData());

            updateWmNews(wmNews, (short) 9, "审核成功");
        }


    }

    @Autowired
    private WmSensitiveMapper wmSensitiveMapper;

    /**
     * 自管理的敏感词审核
     * @param content
     * @param wmNews
     * @return
     */
    private boolean handleSensitiveScan(String content, WmNews wmNews) {
        boolean flag = true;

        // 获取所有的敏感词
        List<WmSensitive> wmSensitives = wmSensitiveMapper.selectList(Wrappers.<WmSensitive>lambdaQuery().select(WmSensitive::getSensitives));
        List<String> sensitiveList = wmSensitives.stream().map(WmSensitive::getSensitives).collect(Collectors.toList());


        // 初始化敏感词库
        SensitiveWordUtil.initMap(sensitiveList);

        // 查看文章中是否存在敏感词:map就是找到的敏感词
        Map<String, Integer> map = SensitiveWordUtil.matchWords(content);
        if(map.size() > 0){
            // 存在敏感词
            updateWmNews(wmNews, (short) 2, "文章存在违规内容" + map);
            flag = false;
        }
        return flag;
    }

    @Autowired
    private IArticleClient articleClient;

    @Autowired
    private WmChannelMapper wmChannelMapper;

    @Autowired
    private WmUserMapper wmUserMapper;

    /**
     * 保存app端相关的文章数据
     * @param wmNews
     */
    private ResponseResult saveAppArticle(WmNews wmNews) {
        ArticleDto articleDto = new ArticleDto();
        // 属性拷贝
        BeanUtils.copyProperties(wmNews, articleDto);

        // 文章布局:属性名不同，需要单独设置
        articleDto.setLayout(wmNews.getType());
        // 频道
        WmChannel wmChannel = wmChannelMapper.selectById(wmNews.getChannelId());
        if(wmChannel != null) {
            articleDto.setChannelName(wmChannel.getName());
        }

        // 作者
        articleDto.setAuthorId(wmNews.getUserId().longValue());
        WmUser wmUser = wmUserMapper.selectById(wmNews.getUserId());
        if(wmUser != null){
            articleDto.setAuthorName(wmUser.getName());
        }

        // 文章id
        if(wmNews.getArticleId() != null ){
            articleDto.setId(wmNews.getArticleId().longValue());
        }

        articleDto.setCreatedTime(new Date());

        // 调用articleClient下的saveArticle方法才能根据雪花算法得到article_id的值
        ResponseResult responseResult = articleClient.saveArticle(articleDto);
        return responseResult;
    }

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired(required = false)
    private GreenImageScan greenImageScan;

    @Autowired
    private Tess4jClient tess4jClient;

    /**
     * 审核图片
     * @param images
     * @param wmNews
     * @return
     */

    private boolean handleImagesScan(List<String> images, WmNews wmNews) {
       boolean flag = true;
       if(images == null ||images.size() == 0){
           return flag;
       }
/*
        // 从minIO中下载图片
        // 图片去重
        images = images.stream().distinct().collect(Collectors.toList());
        List<byte[]> imageList = new ArrayList<>();

        try {
            for (String image : images) {
                byte[] bytes = fileStorageService.downLoadFile(image);

                // byte[] -- bufferedImage
                ByteArrayInputStream in = new ByteArrayInputStream(bytes);
                BufferedImage bufferedImage = ImageIO.read(in);

                // 图片识别
                String result = tess4jClient.doOCR(bufferedImage);

                // 过滤文字:审核
                boolean isSensitive = handleSensitiveScan(result, wmNews);
                if(!isSensitive){
                    return isSensitive;
                }

                imageList.add(bytes);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 审核图片
        try {
            Map map = greenImageScan.imageScan(imageList);

            // 审核失败
            if(map.get("suggestion").equals("block")){
                flag = false;
                updateWmNews(wmNews, (short) 2, "当前文章中包含违规内容，请重新修改");
            }
            // 不确定信息.需要人工审核
            if(map.get("suggestion").equals("review")){
                flag = false;
                updateWmNews(wmNews, (short) 3, "当前文章中存在不确定内容");
            }
        } catch (Exception e) {
            flag = false;
            throw new RuntimeException(e);
        }*/
        return flag;
    }

    @Autowired(required = false)
    private GreenTextScan greenTextScan;

    /**
     * 审核纯文本内容
     * @param content
     * @param wmNews  之所以要传wmNews对象，是因为如果审核失败需要在wmNews中修改status状态
     * @return
     */
    private boolean handleTextScan(String content, WmNews wmNews) {
        boolean flag = true;

        if((wmNews.getTitle() + "" + content).length() == 0){
            return flag;
        }

        /*try {
            Map map = greenTextScan.greeTextScan(wmNews.getTitle() + "" + content);
            // 审核失败
            if(map.get("suggestion").equals("block")){
                flag = false;
                updateWmNews(wmNews, (short) 2, "当前文章内容包含违规内容，请重新修改");
            }
            // 不确定信息.需要人工审核
            if(map.get("suggestion").equals("review")){
                flag = false;
                updateWmNews(wmNews, (short) 3, "当前文章内容中存在不确定内容");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }*/
        return flag;
    }

    /**
     * 修改文章内容
     * @param wmNews
     * @param status
     * @param reason
     */

    private void updateWmNews(WmNews wmNews, short status, String reason) {
        wmNews.setStatus(status);
        wmNews.setReason(reason);
        wmNewsMapper.updateById(wmNews);
    }

    /**
     * 从wemedia的文章中提取图片和内容文本
     *
     * @param wmNews 因为提取的图片是content中的图片和封面图片（存在wmNews对象中
     */
    private Map<String, Object> handleTextAndImage(WmNews wmNews) {
        StringBuilder stringBuilder = new StringBuilder();

        ArrayList<String> images = new ArrayList<>();

        // 1. 从content中提取图片和文本
        if (StringUtils.isNotBlank(wmNews.getContent())) {
            List<Map> maps = JSON.parseArray(wmNews.getContent(), Map.class);
            for (Map map : maps) {
                if (map.get("type").equals("text")) {
                    // 存贮纯文本内容
                    stringBuilder.append(map.get("value"));
                }
                if (map.get("type").equals("image")) {
                    images.add((String) map.get("value"));
                }
            }
        }

        // 2. 提取封面的图片
        if (StringUtils.isNotBlank(wmNews.getImages())) {
            String[] split = wmNews.getImages().split(",");
            images.addAll(Arrays.asList(split));
        }

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("content", stringBuilder.toString());
        resultMap.put("images", images);

        return resultMap;
    }
}
