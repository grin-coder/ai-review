package com.grin.infrastructure.ai.impl;

import com.grin.infrastructure.ai.IOpenAi;
import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * ClassName:ChatGLM
 * Package:com.grin.infrastructure.ai.impl
 * Description
 *
 * @Author :刘昂
 * @Create :2025/6/2--15:47
 * @Version :v1.0
 */
public class ChatGLM implements IOpenAi {
    private final String apiSecretKey;

    private final String REVIEW_PROMPT = "请对以下git提交的代码进行代码审查，并指出以下方面的问题或改进建议：\n" +
            "请以中文输出结果" +
            "\n" + "############### 变更代码如下：" + "\n";

    public ChatGLM(String apiSecretKey) {
        this.apiSecretKey = apiSecretKey;
    }

    @Override
    public String codeReview(String diffCode) {
        String prompt = REVIEW_PROMPT + diffCode;
        // 创建鉴权client
        ClientV4 client = new ClientV4.Builder(apiSecretKey).networkConfig(120, 120, 120, 120, TimeUnit.SECONDS).build();
        List<ChatMessage> messages = new ArrayList<>();
        // 构建请求
        ChatMessage chatMessage = new ChatMessage(ChatMessageRole.USER.value(), prompt);
        messages.add(chatMessage);
        String requestId = String.format("YourRequestId-%d", System.currentTimeMillis());
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(Constants.ModelChatGLM4)
                .stream(Boolean.FALSE)
                .invokeMethod(Constants.invokeMethod)
                .messages(messages)
                .requestId(requestId)
                .build();
        // 进行调用
        ModelApiResponse invokeModelApiResp = client.invokeModelApi(chatCompletionRequest);
        // 获取消息内容
        Optional<ModelApiResponse> resp = Optional.ofNullable(invokeModelApiResp);
        Object chatResp = resp.map(ModelApiResponse::getData).map(ModelData::getChoices)
                .filter(choices -> !choices.isEmpty()).map(choices -> choices.get(0)).map(Choice::getMessage)
                .map(ChatMessage::getContent).orElse(null);
        assert chatResp != null;
        return chatResp.toString();
    }
}
