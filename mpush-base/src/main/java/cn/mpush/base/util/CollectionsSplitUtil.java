package cn.mpush.base.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 数据分割工具 支持数组 list分割
 * @author guanxingya[OF3449]
 * company qianmi.com
 * Date 2019-12-04
 */
public class CollectionsSplitUtil {

    public static List<String[]> arraySplit(String[] array, int length) {
        if(array == null || array.length == 0){
            throw new IllegalArgumentException("数组不能为null");
        }
        if(length <= 0){
            throw new IllegalArgumentException("分割长度必须大于0");
        }
        int total = array.length;
        int pages = (total - 1) / length + 1;
        List<String[]> result = new ArrayList<>(pages);
        for (int i = 1; i <= pages; i++) {
            if(i == pages){
                String[] arrayOfRange = Arrays.copyOfRange(array, (i - 1) * length, total);
                result.add(arrayOfRange);
            } else {
                String[] arrayOfRange = Arrays.copyOfRange(array, (i - 1) * length, i * length);
                result.add(arrayOfRange);
            }
        }
        return result;
    }

    public static <T> List<List<T>> listSplit(List<T> list, int length) {
        if(list == null || list.isEmpty()){
            throw new IllegalArgumentException("list不能为null");
        }
        if(length <= 0){
            throw new IllegalArgumentException("分割长度必须大于0");
        }
        int total = list.size();
        int pages = (total - 1) / length + 1;
        List<List<T>> result = new ArrayList<>(pages);
        for (int i = 1; i <= pages; i++) {
            if(i == pages){
                result.add(list.subList((i - 1) * length, total));
            } else {
                result.add(list.subList((i - 1) * length, i * length));
            }
        }
        return result;
    }
}
