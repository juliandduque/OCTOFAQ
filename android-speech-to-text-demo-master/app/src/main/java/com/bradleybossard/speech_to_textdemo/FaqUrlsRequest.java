package com.bradleybossard.speech_to_textdemo;

/**
 * Created by David on 9/17/2017.
 */

public class FaqUrlsRequest {
    private String base_url;
    private String page_limit;

    public FaqUrlsRequest() {}

    public FaqUrlsRequest(String base_url, String page_limit) {
        this.base_url = base_url;
        this.page_limit = page_limit;
    }

    public String getBaseUrl() {
        return base_url;
    }

    public void setBaseUrl(String base_url) {
        this.base_url = base_url;
    }

    public String getPageLimit() {
        return page_limit;
    }

    public void setPageLimit(String page_limit) {
        this.page_limit = page_limit;
    }
}
