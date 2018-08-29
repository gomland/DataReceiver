package my.home.ibgs.tools;

/**
 * Created by shin on 2017-02-02.
 */

public class TransValue {
    //숫자를 제외한 다른 속성 무시하고 정수로 변환
    public static int toInteger(String data){
        int temp = 0;
        int unit = 1;

        for(int i=data.length()-1; i>=0; i--){
            char ch = data.charAt(i);
            if(ch >= '0' && ch <= '9'){
                temp += ((int)ch - '0') * unit;
                unit *= 10;
            }
        }

        return temp;
    }
}
