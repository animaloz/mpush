package cn.mpush.base.util;

import lombok.experimental.UtilityClass;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * 表达式执行公式
 *
 * @author guanxingya[OF3449]
 * company qianmi.com
 * Date 2019-12-04
 */
@UtilityClass
public class EvalUtil {
    public <T> T eval(String[] parameterNames, Object[] args, String key, Class<T> clazz) {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext();

        for (int i = 0; i < parameterNames.length; i++) {
            context.setVariable(parameterNames[i], args[i]);
        }
        return parser.parseExpression(key).getValue(context, clazz);
    }

    public Object eval(String[] parameterNames, Object[] args, String key) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < parameterNames.length; i++) {
            context.setVariable(parameterNames[i], args[i]);
        }
        ExpressionParser parser = new SpelExpressionParser();
        return parser.parseExpression(key).getValue(context);
    }
}