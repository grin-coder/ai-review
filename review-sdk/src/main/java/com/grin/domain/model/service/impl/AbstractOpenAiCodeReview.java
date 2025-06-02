package com.grin.domain.model.service.impl;

import com.grin.domain.model.service.IOpenAiCodeReview;
import com.grin.infrastructure.ai.IOpenAi;
import com.grin.infrastructure.git.GitCommand;
import com.grin.infrastructure.weixin.WeiXin;

/**
 * ClassName:AbstactOpenAiCodeReview
 * Package:com.grin.domain.model.service.impl
 * Description
 *
 * @Author :刘昂
 * @Create :2025/6/2--23:00
 * @Version :v1.0
 */
public abstract class AbstractOpenAiCodeReview implements IOpenAiCodeReview {
    @Override
    public void exec() throws Exception {
        // 1.获取需要评审的代码
        String diff = diff();
        // 2. 进行codeReview
        String review = codeReview(diff);
        // 3. 将review结果写入git仓库
        String logUrl = writeLog(review);
        // 4. 发送微信消息
        sendWeixinMessage(logUrl);
    }

    protected abstract String diff() throws Exception;

    protected abstract String codeReview(String diff);

    protected abstract String writeLog(String review) throws Exception;

    protected abstract void sendWeixinMessage(String logUrl);
}
