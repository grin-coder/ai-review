package com.grin.infrastructure.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ClassName:GitComman
 * Package:com.grin.infrastructure.git
 * Description
 *
 * @Author :刘昂
 * @Create :2025/6/2--15:10
 * @Version :v1.0
 */
public class GitCommand {
    // 创建人，分支名，提交时的message
    private final Logger logger = LoggerFactory.getLogger(GitCommand.class);

    private final String githubReviewLogUri;

    private final String githubToken;

    private final String project;

    private final String branch;

    private final String author;

    // 评审后的文字
    private final String message;

    /**
     * 获取需要评审的代码
     *
     * @return 需要评审的代码
     * @throws Exception 异常
     */
    public String diff() throws Exception {
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
        // 读取错误输出（用于排查错误）
        BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        while ((line = stderr.readLine()) != null) {
            System.err.println("Git Error: " + line);
        }
        logger.info("diffCode：\n{}", diffCode);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            logger.warn("exitCode:{}", exitCode);
            throw new RuntimeException("warn:exitCode is " + exitCode);
        }
        return diffCode.toString();
    }

    /**
     * 向仓库中写日志
     *
     * @param reviewRes 评审结果
     * @return 评审结果文件url
     * @throws Exception 异常
     */
    public String writeLog(String reviewRes) throws Exception {
        String storagePath = "review";
        Git git = Git.cloneRepository().setURI(githubReviewLogUri + ".git")
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(githubToken, ""))
                .setDirectory(new File(storagePath))
                .call();
        // 创建评审文件,日期 / 时分秒毫秒.md
        ZoneId zoneId = ZoneId.of("Asia/Shanghai");  // 也可以换成其他时区，比如 ZoneId.of("Europe/London")
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(zoneId);
        String dateStr = dateFormatter.format(now);

        // 创建文件夹
        String folderStr = storagePath + "/" + dateStr + "/" + project + "/" + branch + "/" + author.replaceAll(" ","");
        File folder = new File(folderStr);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        // 创建文件
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH时mm分ss秒SSS毫秒").withZone(zoneId);
        String fileName = timeFormatter.format(now) + ".md";
        String filePath = folderStr + fileName;
        File file = new File(filePath);
        // 将评审结果写入文件
        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(reviewRes);
        }
        // 进行提交
        git.add().addFilepattern(dateStr + "/" + project + "/" + branch + "/"
                + author.replaceAll(" ","") + "/" + fileName).call();
        git.commit().setMessage(author + "代码评审结果完成").call();
        git.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(githubToken, "")).call();

        // 返回写入后的文件url地址,默认使用main分支
        return githubReviewLogUri + "/" + project + "/" + branch + "/" + author.replaceAll(" ","") + fileName;
    }


    public GitCommand(String githubReviewLogUri, String githubToken, String project, String branch, String author, String message) {
        this.githubReviewLogUri = githubReviewLogUri;
        this.githubToken = githubToken;
        this.project = project;
        this.branch = branch;
        this.author = author;
        this.message = message;
    }

    public Logger getLogger() {
        return logger;
    }

    public String getGithubReviewLogUri() {
        return githubReviewLogUri;
    }

    public String getGithubToken() {
        return githubToken;
    }

    public String getProject() {
        return project;
    }

    public String getBranch() {
        return branch;
    }

    public String getAuthor() {
        return author;
    }

    public String getMessage() {
        return message;
    }
}
