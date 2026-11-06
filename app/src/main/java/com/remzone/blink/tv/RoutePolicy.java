package com.remzone.blink.tv;

import java.util.ArrayList;
import java.util.List;

/** Internet routes; private LAN/loopback/link-local remain usable for casting and ADB. */
final class RoutePolicy {
    private static final long[][] EXCLUDE={{0x00000000L,8},{0x0a000000L,8},{0x7f000000L,8},{0xa9fe0000L,16},{0xac100000L,12},{0xc0a80000L,16},{0xe0000000L,3}};
    static List<String> ipv4(){List<String> result=new ArrayList<>();split(0,0,result,EXCLUDE);return result;}
    static List<String> wireGuardDefault(List<String> localNetworks){
        List<long[]> exclude=new ArrayList<>();for(long[] r:new long[][]{{0,8},{0x7f000000L,8},{0xa9fe0000L,16},{0xe0000000L,3}})exclude.add(r);
        for(String cidr:localNetworks){String[] p=cidr.split("/");int bits=Integer.parseInt(p[1]);if(bits<1||bits>32)continue;long address=0;for(String part:p[0].split("\\."))address=(address<<8)|Integer.parseInt(part);long mask=(0xffffffffL<<(32-bits))&0xffffffffL;exclude.add(new long[]{address&mask,bits});}
        List<String> result=new ArrayList<>();split(0,0,result,exclude.toArray(new long[0][]));return result;
    }
    private static void split(long base,int bits,List<String> result,long[][] exclude){
        long end=base+(1L<<(32-bits))-1;boolean overlap=false;
        for(long[] range:exclude){long rEnd=range[0]+(1L<<(32-range[1]))-1;if(base>=range[0]&&end<=rEnd)return;if(base<=rEnd&&end>=range[0])overlap=true;}
        if(!overlap){result.add(((base>>>24)&255)+"."+((base>>>16)&255)+"."+((base>>>8)&255)+"."+(base&255)+"/"+bits);return;}
        split(base,bits+1,result,exclude);split(base+(1L<<(31-bits)),bits+1,result,exclude);
    }
}
