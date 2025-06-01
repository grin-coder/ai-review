package com.grin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.model.*;
import com.zhipu.oapi.utils.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.ChainingCredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
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
            "除了上面这几条，你也可以自由发挥，请以中文输出结果，并按严重程度分类：建议、警告、错误。" +
            "\n" + "############### 变更代码如下：" + "\n";

    private String github_token = "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";

    public static void main(String[] args) throws Exception {
        // 1.获取需要评审的代码
        ProcessBuilder processBuilder = new ProcessBuilder("git", "diff", "HEAD^", "HEAD");
        // 设置命令执行目录
        processBuilder.directory(new File("."));
        Process process = processBuilder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        // 获取代码变更
        StringBuilder diffCode = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            diffCode.append(line);
        }
        // 读取错误输出（关键！）
        BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        while ((line = stderr.readLine()) != null) {
            System.err.println("Git Error: " + line);
        }

        System.out.println("diffCode：\n" + diffCode);
        int exitCode = process.waitFor();
        System.out.println("exitCode:" + exitCode);
        // 2. 进行codeReview
        String reviewRes = codeReview(diffCode.toString());
        System.out.println("review result:\n" + reviewRes);
        // 写入github的日志仓库里，用来追溯
        String githubToken = System.getenv("GITHUB_TOKEN");
        if (StringUtils.isEmpty(githubToken)) {
            throw new NullPointerException("error: githubToken is null");
        }
        System.out.println("githubToken:" + githubToken);
        writeLog(githubToken, reviewRes);
    }

    private static String writeLog(String githubToken, String reviewRes) throws Exception {
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
        File folder = new File(storagePath + "/" + date);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        // 创建文件
        SimpleDateFormat timeformat = new SimpleDateFormat("hh:mm:ss");
        String filePath = storagePath + "/" + date + "/" + timeformat.format(date) + ".md";
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
        // 返回写入后的文件url地址,默认使用main分支
        return repositoryUrl + "/blob/main/" + filePath;
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
        System.out.println("model output:" + chatResp);
        assert chatResp != null;
        return chatResp.toString();
    }

}