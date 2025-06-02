import com.alibaba.fastjson2.JSON;
import com.grin.domain.model.Message;
import com.grin.types.utils.WXAccessTokenUtils;
import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.model.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
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
//        ClientV4 client = new ClientV4.Builder("f650eb8bc4954b56a21f908b08301158.9dUNbF2pgJ9lHKuF").build();
//        ClientV4 client = new ClientV4.Builder("f650eb8bc4954b56a21f908b08301158.9dUNbF2pgJ9lHKuF").networkConfig(15, 15, 15, 60, TimeUnit.SECONDS).build();
        // 创建鉴权client
        ClientV4 client = new ClientV4.Builder("f650eb8bc4954b56a21f908b08301158.9dUNbF2pgJ9lHKuF").networkConfig(60, 60, 60, 60, TimeUnit.SECONDS).build();

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
        System.out.println(chatResp);
    }

    @Test
    public void testWriteLog() throws Exception {
        String githubToken = "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";
        String reviewRes = "这是我的日志";
        // 对记录log仓库进行clone，然后把评审结果写入后，进行提交并推送给远程仓库
        String repositoryUrl = "https://github.com/grin-coder/ai-review-log.git";
        String storagePath = "review";
        Git git = Git.cloneRepository().setURI(repositoryUrl)
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(githubToken, ""))
                .setDirectory(new File(storagePath))
                .call();
        // 创建评审文件,日期 / 随机数
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        String dateStr = simpleDateFormat.format(date);
        // 创建文件夹
        File folder = new File(storagePath + "/" + dateStr);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        // 创建文件
        SimpleDateFormat timeformat = new SimpleDateFormat("hh时mm分ss秒SSS毫秒");
        String filePath = storagePath + "/" + dateStr + "/" + timeformat.format(date) + ".md";
        File file = new File(filePath);
        file.createNewFile();
        // 将评审结果写入文件
        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(reviewRes);
        }
        // 进行提交
        git.add().addFilepattern(filePath).call();
        git.commit().setMessage("代码评审结果写入").call();
        git.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(githubToken, "")).call();

        System.out.println("review success!");

    }

    @Test
    public void testPath() throws IOException {
        String githubToken = "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";
        String reviewRes = "这是我的日志";
        // 对记录log仓库进行clone，然后把评审结果写入后，进行提交并推送给远程仓库
        String repositoryUrl = "https://github.com/grin-coder/ai-review-log.git";
        String storagePath = "review";

        // 创建评审文件,日期 / 随机数
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        String dateStr = simpleDateFormat.format(date);
        // 创建文件夹
        File folder = new File(storagePath + "/" + dateStr);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        // 创建文件
        SimpleDateFormat timeformat = new SimpleDateFormat("hh时mm分ss秒SSS毫秒");
        String filePath = storagePath + "/" + dateStr + "/" + timeformat.format(date) + ".md";
        System.out.println(filePath);
        File file = new File(filePath);
        file.createNewFile();
        // 将评审结果写入文件
        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(reviewRes);
        }
        System.out.println("review success!");
    }

    @Test
    public void testWeixinMessage() {
        // 4. 通过微信公众号发送消息
        Message message = new Message();
        message.put("creator", "grin");
        message.put("logUrl", "https://github.com/grin-coder/ai-review-log/blob/main/2025-06-01/04%E6%97%B636%E5%88%8608%E7%A7%92441%E6%AF%AB%E7%A7%92.md");
        message.setTemplate_id("zU9VjZdl0N6SAnLhf3ER4NbDwCSAQsbXgjNUSB9SvtU");
        message.setTopcolor("#98FF98");
        // 获取访问令牌
        String accessToken = WXAccessTokenUtils.getAccessToken();
        System.out.println("AccessToken:" + accessToken);
        // 进行发送
        String url = String.format("https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=%s", accessToken);
        sendPostRequest(url, JSON.toJSONString(message));
    }

    private static void sendPostRequest(String urlString, String jsonBody) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            try (Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name())) {
                String response = scanner.useDelimiter("\\A").next();
                System.out.println(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
