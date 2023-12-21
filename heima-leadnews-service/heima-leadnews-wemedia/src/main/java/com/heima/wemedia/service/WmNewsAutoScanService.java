package com.heima.wemedia.service;

import org.springframework.boot.actuate.autoconfigure.integration.IntegrationGraphEndpointAutoConfiguration;

public interface WmNewsAutoScanService {
    /**
     * 自媒体文章审核
     * @param id 自媒体文章id
     */
    public void autoScanWmNews(Integer id);
}
