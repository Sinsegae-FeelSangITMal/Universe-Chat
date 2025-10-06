
package com.sinse.chat.chat;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

@Component
public class HtmlSanitizer {
    public String clean(String raw) {
        return Jsoup.clean(raw, Safelist.none()); // 텍스트만 허용
    }
}
