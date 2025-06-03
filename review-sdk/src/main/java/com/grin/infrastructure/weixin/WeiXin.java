package com.grin.infrastructure.weixin;

import com.alibaba.fastjson2.JSON;
import com.grin.infrastructure.weixin.dto.MessageDto;
import com.grin.types.utils.WXAccessTokenUtils;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Scanner;

/**
 * ClassName:Weixin
 * Package:com.grin.infrastructure.weixin
 * Description
 *
 * @Author :刘昂
 * @Create :2025/6/2--15:59
 * @Version :v1.0
 */
public class WeiXin {
    private String appId;
    private String secret;
    private String touser;
    private String template_id;

    public WeiXin(String appId, String secret, String touser, String template_id) {
        this.appId = appId;
        this.secret = secret;
        this.touser = touser;
        this.template_id = template_id;
    }

    public void sendWeixinMessage(String logUrl, Map<String, String> data) {
        // 构建微信模板消息
        MessageDto message = new MessageDto();
        message.setTouser(touser);
        message.putMap(data);
        message.setUrl(logUrl);
        message.setTemplate_id(template_id);
        // 获取访问令牌
        String accessToken = WXAccessTokenUtils.getAccessToken(appId, secret);
        System.out.println("AccessToken:" + accessToken);
        // 进行发送
        String url = String.format("https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=%s", accessToken);
        sendPostRequest(url, JSON.toJSONString(message));
    }

    private void sendPostRequest(String urlString, String jsonBody) {
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
