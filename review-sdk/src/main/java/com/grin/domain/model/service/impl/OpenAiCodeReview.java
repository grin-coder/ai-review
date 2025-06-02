package com.grin.domain.model.service.impl;

import com.grin.infrastructure.ai.IOpenAi;
import com.grin.infrastructure.git.GitCommand;
import com.grin.infrastructure.weixin.WeiXin;
import com.grin.infrastructure.weixin.dto.MessageDto;

import java.util.HashMap;

/**
 * ClassName:OpenAiCodeReview
 * Package:com.grin.domain.model.service.impl
 * Description
 *
 * @Author :刘昂
 * @Create :2025/6/2--23:49
 * @Version :v1.0
 */
public class OpenAiCodeReview extends AbstractOpenAiCodeReview {
    private GitCommand gitCommand;
    private WeiXin weiXin;
    private IOpenAi openAi;

    public OpenAiCodeReview(GitCommand gitCommand, WeiXin weiXin, IOpenAi openAi) {
        this.gitCommand = gitCommand;
        this.weiXin = weiXin;
        this.openAi = openAi;
    }

    @Override
    protected String diff() throws Exception {
        return gitCommand.diff();
    }

    @Override
    protected String codeReview(String diff) {
        return openAi.codeReview(diff);
    }

    @Override
    protected String writeLog(String review) throws Exception {
        return gitCommand.writeLog(review);
    }

    @Override
    protected void sendWeixinMessage(String logUrl) {
        // 获取分支名称、提交者信息、仓库名称、commit message
        HashMap<String, String> map = new HashMap<>();
        // 仓库名称
        map.put(MessageDto.TemplateKey.REPO_NAME.getCode(), gitCommand.getProject());
        // 分支名称
        map.put(MessageDto.TemplateKey.BRANCH_NAME.getCode(), gitCommand.getBranch());
        // 提交的信息
        map.put(MessageDto.TemplateKey.COMMIT_MESSAGE.getCode(), gitCommand.getMessage());
        // 提交人信息
        map.put(MessageDto.TemplateKey.COMMIT_AUTHOR.getCode(), gitCommand.getAuthor());

        weiXin.sendWeixinMessage(logUrl, map);
    }
}
