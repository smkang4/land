package com.dage.rent.Component;

import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Component
public class CommonUtils {

    public Map<String, String> returnMapData(Map<String,String[]> map){

        Iterator it = map.keySet().iterator();

        String keys = null;
        String[] value = null;

        Map<String,String> returnMap = new HashMap<>();

        while(it.hasNext()){
            keys = String.valueOf(it.next());
            value = map.get(keys);
            for (int i = 0 ; i < value.length ; i++){
                returnMap.put(keys,value[i]);
            }
        }
        return returnMap;
    }

    public Map<String, Object> returnMapDataObj(Map<String,String[]> map){

        Iterator it = map.keySet().iterator();

        String keys = null;
        String[] value = null;

        Map<String,Object> returnMap = new HashMap<>();

        while(it.hasNext()){
            keys = String.valueOf(it.next());
            value = map.get(keys);
            for (int i = 0 ; i < value.length ; i++){
                returnMap.put(keys,value[i]);
            }
        }
        return returnMap;
    }
}
