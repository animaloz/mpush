package cn.mpush.base.util.encrypt;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Md5Util {
    private Md5Util() {
    }

    public static String md5Hex(String string) {
        byte[] hash;
        try {
            // 创建加密对象
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            // 调用加密对象的方法，加密的动作已经完成
            hash = md5.digest(string.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            // 第一步，将数据全部转换成正数：
            // 解释：为什么采用b&255
            /*
             * b:它本来是一个byte类型的数据(1个字节) 255：是一个int类型的数据(4个字节)
             * byte类型的数据与int类型的数据进行运算，会自动类型提升为int类型 eg: b: 1001 1100(原始数据)
             * 运算时：
             * b:    0000 0000 0000 0000 0000 0000 1001 1100
             * 255:  0000 0000 0000 0000 0000 0000 1111 1111
             * 结果：0000 0000 0000 0000 0000 0000 1001 1100
             * 此时的temp是一个int类型的整数
             */
            // 第二步，将所有的数据转换成16进制的形式
            // 注意：转换的时候注意if正数>=0&&<16，那么如果使用Integer.toHexString()，可能会造成缺少位数
            // 因此，需要对temp进行判断
            int bOx = b & 0xFF;
            if (bOx < 0x10){
                // 手动补0
                hex.append("0");
            }
            hex.append(Integer.toHexString(bOx));
        }
        return hex.toString();
    }

    public static void main(String[] args) {
        System.out.println(md5Hex("http://wiki.dev.qianmi.com/pages/viewpage.action?pageId=27002634"));
    }
}
