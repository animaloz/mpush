package cn.mpush.base.util;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 数据补充转换工具类
 * <p>
 * A 用于补充数据的对象
 * K 补充对象用于比较的key值
 * T 被补充数据的目标对象
 *
 * @author guanxingya[OF3449]
 * company qianmi.com
 * Date 2019-12-04
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DataConvertUtil {

    @Data
    public static class Converter<A, T, K> {
        /**
         * additon data
         * 用于补充数据的对象
         */
        private Collection<? extends A> additionalData;
        /**
         * key值
         * 补充对象用于比较的key值
         */
        private Function<? extends A, K> function;
        /**
         * 数据消费
         */
        private BiConsumer<T, ? extends A> biConsumer;
        /**
         * 数据实体类
         */
        private Class entityClass;

        /**
         * map格式数据一个key对应多个value时的解决方案
         * 如果为null 默认取第一个值作为对应
         */
        private BinaryOperator<A> binaryOperator;
    }

    /**
     * 数据转换
     * <p>
     * 数据整理过程中如果一个key值由多个value对应默认取第一个
     *
     * @param targetDataSource 源数据
     * @param function         key值
     * @param converters       转换数据
     * @param <A>              addition data
     * @param <T>              target data
     * @param <K>              key
     * @return
     * @throws IllegalStateException method function#apply return null throws this exception
     */
    public static <A, T, K> List<T> convert(List<T> targetDataSource, Function<T, K> function, Converter<? extends A, T, K>... converters) {
        if (CollectionUtils.isEmpty(targetDataSource)) {
            return targetDataSource;
        }
        Map<Class, Map<K, A>> additionalDataMap = new HashMap<>(converters.length);
        BinaryOperator<A> defaultBinaryOperator = (o, n) -> o;
        Function<A, A> defauleValueMapper = v -> v;
        for (Converter converter : converters) {
            Collection<A> additionalData = converter.getAdditionalData();
            if (CollectionUtils.isEmpty(additionalData)) {
                continue;
            }
            Function<A, K> keyMapper = converter.getFunction();
            BinaryOperator<A> binaryOperator = converter.getBinaryOperator();
            Map<K, A> dataMap = additionalData.stream().collect(Collectors.toMap(keyMapper,
                    defauleValueMapper,
                    binaryOperator == null ? defaultBinaryOperator : binaryOperator));
            additionalDataMap.put(converter.getEntityClass(), dataMap);
        }
        targetDataSource.forEach(origin -> {
            K key = function.apply(origin);
            if (key == null) {
                throw new IllegalStateException("collection convert data entity key is null");
            }
            for (Converter converter : converters) {
                Collection additionalData = converter.getAdditionalData();
                if (CollectionUtils.isEmpty(additionalData)) {
                    continue;
                }
                Map<K, A> keyValueMap = additionalDataMap.get(converter.getEntityClass());
                if (keyValueMap != null && !keyValueMap.isEmpty()) {
                    A a = keyValueMap.get(key);
                    if (a != null) {
                        converter.getBiConsumer().accept(origin, a);
                    }
                }
            }
        });
        return targetDataSource;
    }

    public static void main(String[] args) {
        List<Student> dataSource = new ArrayList<>(100);
        List<School> addtionData1 = new ArrayList<>(100);
        List<Score> addtionData2 = new ArrayList<>(100);
        for (int i = 100; i > 0; i--) {
            String stuNo = String.valueOf(i);
            Student student = new Student();
            student.setStuNo(stuNo);
            student.setName("name" + stuNo);
            School school = new School();
            school.setSchool("school" + stuNo);
            school.setStuNo(stuNo);
            Score score = new Score();
            score.setStuNo("stuNo");
            score.setScore(stuNo);
            dataSource.add(student);
            addtionData1.add(school);
            addtionData2.add(score);
        }
        Converter<School, Student, String> schoolConvert = new Converter();
        schoolConvert.setAdditionalData(addtionData1);
        schoolConvert.setFunction(School::getStuNo);
        schoolConvert.setBiConsumer((student, school) -> student.setSchool(school.getSchool()));
        schoolConvert.setEntityClass(School.class);

        Converter<Score, Student, String> scoreConvert = new Converter();
        scoreConvert.setAdditionalData(addtionData2);
        scoreConvert.setFunction(Score::getStuNo);
        scoreConvert.setBiConsumer((student, score) -> student.setScore(score.getScore()));
        scoreConvert.setEntityClass(Score.class);
        scoreConvert.setBinaryOperator((score, score2) -> {
            throw new RuntimeException("");
        });

        convert(dataSource, Student::getStuNo, schoolConvert, scoreConvert);
    }

    @Data
    public static class Student {
        private String stuNo;
        private String name;
        private String school;
        private String score;
    }

    @Data
    public static class Score {
        private String stuNo;
        private String score;
    }

    @Data
    public static class School {
        private String stuNo;
        private String school;
    }
}
