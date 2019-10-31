package ru.er_log.bluetooth;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest
{
    @Test
    public void addition_isCorrect()
    {
//        int number = 123;
//        String s1 = String.valueOf(number);
//        String s2 = String.valueOf(number);
//
//        String s3 = s1.intern();
//        String s4 = s2.intern();
//
//        System.out.println(s1 == s2);
//        System.out.println(s1.equals(s2));
//        System.out.println(s3 == s4);
//        System.out.println(s3.equals(s4));
//        System.out.println(s1);

        String string = new String("Хелло");
        System.out.println(
                "Char size=" + Character.SIZE/8
                        + ", Hello.getBytes().length=" + "Хелло".getBytes().length
                        + ", string=" + string.getBytes(StandardCharsets.UTF_8).length
                );

        String lol = new String((byte[]) null);
        System.out.println(lol);
    }
}