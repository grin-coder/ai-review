package com.grin;

import com.grin.domain.model.service.impl.OpenAiCodeReview;
import com.grin.infrastructure.ai.IOpenAi;
import com.grin.infrastructure.ai.impl.ChatGLM;
import com.grin.infrastructure.git.GitCommand;
import com.grin.infrastructure.weixin.WeiXin;

/**
 * ClassName:${NAME}
 * Package:com.grin
 * Description
 *
 * @Author :刘昂
 * @Create :${DATE}--${TIME}
 * @Version :v1.0
 */
public class Main {
    private static final String API_SECRET_KEY = "f650eb8bc4954b56a21f908b08301158.9dUNbF2pgJ9lHKuF";

    private static final String REVIEW_PROMPT = "请对以下git提交的代码进行代码审查，并指出以下方面的问题或改进建议：\n" +
            "请以中文输出结果" +
            "\n" + "############### 变更代码如下：" + "\n";

    public static void main(String[] args) throws Exception {
        GitCommand gitCommand = new GitCommand(
                getEnv("GITHUB_REVIEW_LOG_URI"),
                getEnv("GITHUB_TOKEN"),
                getEnv("COMMIT_PROJECT"),
                getEnv("COMMIT_BRANCH"),
                getEnv("COMMIT_AUTHOR"),
                getEnv("COMMIT_MESSAGE")
        );

        WeiXin weiXin = new WeiXin(
                getEnv("WEIXIN_APPID"),
                getEnv("WEIXIN_SECRET"),
                getEnv("WEIXIN_TOUSER"),
                getEnv("WEIXIN_TEMPLATE_ID")
        );

        IOpenAi iOpenAi = new ChatGLM(getEnv("CHATGLM_APIKEYSECRET"));
        OpenAiCodeReview openAiCodeReview = new OpenAiCodeReview(gitCommand, weiXin, iOpenAi);
        openAiCodeReview.exec();
    }

    private static String getEnv(String key) {
        String value = System.getenv(key);
        System.out.println("环境变量" + key + "；" + value);
        if (null == value || value.isEmpty()) {
            throw new RuntimeException("value is null");
        }
        return value;
    }
}