package com.grin;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.grin.domain.model.Message;
import com.grin.domain.model.service.impl.OpenAiCodeReview;
import com.grin.infrastructure.ai.IOpenAi;
import com.grin.infrastructure.ai.impl.ChatGLM;
import com.grin.infrastructure.git.GitCommand;
import com.grin.infrastructure.weixin.WeiXin;
import com.grin.types.utils.WXAccessTokenUtils;
import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.model.*;
import com.zhipu.oapi.utils.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.ChainingCredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

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
        System.out.println(getEnv("GITHUB_TOKEN"));
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

//    public static void main(String[] args) throws Exception {
//        // 1.获取需要评审的代码
//        ProcessBuilder processBuilder = new ProcessBuilder("git", "diff", "HEAD^", "HEAD");
//        // 设置命令执行目录
//        processBuilder.directory(new File("."));
//        Process process = processBuilder.start();
//        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//        String line;
//        // 获取代码变更
//        StringBuilder diffCode = new StringBuilder();
//        while ((line = reader.readLine()) != null) {
//            diffCode.append(line);
//        }
//        // 读取错误输出（关键！）
//        BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
//        while ((line = stderr.readLine()) != null) {
//            System.err.println("Git Error: " + line);
//        }
//
//        System.out.println("diffCode：\n" + diffCode);
//        int exitCode = process.waitFor();
//        System.out.println("exitCode:" + exitCode);
//        // 2. 进行codeReview
//        String reviewRes = codeReview(diffCode.toString());
//        System.out.println("review result:\n" + reviewRes);
//        // 3. 写入github的日志仓库里，用来追溯
//        String githubToken = System.getenv("GITHUB_TOKEN");
//        if (StringUtils.isEmpty(githubToken)) {
//            throw new NullPointerException("error: githubToken is null");
//        }
//        System.out.println("githubToken:" + githubToken);
//        String logUrl = writeLog(githubToken, reviewRes);
//        System.out.println("logUrl:" + logUrl);
//        // 4. 通过微信公众号发送消息
//        Message message = new Message();
//        message.setUrl(logUrl);
//        message.put("creator", "grin");
//        message.put("logUrl", logUrl);
//        message.setTemplate_id("eU0ZsNPNp3P6LQgzzSoURunPda9_Ytkd_ZDehImk5S8");
//        message.setTopcolor("#98FF98");
//        sendWeixinMessage(message);
//    }

//    private static void sendWeixinMessage(Message message) {
//        // 获取访问令牌
//        String accessToken = WXAccessTokenUtils.getAccessToken();
//        System.out.println("AccessToken:" + accessToken);
//        // 进行发送
//        String url = String.format("https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=%s", accessToken);
//        sendPostRequest(url, JSON.toJSONString(message));
//    }

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

    private static String writeLog(String githubToken, String reviewRes) throws Exception {
        // 对记录log仓库进行clone，然后把评审结果写入后，进行提交并推送给远程仓库
        String repositoryUrl = "https://github.com/grin-coder/ai-review-log.git";
        // 文件的前缀url
        String logFileUrl = "https://github.com/grin-coder/ai-review-log/blob/main/";
        String storagePath = "review";
        Git git = Git.cloneRepository().setURI(repositoryUrl)
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(githubToken, ""))
                .setDirectory(new File(storagePath))
                .call();
        // 创建评审文件,日期 / 随机数
        // 指定时区为北京时间（UTC+8）
        ZoneId zoneId = ZoneId.of("Asia/Shanghai");  // 也可以换成其他时区，比如 ZoneId.of("Europe/London")
        // 使用带有时区的 ZonedDateTime 替代 Date
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        // 格式化日期为 yyyy-MM-dd（用于文件夹名称）
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(zoneId);
        String dateStr = dateFormatter.format(now);
        // 创建文件夹
        File folder = new File(storagePath + "/" + dateStr);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        // 创建文件
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH时mm分ss秒SSS毫秒").withZone(zoneId);
        String fileName = timeFormatter.format(now) + ".md";
        String filePath = storagePath + "/" + dateStr + "/" + fileName;
        File file = new File(filePath);
        // 将评审结果写入文件
        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(reviewRes);
        }
        // 进行提交
        git.add().addFilepattern(dateStr + "/" + fileName).call();
        git.commit().setMessage("代码评审结果写入v2.0").call();
        git.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(githubToken, "")).call();

        System.out.println("review success!");
        // 返回写入后的文件url地址,默认使用main分支
        return logFileUrl + dateStr + "/" + fileName;
    }

    public static String codeReview(String content) {
        String prompt = REVIEW_PROMPT + content;

        // 创建鉴权client
        ClientV4 client = new ClientV4.Builder(API_SECRET_KEY).networkConfig(120, 120, 120, 120, TimeUnit.SECONDS).build();
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