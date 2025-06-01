import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.model.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * ClassName:ProjectTest
 * Package:PACKAGE_NAME
 * Description
 *
 * @Author :刘昂
 * @Create :2025/6/1--10:41
 * @Version :v1.0
 */
@SpringBootTest
@RunWith(JUnit4.class)
public class ProjectTest {
    @Test
    public void testFirst() {
        System.out.println(Integer.parseInt("aaa111"));
    }

    /**
     * 同步调用
     */
    @Test
    public void testInvoke() {
        ClientV4 client = new ClientV4.Builder("f650eb8bc4954b56a21f908b08301158.9dUNbF2pgJ9lHKuF").build();
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage chatMessage = new ChatMessage(ChatMessageRole.USER.value(), "请对以下代码进行代码审查，并指出以下方面的问题或改进建议：\n" +
                "1. 代码是否符合编码规范（命名、格式、缩进等）？\n" +
                "2. 是否存在潜在的 bug 或边界条件未处理？\n" +
                "3. 是否有重复代码？是否可以提取为公共方法？\n" +
                "4. 是否有性能问题或资源泄漏风险？\n" +
                "5. 异常处理是否完善？是否有 try-catch 滥用或缺失？\n" +
                "6. 是否有安全漏洞（如 SQL 注入、XSS 等）？\n" +
                "7. 是否有足够的注释和文档说明？\n" +
                "8. 类/方法职责是否单一？是否符合 SOLID 原则？\n" +
                "9. 接口设计是否合理？是否具备扩展性？\n" +
                "10. 日志打印是否完整、清晰且不过量？\n" +
                "\n" +
                "请以中文输出结果，并按严重程度分类：建议、警告、错误。");
        messages.add(chatMessage);
        String requestId = String.format("YourRequestId-%d", System.currentTimeMillis());
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(Constants.ModelChatGLM4)
                .stream(Boolean.FALSE)
                .invokeMethod(Constants.invokeMethod)
                .messages(messages)
                .requestId(requestId)
                .build();
        ModelApiResponse invokeModelApiResp = client.invokeModelApi(chatCompletionRequest);
        // 获取消息内容
        Optional<ModelApiResponse> resp = Optional.ofNullable(invokeModelApiResp);
        Object chatResp = resp.map(ModelApiResponse::getData).map(ModelData::getChoices)
                .filter(choices -> !choices.isEmpty()).map(choices -> choices.get(0)).map(Choice::getMessage)
                .map(ChatMessage::getContent).orElse(null);
    }
}
