package com.grin.infrastructure.ai;

/**
 * ClassName:IOpenAi
 * Package:com.grin.infrastructure.ai
 * Description
 *
 * @Author :刘昂
 * @Create :2025/6/2--15:43
 * @Version :v1.0
 */
public interface IOpenAi {
    /**
     * 对代码进行评审
     *
     * @param diffCode 需要评审的代码
     * @return 评审结果
     */
    String codeReview(String diffCode);
}
