package com.remzone.blink.tv;
import org.junit.Test;
import static org.junit.Assert.*;
public class RoutePolicyTest {
    private long ip(String s){long n=0;for(String b:s.split("\\."))n=(n<<8)|Integer.parseInt(b);return n;}
    private boolean routed(String address){return routed(address,RoutePolicy.ipv4());}
    private boolean routed(String address,java.util.List<String> routes){long n=ip(address);for(String route:routes){String[] p=route.split("/");long mask=(0xffffffffL<<(32-Integer.parseInt(p[1])))&0xffffffffL;if((n&mask)==(ip(p[0])&mask))return true;}return false;}
    @Test public void wireGuardKeepsOnlyActualLanAndRoutesRemotePrivateNetworks(){java.util.List<String> routes=RoutePolicy.wireGuardDefault(java.util.List.of("192.168.10.114/24"));assertFalse(routed("192.168.10.1",routes));assertFalse(routed("127.0.0.1",routes));assertTrue(routed("10.77.0.1",routes));assertTrue(routed("192.168.20.1",routes));assertTrue(routed("1.1.1.1",routes));assertTrue(routes.size()<128);}
    @Test public void preservesLanAndLoopbackWhileRoutingInternet(){for(String local:new String[]{"0.0.0.0","10.0.0.1","127.0.0.1","169.254.2.3","172.16.0.1","172.31.255.254","192.168.1.50","224.0.0.1","255.255.255.255"})assertFalse(local,routed(local));for(String external:new String[]{"1.1.1.1","8.8.8.8","172.15.255.255","172.32.0.0","192.167.255.255","192.169.0.0","223.255.255.255"})assertTrue(external,routed(external));assertTrue(RoutePolicy.ipv4().size()<100);}
}
