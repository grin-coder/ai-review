package com.grin.infrastructure.weixin.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * ClassName:messageDto
 * Package:com.grin.infrastructure.weixin.dto
 * Description
 *
 * @Author :刘昂
 * @Create :2025/6/2--23:12
 * @Version :v1.0
 */
public class MessageDto {

    private String touser = "oA5bZ7bK-2U6qF6WFN4sbIXQAeRo";
    private String template_id = "GLlAM-Q4jdgsktdNd35hnEbHVam2mwsW2YWuxDhpQkU";
    private String topcolor = "#FF0000";
    // 消息卡片跳转url
    private String url = "https://weixin.qq.com";
    private Map<String, Map<String, String>> data = new HashMap<>();

    public void put(String key, String value) {
        data.put(key, new HashMap<String, String>() {
            private static final long serialVersionUID = 7092338402387318563L;

            {
                put("value", value);
            }
        });
    }

    /**
     * 封装map存入模板消息
     *
     * @param map 模板消息中的变量
     */
    public void putMap(Map<String, String> map) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    public enum TemplateKey {
        REPO_NAME("repo_name", "项目名称"),
        BRANCH_NAME("branch_name", "分支名称"),
        COMMIT_AUTHOR("commit_author", "提交者"),
        COMMIT_MESSAGE("commit_message", "提交信息"),
        ;

        private String code;
        private String desc;

        TemplateKey(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public String getCode() {
            return code;
        }

        public String getDesc() {
            return desc;
        }

    }

    public String getTouser() {
        return touser;
    }

    public String getTopcolor() {
        return topcolor;
    }

    public void setTopcolor(String topcolor) {
        this.topcolor = topcolor;
    }

    public void setTouser(String touser) {
        this.touser = touser;
    }

    public String getTemplate_id() {
        return template_id;
    }

    public void setTemplate_id(String template_id) {
        this.template_id = template_id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, Map<String, String>> getData() {
        return data;
    }

    public void setData(Map<String, Map<String, String>> data) {
        this.data = data;
    }
}
