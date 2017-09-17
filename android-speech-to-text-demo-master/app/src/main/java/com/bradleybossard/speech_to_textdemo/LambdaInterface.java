package com.bradleybossard.speech_to_textdemo;

import com.amazonaws.mobileconnectors.lambdainvoker.LambdaFunction;

/**
 * Created by David on 9/17/2017.
 */

public interface LambdaInterface {
    @LambdaFunction
    String OctoFaqLinksRequest(FaqUrlsRequest faqUrlsRequest);
}
