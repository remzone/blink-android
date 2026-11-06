package com.remzone.blink.tv;

import java.util.ArrayList;
import java.util.List;

/** Internet routes; private LAN/loopback/link-local remain usable for casting and ADB. */
final class RoutePolicy {
    private static final long[][] EXCLUDE={{0x00000000L,8},{0x0a000000L,8},{0x7f000000L,8},{0xa9fe0000L,16},{0xac100000L,12},{0xc0a80000L,16},{0xe0000000L,3}};
    static List<String> ipv4(){List<String> result=new ArrayList<>();split(0,0,result);return result;}
    private static void split(long base,int bits,List<String> result){
        long end=base+(1L<<(32-bits))-1;boolean overlap=false;
        for(long[] range:EXCLUDE){long rEnd=range[0]+(1L<<(32-range[1]))-1;if(base>=range[0]&&end<=rEnd)return;if(base<=rEnd&&end>=range[0])overlap=true;}
        if(!overlap){result.add(((base>>>24)&255)+"."+((base>>>16)&255)+"."+((base>>>8)&255)+"."+(base&255)+"/"+bits);return;}
        split(base,bits+1,result);split(base+(1L<<(31-bits)),bits+1,result);
    }
}
