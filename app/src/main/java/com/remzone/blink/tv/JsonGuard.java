package com.remzone.blink.tv;

import org.json.JSONTokener;
import java.util.HashSet;
import java.util.Set;

/** Strict lexical JSON validation before platform decoding (including escaped duplicate keys). */
final class JsonGuard {
    private final String text; private int pos;
    private JsonGuard(String text) { this.text=text; }
    static void validate(String text) throws Exception {
        if(text==null || text.length()>131072) throw new Exception("Документ слишком большой");
        JsonGuard p=new JsonGuard(text);p.value(0);p.space();if(p.pos!=text.length())p.bad();
    }
    private void bad() throws Exception { throw new Exception("Некорректный JSON подписки"); }
    private void space(){while(pos<text.length()&&" \r\n\t".indexOf(text.charAt(pos))>=0)pos++;}
    private boolean eat(char c){space();if(pos<text.length()&&text.charAt(pos)==c){pos++;return true;}return false;}
    private String string() throws Exception {
        space();int start=pos;if(!eat('"'))bad();
        while(pos<text.length()){
            char c=text.charAt(pos++);if(c=='"')return (String)new JSONTokener(text.substring(start,pos)).nextValue();
            if(c<32)bad();
            if(c=='\\'){
                if(pos>=text.length())bad();char e=text.charAt(pos++);
                if(e=='u'){for(int i=0;i<4;i++)if(pos>=text.length()||Character.digit(text.charAt(pos++),16)<0)bad();}
                else if("\"\\/bfnrt".indexOf(e)<0)bad();
            }
        }
        bad();return "";
    }
    private void value(int depth) throws Exception {
        if(depth>32)bad();space();if(pos>=text.length())bad();char c=text.charAt(pos);
        if(c=='{'){
            pos++;Set<String> keys=new HashSet<>();if(eat('}'))return;
            do{String key=string();if(!keys.add(key)||!eat(':'))bad();value(depth+1);if(eat('}'))return;}while(eat(','));bad();
        }else if(c=='['){pos++;if(eat(']'))return;do{value(depth+1);if(eat(']'))return;}while(eat(','));bad();
        }else if(c=='"'){string();}
        else {int start=pos;while(pos<text.length()&&",]} \r\n\t".indexOf(text.charAt(pos))<0)pos++;String token=text.substring(start,pos);if(!token.matches("(?:true|false|null|-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?)"))bad();}
    }
}
